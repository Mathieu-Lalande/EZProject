package com.ezproject.portfolio;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.AccessContext;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.project.Health;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectRepository;
import com.ezproject.project.ProjectStatus;
import com.ezproject.retroplanning.PhaseKind;
import com.ezproject.retroplanning.PhaseState;
import com.ezproject.retroplanning.RetroPhase;
import com.ezproject.retroplanning.RetroPhaseRepository;
import com.ezproject.retroplanning.RetroPlanDto;
import com.ezproject.retroplanning.RetroPlanningService;
import com.ezproject.task.TaskRepository;
import com.ezproject.time.TimeService;
import com.ezproject.task.TaskStatus;
import com.ezproject.team.Team;
import com.ezproject.team.TeamRepository;
import com.ezproject.user.AppUser;
import com.ezproject.user.AppUserRepository;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final RetroPhaseRepository phaseRepository;
    private final RetroPlanningService retroPlanningService;
    private final AppUserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ProjectAccessService access;
    private final Clock clock;
    private final TimeService timeService;

    public PortfolioService(ProjectRepository projectRepository, TaskRepository taskRepository,
                            RetroPhaseRepository phaseRepository, RetroPlanningService retroPlanningService,
                            AppUserRepository userRepository, TeamRepository teamRepository,
                            ProjectAccessService access, Clock clock, TimeService timeService) {
        this.timeService = timeService;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.phaseRepository = phaseRepository;
        this.retroPlanningService = retroPlanningService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.access = access;
        this.clock = clock;
    }

    /** Projets du portefeuille visibles par l'utilisateur courant, éventuellement filtrés par équipe. */
    public List<Project> visibleProjects(Long teamId) {
        AccessContext ctx = access.context();
        return projectRepository.findByOrganizationIdOrderByNameAsc(ctx.user().getOrganization().getId()).stream()
                .filter(ctx::canSee)
                .filter(p -> teamId == null || (p.getTeam() != null && p.getTeam().getId().equals(teamId)))
                .toList();
    }

    public PortfolioDto overview(Long teamId) {
        AccessContext ctx = access.context();
        AppUser me = ctx.user();
        LocalDate today = LocalDate.now(clock);
        List<Project> projects = visibleProjects(teamId);
        List<Long> ids = projects.stream().map(Project::getId).toList();
        List<PortfolioDto.ProjectSummary> summaries = summarize(projects, today);

        List<PortfolioDto.ProjectSummary> open = summaries.stream().filter(s -> s.status().isOpen()).toList();
        long openTasks = open.stream()
                .mapToLong(s -> s.tasksTotal() - s.tasksByStatus().getOrDefault(TaskStatus.DONE, 0L)).sum();
        PortfolioDto.Kpis kpis = new PortfolioDto.Kpis(
                open.size(),
                (int) open.stream().filter(PortfolioDto.ProjectSummary::atRisk).count(),
                (int) open.stream().filter(s -> s.status() == ProjectStatus.ON_HOLD).count(),
                (int) open.stream().filter(s -> s.priority() == com.ezproject.project.ProjectPriority.P1).count(),
                openTasks,
                open.stream().mapToLong(PortfolioDto.ProjectSummary::overdueTasks).sum(),
                ids.isEmpty() ? 0 : taskRepository.countCompletedSince(ids, clock.instant().minus(Duration.ofDays(7))),
                (int) open.stream().filter(s -> s.budgetPct() != null && s.budgetPct() > 100).count());

        return new PortfolioDto(
                me.getRole().seesWholePortfolio() ? PortfolioDto.Scope.ORGANIZATION : PortfolioDto.Scope.MY_PROJECTS,
                kpis, summaries, workload(me, ids), teams(me, summaries));
    }

    /** Synthèse par projet, triée par priorité puis échéance (ordre d'arbitrage). */
    public List<PortfolioDto.ProjectSummary> summarize(List<Project> projects, LocalDate today) {
        List<Long> ids = projects.stream().map(Project::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Map<TaskStatus, Long>> statusCounts = new HashMap<>();
        for (TaskRepository.StatusCount c : taskRepository.countByProjectAndStatus(ids)) {
            statusCounts.computeIfAbsent(c.getProjectId(), k -> new EnumMap<>(TaskStatus.class))
                    .put(c.getStatus(), c.getTotal());
        }
        Map<Long, Long> overdue = taskRepository.countOverdueByProject(ids, today).stream()
                .collect(Collectors.toMap(TaskRepository.ProjectCount::getProjectId,
                        TaskRepository.ProjectCount::getTotal));
        Map<Long, List<RetroPhase>> phases = phaseRepository.findByProjectIdInOrderBySortOrderAsc(ids).stream()
                .collect(Collectors.groupingBy(p -> p.getProject().getId()));
        Map<Long, java.math.BigDecimal> consumed = timeService.consumedByProject(ids);
        return projects.stream()
                .map(p -> summarize(p, statusCounts.getOrDefault(p.getId(), Map.of()),
                        overdue.getOrDefault(p.getId(), 0L), phases.getOrDefault(p.getId(), List.of()), today,
                        consumed.getOrDefault(p.getId(), java.math.BigDecimal.ZERO)))
                .sorted(Comparator.comparing(PortfolioDto.ProjectSummary::priority)
                        .thenComparing(s -> s.endDate() == null ? LocalDate.MAX : s.endDate()))
                .toList();
    }

    private PortfolioDto.ProjectSummary summarize(Project p, Map<TaskStatus, Long> byStatus, long overdueTasks,
                                                  List<RetroPhase> phases, LocalDate today,
                                                  java.math.BigDecimal consumedDays) {
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long done = byStatus.getOrDefault(TaskStatus.DONE, 0L);
        int progress = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);

        RetroPlanDto plan = retroPlanningService.buildPlan(p, phases);
        PortfolioDto.Milestone next = plan.phases().stream()
                .filter(ph -> ph.kind() == PhaseKind.MILESTONE && !ph.done() && ph.start() != null)
                .filter(ph -> !ph.start().isBefore(today))
                .findFirst()
                .map(ph -> new PortfolioDto.Milestone(ph.name(), ph.start()))
                .orElse(null);
        long latePhases = plan.phases().stream().filter(ph -> ph.state() == PhaseState.LATE).count();
        Long daysRemaining = p.getEndDate() == null ? null : ChronoUnit.DAYS.between(today, p.getEndDate());

        Integer budgetPct = p.getBudgetDays() == null || p.getBudgetDays() == 0 ? null
                : consumedDays.multiply(java.math.BigDecimal.valueOf(100))
                        .divide(java.math.BigDecimal.valueOf(p.getBudgetDays()), 0, java.math.RoundingMode.HALF_UP).intValue();
        // Dérive budgétaire : budget dépassé, ou consommation en avance de plus de 25 points sur l'avancement
        boolean budgetDrift = budgetPct != null && (budgetPct > 100 || (total > 0 && budgetPct - progress > 25));
        // Un projet en pause n'est pas "à risque" du seul fait de ses retards : il est arbitré
        boolean atRisk = p.getStatus().isOpen() && p.getStatus() != ProjectStatus.ON_HOLD
                && (p.getHealth() != Health.GREEN || latePhases > 0 || overdueTasks > 0 || plan.startConflict()
                || budgetDrift || (daysRemaining != null && daysRemaining < 0 && progress < 100));

        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            counts.put(s, byStatus.getOrDefault(s, 0L));
        }
        AppUser owner = p.getOwner();
        Team team = p.getTeam();
        return new PortfolioDto.ProjectSummary(p.getId(), p.getCode(), p.getName(), p.getClient(), p.getType(),
                p.getStatus(), p.getStatusReason(), p.getPriority(), p.getHealth(), p.getColor(),
                owner == null ? null : owner.getDisplayName(), team == null ? null : team.getId(),
                team == null ? null : team.getName(), p.getStartDate(), p.getEndDate(), daysRemaining, progress,
                total, counts, overdueTasks, next, latePhases, atRisk, p.getBudgetDays(), consumedDays, budgetPct);
    }

    private List<PortfolioDto.Workload> workload(AppUser me, List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return List.of();
        }
        Map<Long, TaskRepository.Workload> byUser = taskRepository.workloadByAssignee(projectIds).stream()
                .collect(Collectors.toMap(TaskRepository.Workload::getUserId, w -> w));
        return userRepository.findByOrganizationIdOrderByDisplayNameAsc(me.getOrganization().getId()).stream()
                .filter(u -> me.getRole().seesWholePortfolio() || byUser.containsKey(u.getId()))
                .map(u -> {
                    TaskRepository.Workload w = byUser.get(u.getId());
                    return new PortfolioDto.Workload(u.getId(), u.getDisplayName(), u.getJobTitle(),
                            w == null ? 0 : w.getOpenTasks(), w == null ? 0 : w.getOpenPoints());
                })
                .sorted((a, b) -> Long.compare(b.openTasks(), a.openTasks()))
                .toList();
    }

    private List<PortfolioDto.TeamSummary> teams(AppUser me, List<PortfolioDto.ProjectSummary> summaries) {
        return teamRepository.findByOrganizationIdOrderByNameAsc(me.getOrganization().getId()).stream()
                .map(t -> {
                    var mine = summaries.stream()
                            .filter(s -> t.getId().equals(s.teamId()) && s.status().isOpen()).toList();
                    return new PortfolioDto.TeamSummary(t.getId(), t.getName(), t.getColor(), mine.size(),
                            (int) mine.stream().filter(PortfolioDto.ProjectSummary::atRisk).count());
                })
                .filter(t -> me.getRole().seesWholePortfolio() || t.openProjects() > 0)
                .toList();
    }
}
