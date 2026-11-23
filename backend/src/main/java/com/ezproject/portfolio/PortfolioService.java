package com.ezproject.portfolio;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.project.Health;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectRepository;
import com.ezproject.retroplanning.PhaseKind;
import com.ezproject.retroplanning.PhaseState;
import com.ezproject.retroplanning.RetroPhase;
import com.ezproject.retroplanning.RetroPhaseRepository;
import com.ezproject.retroplanning.RetroPlanDto;
import com.ezproject.retroplanning.RetroPlanningService;
import com.ezproject.task.TaskRepository;
import com.ezproject.task.TaskStatus;
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
    private final Clock clock;

    public PortfolioService(ProjectRepository projectRepository, TaskRepository taskRepository,
                            RetroPhaseRepository phaseRepository, RetroPlanningService retroPlanningService,
                            AppUserRepository userRepository, Clock clock) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.phaseRepository = phaseRepository;
        this.retroPlanningService = retroPlanningService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public PortfolioDto overview() {
        LocalDate today = LocalDate.now(clock);
        List<Project> projects = projectRepository.findAllByOrderByNameAsc();

        Map<Long, Map<TaskStatus, Long>> statusCounts = new HashMap<>();
        for (TaskRepository.StatusCount c : taskRepository.countByProjectAndStatus()) {
            statusCounts.computeIfAbsent(c.getProjectId(), k -> new EnumMap<>(TaskStatus.class))
                    .put(c.getStatus(), c.getTotal());
        }
        Map<Long, Long> overdue = taskRepository.countOverdueByProject(today).stream()
                .collect(Collectors.toMap(TaskRepository.ProjectCount::getProjectId,
                        TaskRepository.ProjectCount::getTotal));
        Map<Long, List<RetroPhase>> phases = phaseRepository
                .findByProjectIdInOrderBySortOrderAsc(projects.stream().map(Project::getId).toList()).stream()
                .collect(Collectors.groupingBy(p -> p.getProject().getId()));

        List<PortfolioDto.ProjectSummary> summaries = projects.stream()
                .map(p -> summarize(p, statusCounts.getOrDefault(p.getId(), Map.of()),
                        overdue.getOrDefault(p.getId(), 0L), phases.getOrDefault(p.getId(), List.of()), today))
                .toList();

        List<PortfolioDto.ProjectSummary> open = summaries.stream().filter(s -> s.status().isOpen()).toList();
        long openTasks = open.stream()
                .mapToLong(s -> s.tasksTotal() - s.tasksByStatus().getOrDefault(TaskStatus.DONE, 0L)).sum();
        PortfolioDto.Kpis kpis = new PortfolioDto.Kpis(
                open.size(),
                (int) open.stream().filter(PortfolioDto.ProjectSummary::atRisk).count(),
                openTasks,
                open.stream().mapToLong(PortfolioDto.ProjectSummary::overdueTasks).sum(),
                taskRepository.countCompletedSince(clock.instant().minus(Duration.ofDays(7))));

        return new PortfolioDto(kpis, summaries, workload());
    }

    private PortfolioDto.ProjectSummary summarize(Project p, Map<TaskStatus, Long> byStatus, long overdueTasks,
                                                  List<RetroPhase> phases, LocalDate today) {
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

        boolean atRisk = p.getStatus().isOpen() && (p.getHealth() != Health.GREEN || latePhases > 0
                || overdueTasks > 0 || plan.startConflict()
                || (daysRemaining != null && daysRemaining < 0 && progress < 100));

        Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            counts.put(s, byStatus.getOrDefault(s, 0L));
        }
        AppUser owner = p.getOwner();
        return new PortfolioDto.ProjectSummary(p.getId(), p.getCode(), p.getName(), p.getClient(), p.getType(),
                p.getStatus(), p.getHealth(), p.getColor(), owner == null ? null : owner.getDisplayName(),
                p.getStartDate(), p.getEndDate(), daysRemaining, progress, total, counts, overdueTasks, next,
                latePhases, atRisk);
    }

    private List<PortfolioDto.Workload> workload() {
        Map<Long, TaskRepository.Workload> byUser = taskRepository.workloadByAssignee().stream()
                .collect(Collectors.toMap(TaskRepository.Workload::getUserId, w -> w));
        return userRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(u -> {
                    TaskRepository.Workload w = byUser.get(u.getId());
                    return new PortfolioDto.Workload(u.getId(), u.getDisplayName(), u.getJobTitle(),
                            w == null ? 0 : w.getOpenTasks(), w == null ? 0 : w.getOpenPoints());
                })
                .sorted((a, b) -> Long.compare(b.openTasks(), a.openTasks()))
                .toList();
    }
}
