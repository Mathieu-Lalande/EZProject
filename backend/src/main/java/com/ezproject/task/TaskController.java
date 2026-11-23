package com.ezproject.task;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public List<TaskDto> list(@PathVariable Long projectId) {
        return taskService.findByProject(projectId);
    }

    @GetMapping("/api/tasks/mine")
    public List<TaskDto> mine() {
        return taskService.findMine();
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto create(@PathVariable Long projectId, @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, request);
    }

    @PutMapping("/api/tasks/{id}")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/api/tasks/{id}/move")
    public TaskDto move(@PathVariable Long id, @Valid @RequestBody MoveTaskRequest request) {
        return taskService.move(id, request);
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}
