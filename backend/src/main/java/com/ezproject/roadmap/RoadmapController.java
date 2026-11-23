package com.ezproject.roadmap;

import java.time.LocalDate;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ezproject.access.AccessContext;
import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.portfolio.PortfolioService;
import com.ezproject.project.Health;
import com.ezproject.project.ModuleType;
import com.ezproject.project.ProjectPriority;
import com.ezproject.project.ProjectStatus;
import com.ezproject.retroplanning.RetroPlanDto;
import com.ezproject.retroplanning.RetroPlanningService;
import com.ezproject.retroplanning.RetroPhaseRepository;

/** Roadmap multi-projets : périodes, phases et jalons des projets visibles, pour une vue Gantt de portefeuille. */
@RestController
public class RoadmapController {

    public record RoadmapProject(Long id, String code, String name, String color, String teamName, ProjectStatus status,
                                 ProjectPriority priority, Health health, LocalDate startDate, LocalDate endDate,
                                 List<RetroPlanDto.PhaseDto> phases) {
    }

    private final PortfolioService portfolio;
    private final ProjectAccessService access;
    private final RetroPlanningService retroPlanning;
    private final RetroPhaseRepository phases;

    public RoadmapController(PortfolioService portfolio, ProjectAccessService access,
                             RetroPlanningService retroPlanning, RetroPhaseRepository phases) {
        this.portfolio = portfolio;
        this.access = access;
        this.retroPlanning = retroPlanning;
        this.phases = phases;
    }

    @GetMapping("/api/portfolio/roadmap")
    @Transactional(readOnly = true)
    public List<RoadmapProject> roadmap(@RequestParam(required = false) Long teamId) {
        AccessContext ctx = access.context();
        return portfolio.visibleProjects(teamId).stream()
                .filter(p -> p.getStatus().isOpen())
                .map(p -> {
                    boolean showPhases = p.hasModule(ModuleType.RETRO_PLANNING) && ctx.can(p, Permission.RETRO_VIEW);
                    List<RetroPlanDto.PhaseDto> planned = showPhases
                            ? retroPlanning.buildPlan(p, phases.findByProjectIdOrderBySortOrderAsc(p.getId())).phases()
                            : List.of();
                    return new RoadmapProject(p.getId(), p.getCode(), p.getName(), p.getColor(),
                            p.getTeam() == null ? null : p.getTeam().getName(), p.getStatus(), p.getPriority(),
                            p.getHealth(), p.getStartDate(), p.getEndDate(), planned);
                })
                .toList();
    }
}
