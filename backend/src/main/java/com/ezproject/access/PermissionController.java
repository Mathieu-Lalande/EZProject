package com.ezproject.access;

import java.util.Map;
import java.util.Set;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/organization/permissions")
public class PermissionController {

    private final PermissionMatrixService service;

    public PermissionController(PermissionMatrixService service) {
        this.service = service;
    }

    public record MatrixRequest(@NotNull Map<ProjectRole, Set<Permission>> roles) {
    }

    @GetMapping
    public PermissionMatrix get() {
        return service.currentMatrix();
    }

    @PutMapping
    public PermissionMatrix update(@Valid @RequestBody MatrixRequest request) {
        return service.update(request.roles());
    }

    @DeleteMapping
    public PermissionMatrix reset() {
        return service.reset();
    }
}
