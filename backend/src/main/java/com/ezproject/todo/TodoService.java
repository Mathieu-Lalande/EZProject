package com.ezproject.todo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.common.NotFoundException;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.security.CurrentUser;
import com.ezproject.user.AppUser;

@Service
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository repository;
    private final ProjectAccessService access;
    private final CurrentUser currentUser;

    public TodoService(TodoRepository repository, ProjectAccessService access, CurrentUser currentUser) {
        this.repository = repository;
        this.access = access;
        this.currentUser = currentUser;
    }

    public List<TodoDto> findMine() {
        return repository.findByOwnerIdAndProjectIsNullOrderByDoneAscCreatedAtDesc(currentUser.get().getId())
                .stream().map(TodoDto::from).toList();
    }

    public List<TodoDto> findByProject(Long projectId) {
        access.require(projectId, Permission.TODO_VIEW);
        return repository.findByProjectIdOrderByDoneAscCreatedAtDesc(projectId).stream().map(TodoDto::from).toList();
    }

    @Transactional
    public TodoDto createPersonal(TodoRequest request) {
        return TodoDto.from(repository.save(new TodoItem(currentUser.get(), null, request.title(), request.dueDate())));
    }

    @Transactional
    public TodoDto createForProject(Long projectId, TodoRequest request) {
        Project project = access.require(projectId, Permission.TODO_EDIT, ModuleType.TODOS);
        return TodoDto.from(repository.save(new TodoItem(currentUser.get(), project, request.title(), request.dueDate())));
    }

    @Transactional
    public TodoDto update(Long id, TodoRequest request) {
        TodoItem todo = getEditable(id);
        todo.update(request.title(), request.done(), request.dueDate());
        return TodoDto.from(todo);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEditable(id));
    }

    /**
     * Todo personnel : uniquement son propriétaire (sinon 404, pas de fuite d'information / IDOR).
     * Todo de projet : permission TODO_EDIT sur le projet.
     */
    private TodoItem getEditable(Long id) {
        AppUser me = currentUser.get();
        TodoItem todo = repository.findById(id).orElseThrow(() -> new NotFoundException("Todo", id));
        if (todo.isPersonal()) {
            if (!todo.getOwner().getId().equals(me.getId())) {
                throw new NotFoundException("Todo", id);
            }
        } else {
            access.require(todo.getProject(), Permission.TODO_EDIT);
        }
        return todo;
    }
}
