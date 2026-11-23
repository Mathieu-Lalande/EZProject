package com.ezproject.portfolio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.ezproject.project.Health;
import com.ezproject.project.ProjectStatus;
import com.ezproject.project.ProjectType;
import com.ezproject.task.TaskStatus;

/** Vue consolidée du portefeuille de projets (pilotage COO / PMO / office manager). */
public record PortfolioDto(Kpis kpis, List<ProjectSummary> projects, List<Workload> workload) {

    public record Kpis(int openProjects, int atRiskProjects, long openTasks, long overdueTasks,
                       long completedLast7Days) {
    }

    public record ProjectSummary(
            Long id, String code, String name, String client, ProjectType type, ProjectStatus status,
            Health health, String color, String ownerName, LocalDate startDate, LocalDate endDate,
            Long daysRemaining, int progress, long tasksTotal, Map<TaskStatus, Long> tasksByStatus,
            long overdueTasks, Milestone nextMilestone, long latePhases, boolean atRisk) {
    }

    public record Milestone(String name, LocalDate date) {
    }

    public record Workload(Long userId, String displayName, String jobTitle, long openTasks, long openPoints) {
    }
}
