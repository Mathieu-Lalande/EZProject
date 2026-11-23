package com.ezproject.todo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/api/todos")
    public List<TodoDto> mine() {
        return todoService.findMine();
    }

    @PostMapping("/api/todos")
    @ResponseStatus(HttpStatus.CREATED)
    public TodoDto create(@Valid @RequestBody TodoRequest request) {
        return todoService.createPersonal(request);
    }

    @GetMapping("/api/projects/{projectId}/todos")
    public List<TodoDto> byProject(@PathVariable Long projectId) {
        return todoService.findByProject(projectId);
    }

    @PostMapping("/api/projects/{projectId}/todos")
    @ResponseStatus(HttpStatus.CREATED)
    public TodoDto createForProject(@PathVariable Long projectId, @Valid @RequestBody TodoRequest request) {
        return todoService.createForProject(projectId, request);
    }

    @PutMapping("/api/todos/{id}")
    public TodoDto update(@PathVariable Long id, @Valid @RequestBody TodoRequest request) {
        return todoService.update(id, request);
    }

    @DeleteMapping("/api/todos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        todoService.delete(id);
    }
}
