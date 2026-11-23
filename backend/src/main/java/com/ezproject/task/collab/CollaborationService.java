package com.ezproject.task.collab;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.AccessContext;
import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.audit.AuditService;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.project.Project;
import com.ezproject.task.Task;
import com.ezproject.task.TaskRepository;
import com.ezproject.task.collab.CollaborationDtos.AttachmentDto;
import com.ezproject.task.collab.CollaborationDtos.ChecklistDto;
import com.ezproject.task.collab.CollaborationDtos.CommentDto;
import com.ezproject.task.collab.CollaborationDtos.TaskDetailsDto;
import com.ezproject.user.AppUser;

/**
 * Collaboration sur une tâche : commentaires (TASK_COMMENT), sous-tâches et pièces jointes (TASK_EDIT),
 * lecture avec TASK_VIEW. L'auteur d'un commentaire ou d'un fichier peut le modifier / supprimer ;
 * TASK_DELETE permet de modérer.
 */
@Service
@Transactional(readOnly = true)
public class CollaborationService {

    public record Download(String filename, String contentType, byte[] content) {
    }

    private final TaskRepository tasks;
    private final TaskCommentRepository comments;
    private final ChecklistItemRepository checklist;
    private final TaskAttachmentRepository attachments;
    private final AttachmentContentStore contentStore;
    private final ProjectAccessService access;
    private final AuditService audit;

    public CollaborationService(TaskRepository tasks, TaskCommentRepository comments, ChecklistItemRepository checklist,
                                TaskAttachmentRepository attachments, AttachmentContentStore contentStore,
                                ProjectAccessService access, AuditService audit) {
        this.tasks = tasks;
        this.comments = comments;
        this.checklist = checklist;
        this.attachments = attachments;
        this.contentStore = contentStore;
        this.access = access;
        this.audit = audit;
    }

    public TaskDetailsDto details(Long taskId) {
        Task task = task(taskId, Permission.TASK_VIEW);
        AccessContext ctx = access.context();
        Project project = task.getProject();
        Long me = ctx.user().getId();
        boolean moderate = ctx.can(project, Permission.TASK_DELETE);
        boolean canComment = ctx.can(project, Permission.TASK_COMMENT);
        boolean canEdit = ctx.can(project, Permission.TASK_EDIT);
        return new TaskDetailsDto(
                comments.findByTaskIdOrderByCreatedAtAsc(taskId).stream().map(c -> toDto(c, me, moderate)).toList(),
                checklist.findByTaskIdOrderByPositionAsc(taskId).stream().map(CollaborationService::toDto).toList(),
                attachments.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                        .map(a -> toDto(a, moderate || (canEdit && a.getUploader() != null && a.getUploader().getId().equals(me))))
                        .toList(),
                canComment, canEdit);
    }

    // ---------------------------------------------------------------- Commentaires

    @Transactional
    public CommentDto comment(Long taskId, String body) {
        Task task = task(taskId, Permission.TASK_COMMENT);
        AppUser me = access.context().user();
        TaskComment saved = comments.save(new TaskComment(task, me, body.trim()));
        audit.record("TASK_COMMENTED", "TASK", taskId, task.getProject().getCode() + " · " + task.getTitle());
        return toDto(saved, me.getId(), false);
    }

    @Transactional
    public CommentDto editComment(Long commentId, String body) {
        TaskComment comment = comments.findById(commentId).orElseThrow(() -> new NotFoundException("Commentaire", commentId));
        AppUser me = visibleTaskUser(comment.getTask());
        if (comment.getAuthor() == null || !comment.getAuthor().getId().equals(me.getId())) {
            throw new AccessDeniedException("Seul l'auteur peut modifier son commentaire");
        }
        comment.edit(body.trim());
        return toDto(comment, me.getId(), false);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        TaskComment comment = comments.findById(commentId).orElseThrow(() -> new NotFoundException("Commentaire", commentId));
        AppUser me = visibleTaskUser(comment.getTask());
        boolean own = comment.getAuthor() != null && comment.getAuthor().getId().equals(me.getId());
        if (!own) {
            access.require(comment.getTask().getProject(), Permission.TASK_DELETE);
        }
        comments.delete(comment);
    }

    // ---------------------------------------------------------------- Sous-tâches

    @Transactional
    public List<ChecklistDto> addItem(Long taskId, String title) {
        Task task = task(taskId, Permission.TASK_EDIT);
        List<ChecklistItem> items = checklist.findByTaskIdOrderByPositionAsc(taskId);
        if (items.size() >= 50) {
            throw new BusinessRuleException("50 sous-tâches maximum");
        }
        checklist.save(new ChecklistItem(task, title.trim(), items.size()));
        return items(taskId);
    }

