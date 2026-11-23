package com.ezproject.access;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMembersController {

    private final ProjectMembersService service;

    public ProjectMembersController(ProjectMembersService service) {
        this.service = service;
    }

    public record MemberRequest(@NotNull ProjectRole role) {
    }

    @GetMapping
    public ProjectMembersDto list(@PathVariable Long projectId) {
        return service.list(projectId);
    }

    @PutMapping("/{userId}")
    public ProjectMembersDto put(@PathVariable Long projectId, @PathVariable Long userId,
                                 @Valid @RequestBody MemberRequest request) {
        return service.put(projectId, userId, request.role());
    }

    @DeleteMapping("/{userId}")
    public ProjectMembersDto remove(@PathVariable Long projectId, @PathVariable Long userId) {
        return service.remove(projectId, userId);
    }
}
