package com.ezproject.project;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Les autorisations sont appliquées dans {@link ProjectService} (rôle effectif + matrice de permissions). */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectDto> list() {
        return projectService.findAll();
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable Long id) {
        return projectService.get(id);
    }

    @GetMapping("/{id}/history")
    public List<StatusChangeDto> history(@PathVariable Long id) {
        return projectService.history(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(@Valid @RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProjectDto changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return projectService.changeStatus(id, request);
    }

    @PatchMapping("/{id}/priority")
    public ProjectDto changePriority(@PathVariable Long id, @Valid @RequestBody PriorityRequest request) {
        return projectService.changePriority(id, request.priority());
    }

    @PutMapping("/{id}/modules")
    public ProjectDto updateModules(@PathVariable Long id, @Valid @RequestBody ModulesRequest request) {
        return projectService.updateModules(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.delete(id);
    }
}