    @Transactional
    public List<ChecklistDto> updateItem(Long itemId, String title, boolean done) {
        ChecklistItem item = checklist.findById(itemId).orElseThrow(() -> new NotFoundException("Sous-tâche", itemId));
        access.require(item.getTask().getProject(), Permission.TASK_EDIT);
        item.update(title.trim(), done);
        return items(item.getTask().getId());
    }

    @Transactional
    public List<ChecklistDto> deleteItem(Long itemId) {
        ChecklistItem item = checklist.findById(itemId).orElseThrow(() -> new NotFoundException("Sous-tâche", itemId));
        access.require(item.getTask().getProject(), Permission.TASK_EDIT);
        Long taskId = item.getTask().getId();
        checklist.delete(item);
        checklist.flush();
        return items(taskId);
    }

    private List<ChecklistDto> items(Long taskId) {
        return checklist.findByTaskIdOrderByPositionAsc(taskId).stream().map(CollaborationService::toDto).toList();
    }

    // ---------------------------------------------------------------- Pièces jointes

    @Transactional
    public AttachmentDto attach(Long taskId, String originalName, byte[] content) {
        Task task = task(taskId, Permission.TASK_EDIT);
        if (attachments.countByTaskId(taskId) >= AttachmentPolicy.MAX_PER_TASK) {
            throw new BusinessRuleException(AttachmentPolicy.MAX_PER_TASK + " pièces jointes maximum par tâche");
        }
        AttachmentPolicy.Checked checked = AttachmentPolicy.check(originalName, content);
        AppUser me = access.context().user();
        TaskAttachment saved = attachments.saveAndFlush(new TaskAttachment(task, me, checked.filename(),
                checked.contentType(), content.length));
        contentStore.write(saved.getId(), content);
        audit.record("ATTACHMENT_ADDED", "TASK", taskId, checked.filename() + " sur « " + task.getTitle() + " »");
        return toDto(saved, true);
    }

    public Download download(Long attachmentId) {
        TaskAttachment a = attachments.findById(attachmentId).orElseThrow(() -> new NotFoundException("Pièce jointe", attachmentId));
        access.require(a.getTask().getProject(), Permission.TASK_VIEW);
        byte[] content = contentStore.read(attachmentId);
        if (content == null) {
            throw new NotFoundException("Pièce jointe", attachmentId);
        }
        return new Download(a.getFilename(), a.getContentType(), content);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        TaskAttachment a = attachments.findById(attachmentId).orElseThrow(() -> new NotFoundException("Pièce jointe", attachmentId));
        AccessContext ctx = access.context();
        Project project = a.getTask().getProject();
        boolean own = a.getUploader() != null && a.getUploader().getId().equals(ctx.user().getId())
                && ctx.can(project, Permission.TASK_EDIT);
        if (!own) {
            access.require(project, Permission.TASK_DELETE);
        }
        audit.record("ATTACHMENT_DELETED", "TASK", a.getTask().getId(), a.getFilename());
        attachments.delete(a);
    }

    // ---------------------------------------------------------------- Utilitaires

    private Task task(Long taskId, Permission permission) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new NotFoundException("Tâche", taskId));
        access.require(task.getProject(), permission);
        return task;
    }

    private AppUser visibleTaskUser(Task task) {
        access.require(task.getProject(), Permission.TASK_VIEW);
        return access.context().user();
    }

    private static CommentDto toDto(TaskComment c, Long me, boolean moderate) {
        boolean own = c.getAuthor() != null && c.getAuthor().getId().equals(me);
        return new CommentDto(c.getId(), c.getBody(), c.getAuthor() == null ? null : c.getAuthor().getId(),
                c.getAuthor() == null ? "Utilisateur supprimé" : c.getAuthor().getDisplayName(), c.getCreatedAt(),
                c.getEditedAt(), own || moderate);
    }

    private static ChecklistDto toDto(ChecklistItem i) {
        return new ChecklistDto(i.getId(), i.getTitle(), i.isDone());
    }

    private static AttachmentDto toDto(TaskAttachment a, boolean deletable) {
        return new AttachmentDto(a.getId(), a.getFilename(), a.getContentType(), a.getSizeBytes(),
                a.getUploader() == null ? null : a.getUploader().getDisplayName(), a.getCreatedAt(), deletable);
    }
}
