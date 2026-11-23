package com.ezproject.task;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.AccessContext;
import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.common.NotFoundException;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.user.AppUser;
import com.ezproject.user.UserService;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository repository;
    private final ProjectAccessService access;
    private final UserService userService;
    private final Clock clock;

    public TaskService(TaskRepository repository, ProjectAccessService access, UserService userService, Clock clock) {
        this.repository = repository;
        this.access = access;
        this.userService = userService;
        this.clock = clock;
    }

    public List<TaskDto> findByProject(Long projectId) {
        access.require(projectId, Permission.TASK_VIEW);
        LocalDate today = LocalDate.now(clock);
        return repository.findByProjectIdOrderByPositionAsc(projectId).stream()
                .map(t -> TaskDto.from(t, today)).toList();
    }

    /** Mes tâches ouvertes, limitées aux projets que je peux encore consulter. */
    public List<TaskDto> findMine() {
        AccessContext ctx = access.context();
        LocalDate today = LocalDate.now(clock);
        return repository.findByAssigneeIdAndStatusNotOrderByDueDateAsc(ctx.user().getId(), TaskStatus.DONE).stream()
                .filter(t -> ctx.can(t.getProject(), Permission.TASK_VIEW))
                .map(t -> TaskDto.from(t, today)).toList();
    }

    /** Tâche existante dont le projet est visible par l'utilisateur courant (404 sinon). */
    public Task getVisible(Long id) {
        Task task = repository.findById(id).orElseThrow(() -> new NotFoundException("Tâche", id));
        access.requireVisible(task.getProject().getId());
        return task;
    }

    @Transactional
    public TaskDto create(Long projectId, TaskRequest request) {
        Project project = access.require(projectId, Permission.TASK_CREATE, ModuleType.BOARD);
        TaskStatus status = Objects.requireNonNullElse(request.status(), TaskStatus.TODO);
        int position = repository.countByProjectIdAndStatus(projectId, status);
        Task task = new Task(project, request.title(), status, position);
        applyDetails(task, request);
        return TaskDto.from(repository.save(task), LocalDate.now(clock));
    }

    @Transactional
    public TaskDto update(Long id, TaskRequest request) {
        Task task = getWith(id, Permission.TASK_EDIT);
        applyDetails(task, request);
        if (request.status() != null && request.status() != task.getStatus()) {
            move(task, request.status(), Integer.MAX_VALUE);
        }
        return TaskDto.from(task, LocalDate.now(clock));
    }

    /** Déplacement Kanban : insère la tâche à l'index demandé et renumérote la colonne cible. */
    @Transactional
    public TaskDto move(Long id, MoveTaskRequest request) {
        Task task = getWith(id, Permission.TASK_EDIT);
        move(task, request.status(), request.position());
        return TaskDto.from(task, LocalDate.now(clock));
    }

    /** Report d'une estimation (appelé après contrôle de permission par le module poker). */
    @Transactional
    public void estimate(Long taskId, Integer points) {
        repository.findById(taskId).ifPresent(t -> t.estimate(points));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getWith(id, Permission.TASK_DELETE));
    }

    private Task getWith(Long id, Permission permission) {
        Task task = repository.findById(id).orElseThrow(() -> new NotFoundException("Tâche", id));
        access.require(task.getProject(), permission);
        return task;
    }

    private void move(Task task, TaskStatus target, int requestedIndex) {
        TaskStatus source = task.getStatus();
        List<Task> column = new ArrayList<>(
                repository.findByProjectIdAndStatusOrderByPositionAsc(task.getProject().getId(), target));
        column.removeIf(t -> t.getId().equals(task.getId()));
        int index = Math.min(Math.max(requestedIndex, 0), column.size());
        column.add(index, task);
        task.moveTo(target, index);
        for (int i = 0; i < column.size(); i++) {
            column.get(i).reposition(i);
        }
        if (source != target) {
            int i = 0;
            for (Task t : repository.findByProjectIdAndStatusOrderByPositionAsc(task.getProject().getId(), source)) {
                if (!t.getId().equals(task.getId())) {
                    t.reposition(i++);
                }
            }
        }
    }

    private void applyDetails(Task task, TaskRequest r) {
        Long orgId = task.getProject().getOrganization().getId();
        AppUser assignee = r.assigneeId() == null ? null : userService.getInOrganization(r.assigneeId(), orgId);
        task.update(r.title(), r.description(), Objects.requireNonNullElse(r.priority(), Priority.MEDIUM),
                assignee, r.storyPoints(), r.dueDate());
    }
}
