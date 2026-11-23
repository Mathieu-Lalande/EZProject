package com.ezproject.retroplanning;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ezproject.project.ProjectType;

import jakarta.validation.Valid;

@RestController
public class RetroPlanningController {

    private final RetroPlanningService service;

    public RetroPlanningController(RetroPlanningService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{projectId}/retro-plan")
    public RetroPlanDto get(@PathVariable Long projectId) {
        return service.getPlan(projectId);
    }

    @PostMapping("/api/projects/{projectId}/retro-plan/phases")
    public RetroPlanDto addPhase(@PathVariable Long projectId, @Valid @RequestBody PhaseRequest request) {
        return service.addPhase(projectId, request);
    }

    @PostMapping("/api/projects/{projectId}/retro-plan/template")
    public RetroPlanDto applyTemplate(@PathVariable Long projectId, @RequestParam ProjectType type) {
        return service.applyTemplate(projectId, type);
    }

    @PutMapping("/api/projects/{projectId}/retro-plan/order")
    public RetroPlanDto reorder(@PathVariable Long projectId, @Valid @RequestBody ReorderRequest request) {
        return service.reorder(projectId, request);
    }

    @PutMapping("/api/retro-phases/{phaseId}")
    public RetroPlanDto updatePhase(@PathVariable Long phaseId, @Valid @RequestBody PhaseRequest request) {
        return service.updatePhase(phaseId, request);
    }

    @DeleteMapping("/api/retro-phases/{phaseId}")
    public RetroPlanDto deletePhase(@PathVariable Long phaseId) {
        return service.deletePhase(phaseId);
    }
}
