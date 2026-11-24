package com.ezproject.report;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ezproject.report.ReportDtos.CopilDto;
import com.ezproject.report.ReportDtos.ReportDto;
import com.ezproject.report.ReportDtos.ReportRequest;

import jakarta.validation.Valid;

@RestController
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{projectId}/reports")
    public List<ReportDto> history(@PathVariable Long projectId, @RequestParam(defaultValue = "20") int limit) {
        return service.history(projectId, limit);
    }

    @PostMapping("/api/projects/{projectId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDto publish(@PathVariable Long projectId, @Valid @RequestBody ReportRequest request) {
        return service.publish(projectId, request);
    }

    @GetMapping("/api/projects/{projectId}/copil")
    public CopilDto copil(@PathVariable Long projectId) {
        return service.copil(projectId);
    }
}
