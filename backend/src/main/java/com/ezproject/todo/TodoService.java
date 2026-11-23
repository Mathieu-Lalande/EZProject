package com.ezproject.todo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.common.NotFoundException;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectService;
import com.ezproject.security.CurrentUser;
import com.ezproject.user.AppUser;

@Service
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository repository;
    private final ProjectService projectService;
    private final CurrentUser currentUser;

    public TodoService(TodoRepository repository, ProjectService projectService, CurrentUser currentUser) {
        this.repository = repository;
        this.projectService = projectService;
        this.currentUser = currentUser;
    }

    public List<TodoDto> findMine() {
        return repository.findByOwnerIdAndProjectIsNullOrderByDoneAscCreatedAtDesc(currentUser.get().getId())
                .stream().map(TodoDto::from).toList();
    }

    public List<TodoDto> findByProject(Long projectId) {
        projectService.getEntity(projectId);
        return repository.findByProjectIdOrderByDoneAscCreatedAtDesc(projectId).stream().map(TodoDto::from).toList();
    }

    @Transactional
    public TodoDto createPersonal(TodoRequest request) {
        return TodoDto.from(repository.save(new TodoItem(currentUser.get(), null, request.title(), request.dueDate())));
    }

    @Transactional
    public TodoDto createForProject(Long projectId, TodoRequest request) {
        Project project = projectService.requireModule(projectId, ModuleType.TODOS);
        return TodoDto.from(repository.save(new TodoItem(currentUser.get(), project, request.title(), request.dueDate())));
    }

    @Transactional
    public TodoDto update(Long id, TodoRequest request) {
        TodoItem todo = getVisible(id);
        todo.update(request.title(), request.done(), request.dueDate());
        return TodoDto.from(todo);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getVisible(id));
    }

    /** Un todo personnel d'un autre utilisateur est traité comme inexistant (pas de fuite d'information, IDOR). */
    private TodoItem getVisible(Long id) {
        AppUser me = currentUser.get();
        return repository.findById(id)
                .filter(t -> t.isVisibleTo(me))
                .orElseThrow(() -> new NotFoundException("Todo", id));
    }
}
