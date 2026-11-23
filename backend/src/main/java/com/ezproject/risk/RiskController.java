package com.ezproject.risk;

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

import com.ezproject.risk.RiskDtos.DecisionDto;
import com.ezproject.risk.RiskDtos.DecisionRequest;
import com.ezproject.risk.RiskDtos.RiskDto;
import com.ezproject.risk.RiskDtos.RiskRequest;

import jakarta.validation.Valid;

@RestController
public class RiskController {

    private final RiskService service;

    public RiskController(RiskService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{projectId}/risks")
    public List<RiskDto> risks(@PathVariable Long projectId) {
        return service.risks(projectId);
    }

    @PostMapping("/api/projects/{projectId}/risks")
    @ResponseStatus(HttpStatus.CREATED)
    public RiskDto createRisk(@PathVariable Long projectId, @Valid @RequestBody RiskRequest request) {
        return service.createRisk(projectId, request);
    }

    @PutMapping("/api/risks/{id}")
    public RiskDto updateRisk(@PathVariable Long id, @Valid @RequestBody RiskRequest request) {
        return service.updateRisk(id, request);
    }

    @DeleteMapping("/api/risks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRisk(@PathVariable Long id) {
        service.deleteRisk(id);
    }

    @GetMapping("/api/projects/{projectId}/decisions")
    public List<DecisionDto> decisions(@PathVariable Long projectId) {
        return service.decisions(projectId);
    }

    @PostMapping("/api/projects/{projectId}/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    public DecisionDto createDecision(@PathVariable Long projectId, @Valid @RequestBody DecisionRequest request) {
        return service.createDecision(projectId, request);
    }

    @PutMapping("/api/decisions/{id}")
    public DecisionDto updateDecision(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return service.updateDecision(id, request);
    }

    @DeleteMapping("/api/decisions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDecision(@PathVariable Long id) {
        service.deleteDecision(id);
    }
}
