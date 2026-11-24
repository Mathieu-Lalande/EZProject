package com.ezproject.task.collab;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.task.TaskDto;

/** Compteurs (commentaires, pièces jointes, sous-tâches) de toutes les tâches d'un projet, en 4 requêtes. */
@Component
public class CollaborationCounts {

    private final TaskCommentRepository comments;
    private final ChecklistItemRepository checklist;
    private final TaskAttachmentRepository attachments;

    CollaborationCounts(TaskCommentRepository comments, ChecklistItemRepository checklist,
                        TaskAttachmentRepository attachments) {
        this.comments = comments;
        this.checklist = checklist;
        this.attachments = attachments;
    }

    @Transactional(readOnly = true)
    public Map<Long, TaskDto.Counts> forProject(Long projectId) {
        Map<Long, Long> c = toMap(comments.countByTask(projectId));
        Map<Long, Long> a = toMap(attachments.countByTask(projectId));
        Map<Long, Long> total = toMap(checklist.countByTask(projectId));
        Map<Long, Long> done = toMap(checklist.countDoneByTask(projectId));
        Map<Long, TaskDto.Counts> result = new HashMap<>();
        java.util.Set<Long> ids = new java.util.HashSet<>();
        ids.addAll(c.keySet());
        ids.addAll(a.keySet());
        ids.addAll(total.keySet());
        ids.forEach(id -> result.put(id, new TaskDto.Counts(c.getOrDefault(id, 0L), a.getOrDefault(id, 0L),
                done.getOrDefault(id, 0L), total.getOrDefault(id, 0L))));
        return result;
    }

    private static Map<Long, Long> toMap(List<TaskCounted> rows) {
        Map<Long, Long> m = new HashMap<>();
        rows.forEach(r -> m.put(r.getTaskId(), r.getTotal()));
        return m;
    }
}
