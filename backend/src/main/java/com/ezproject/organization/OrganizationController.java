package com.ezproject.organization;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    public record RenameRequest(@NotBlank @Size(min = 2, max = 150) String name) {
    }

    public record PlanRequest(@NotNull Plan plan) {
    }

    /** Inscription publique : création d'une organisation et de son propriétaire. */
    @PostMapping("/api/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest request) {
        service.signup(request);
    }

    @GetMapping("/api/organization")
    public OrganizationDto current() {
        return service.current();
    }

    @PutMapping("/api/organization")
    public OrganizationDto rename(@Valid @RequestBody RenameRequest request) {
        return service.rename(request.name());
    }

    @PutMapping("/api/organization/plan")
    public OrganizationDto changePlan(@Valid @RequestBody PlanRequest request) {
        return service.changePlan(request.plan());
    }
}
