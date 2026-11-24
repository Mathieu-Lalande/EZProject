package com.ezproject.organization;

import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final OrganizationLifecycleService lifecycle;

    public OrganizationController(OrganizationService service, OrganizationLifecycleService lifecycle) {
        this.service = service;
        this.lifecycle = lifecycle;
    }

    public record DeleteRequest(@NotBlank @Size(max = 150) String confirmationName) {
    }

    @GetMapping("/api/organization/export")
    public ResponseEntity<Map<String, Object>> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("organisation-ezproject.json").build().toString())
                .body(lifecycle.export());
    }

    @DeleteMapping("/api/organization")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Valid @RequestBody DeleteRequest request, jakarta.servlet.http.HttpServletRequest http) {
        lifecycle.delete(request.confirmationName());
        var session = http.getSession(false);
        if (session != null) {
            session.invalidate();
        }
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
