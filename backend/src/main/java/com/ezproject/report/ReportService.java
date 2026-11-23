package com.ezproject.report;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccess;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.audit.AuditService;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectDto;
import com.ezproject.report.ReportDtos.CopilDto;
import com.ezproject.report.ReportDtos.ReportDto;
import com.ezproject.report.ReportDtos.ReportRequest;
import com.ezproject.report.ReportDtos.TaskStats;
import com.ezproject.retroplanning.RetroPlanningService;
import com.ezproject.risk.RiskService;
import com.ezproject.task.Task;
import com.ezproject.task.TaskRepository;
import com.ezproject.task.TaskStatus;
import com.ezproject.time.TimeService;

/** Météo projet historisée et rapport de comité de pilotage (COPIL). */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final StatusReportRepository reports;
    private final ProjectAccessService access;
    private final AuditService audit;
    private final TaskRepository tasks;
    private final RetroPlanningService retroPlanning;
    private final RiskService risks;
    private final TimeService time;
    private final Clock clock;

    public ReportService(StatusReportRepository reports, ProjectAccessService access, AuditService audit,
                         TaskRepository tasks, RetroPlanningService retroPlanning, RiskService risks, TimeService time,
                         Clock clock) {
        this.reports = reports;
        this.access = access;
        this.audit = audit;
        this.tasks = tasks;
        this.retroPlanning = retroPlanning;
        this.risks = risks;
        this.time = time;
        this.clock = clock;
    }

    public List<ReportDto> history(Long projectId, int limit) {
        access.requireVisible(projectId);
        return reports.findByProjectIdOrderByReportDateDescIdDesc(projectId, PageRequest.of(0, Math.min(limit, 52)))
                .stream().map(ReportDto::from).toList();
    }

    /** Publie la météo : elle devient la santé courante du projet et rejoint l'historique. */
    @Transactional
    public ReportDto publish(Long projectId, ReportRequest r) {
        Project project = access.require(projectId, Permission.REPORT_EDIT);
        StatusReport report = reports.save(new StatusReport(project, access.context().user(), LocalDate.now(clock),
                r.health(), r.trend(), r.summary().trim(), blank(r.achievements()), blank(r.nextSteps()),
                blank(r.attentionPoints())));
        project.changeHealth(r.health());
        audit.record("REPORT_PUBLISHED", "PROJECT", projectId, project.getCode() + " : météo " + r.health()
                + " (" + r.trend() + ")");
        return ReportDto.from(report);
    }

    /** Dernière météo publiée par projet (portefeuille). */
    public Map<Long, StatusReport> latestByProject(List<Long> projectIds) {
        Map<Long, StatusReport> result = new HashMap<>();
        if (!projectIds.isEmpty()) {
            reports.latestFor(projectIds).forEach(r -> result.put(r.getProject().getId(), r));
        }
        return result;
    }

    public CopilDto copil(Long projectId) {
        Project project = access.requireVisible(projectId);
        ProjectAccess acc = access.accessTo(project);
        List<ReportDto> history = history(projectId, 6);
        TaskStats stats = null;
        if (acc.can(Permission.TASK_VIEW)) {
            LocalDate today = LocalDate.now(clock);
            List<Task> all = tasks.findByProjectIdOrderByPositionAsc(projectId);
            long done = all.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            stats = new TaskStats(all.size(), done, all.stream().filter(t -> t.isOverdue(today)).count(),
                    all.isEmpty() ? 0 : (int) Math.round(done * 100.0 / all.size()));
        }
        boolean riskModule = project.hasModule(ModuleType.RISKS) && acc.can(Permission.RISK_VIEW);
        return new CopilDto(ProjectDto.from(project, acc), history.isEmpty() ? null : history.getFirst(), history, stats,
                acc.can(Permission.RETRO_VIEW) && project.hasModule(ModuleType.RETRO_PLANNING) ? retroPlanning.getPlan(projectId) : null,
                riskModule ? risks.risks(projectId).stream().filter(x -> x.status().isActive()).limit(8).toList() : null,
                riskModule ? risks.decisions(projectId).stream().limit(8).toList() : null,
                acc.can(Permission.TIME_VIEW) && project.hasModule(ModuleType.TIME_TRACKING) ? time.summary(projectId) : null,
                LocalDate.now(clock));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
