package com.ezproject.task;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.common.NotFoundException;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectService;
import com.ezproject.security.CurrentUser;
import com.ezproject.user.AppUser;
import com.ezproject.user.UserService;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository repository;
    private final ProjectService projectService;
    private final UserService userService;
    private final CurrentUser currentUser;
    private final Clock clock;

    public TaskService(TaskRepository repository, ProjectService projectService, UserService userService,
                       CurrentUser currentUser, Clock clock) {
        this.repository = repository;
        this.projectService = projectService;
        this.userService = userService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public List<TaskDto> findByProject(Long projectId) {
        projectService.getEntity(projectId);
        LocalDate today = LocalDate.now(clock);
        return repository.findByProjectIdOrderByPositionAsc(projectId).stream()
                .map(t -> TaskDto.from(t, today)).toList();
    }

    public List<TaskDto> findMine() {
        LocalDate today = LocalDate.now(clock);
        return repository.findByAssigneeIdAndStatusNotOrderByDueDateAsc(currentUser.get().getId(), TaskStatus.DONE)
                .stream().map(t -> TaskDto.from(t, today)).toList();
    }

    public Task getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Tâche", id));
    }

    @Transactional
    public TaskDto create(Long projectId, TaskRequest request) {
        Project project = projectService.requireModule(projectId, ModuleType.BOARD);
        TaskStatus status = Objects.requireNonNullElse(request.status(), TaskStatus.TODO);
        int position = repository.countByProjectIdAndStatus(projectId, status);
        Task task = new Task(project, request.title(), status, position);
        applyDetails(task, request);
        return TaskDto.from(repository.save(task), LocalDate.now(clock));
    }

    @Transactional
    public TaskDto update(Long id, TaskRequest request) {
        Task task = getEntity(id);
        applyDetails(task, request);
        if (request.status() != null && request.status() != task.getStatus()) {
            move(task, request.status(), Integer.MAX_VALUE);
        }
        return TaskDto.from(task, LocalDate.now(clock));
    }

    /** Déplacement Kanban : insère la tâche à l'index demandé et renumérote la colonne cible. */
    @Transactional
    public TaskDto move(Long id, MoveTaskRequest request) {
        Task task = getEntity(id);
        move(task, request.status(), request.position());
        return TaskDto.from(task, LocalDate.now(clock));
    }

    @Transactional
    public void estimate(Long taskId, Integer points) {
        getEntity(taskId).estimate(points);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
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
            List<Task> sourceColumn = repository.findByProjectIdAndStatusOrderByPositionAsc(
                    task.getProject().getId(), source);
            int i = 0;
            for (Task t : sourceColumn) {
                if (!t.getId().equals(task.getId())) {
                    t.reposition(i++);
                }
            }
        }
    }

    private void applyDetails(Task task, TaskRequest r) {
        AppUser assignee = r.assigneeId() == null ? null : userService.getEntity(r.assigneeId());
        task.update(r.title(), r.description(), Objects.requireNonNullElse(r.priority(), Priority.MEDIUM),
                assignee, r.storyPoints(), r.dueDate());
    }
}
