package com.ezproject.portfolio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.ezproject.project.Health;
import com.ezproject.project.ProjectPriority;
import com.ezproject.project.ProjectStatus;
import com.ezproject.project.ProjectType;
import com.ezproject.task.TaskStatus;

/**
 * Vue consolidée du portefeuille. Le périmètre dépend de l'utilisateur : toute l'organisation pour
 * les managers et administrateurs, les projets visibles (équipes, invitations) pour les autres.
 */
public record PortfolioDto(Scope scope, Kpis kpis, List<ProjectSummary> projects, List<Workload> workload,
                           List<TeamSummary> teams) {

    public enum Scope { ORGANIZATION, MY_PROJECTS }

    public record Kpis(int openProjects, int atRiskProjects, int onHoldProjects, int p1Projects, long openTasks,
                       long overdueTasks, long completedLast7Days, int overBudgetProjects) {
    }

    public record ProjectSummary(
            Long id, String code, String name, String client, ProjectType type, ProjectStatus status,
            String statusReason, ProjectPriority priority, Health health, String color, String ownerName,
            Long teamId, String teamName, LocalDate startDate, LocalDate endDate, Long daysRemaining, int progress,
            long tasksTotal, Map<TaskStatus, Long> tasksByStatus, long overdueTasks, Milestone nextMilestone,
            long latePhases, boolean atRisk, Integer budgetDays, java.math.BigDecimal consumedDays, Integer budgetPct) {
    }

    public record Milestone(String name, LocalDate date) {
    }

    public record Workload(Long userId, String displayName, String jobTitle, long openTasks, long openPoints) {
    }

    public record TeamSummary(Long id, String name, String color, int openProjects, int atRiskProjects) {
    }
}
