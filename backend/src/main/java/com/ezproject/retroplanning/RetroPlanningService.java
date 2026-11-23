package com.ezproject.retroplanning;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectType;
import com.ezproject.retroplanning.RetroPlanDto.PhaseDto;
import com.ezproject.retroplanning.RetroPlanningCalculator.PhaseInput;
import com.ezproject.retroplanning.RetroPlanningCalculator.ScheduledPhase;

@Service
@Transactional(readOnly = true)
public class RetroPlanningService {

    private final RetroPhaseRepository repository;
    private final ProjectAccessService access;
    private final RetroPlanningCalculator calculator;
    private final Clock clock;

    public RetroPlanningService(RetroPhaseRepository repository, ProjectAccessService access,
                                RetroPlanningCalculator calculator, Clock clock) {
        this.repository = repository;
        this.access = access;
        this.calculator = calculator;
        this.clock = clock;
    }

    public RetroPlanDto getPlan(Long projectId) {
        Project project = access.require(projectId, Permission.RETRO_VIEW);
        return buildPlan(project, repository.findByProjectIdOrderBySortOrderAsc(projectId));
    }

    /** Construit le plan daté d'un projet (réutilisé par la vue portefeuille). */
    public RetroPlanDto buildPlan(Project project, List<RetroPhase> phases) {
        LocalDate today = LocalDate.now(clock);
        LocalDate deadline = project.getEndDate();
        int total = phases.stream().mapToInt(RetroPhase::getDurationDays).sum();
        if (deadline == null) {
            List<PhaseDto> undated = phases.stream()
                    .map(p -> new PhaseDto(p.getId(), p.getName(), p.getKind(), p.getDurationDays(), p.isDone(),
                            null, null, p.isDone() ? PhaseState.DONE : PhaseState.UNSCHEDULED))
                    .toList();
            return new RetroPlanDto(null, project.getStartDate(), null, false, total, undated);
        }
        List<PhaseInput> inputs = phases.stream()
                .map(p -> new PhaseInput(p.getId(), p.getName(), p.getKind(), p.getDurationDays(), p.isDone()))
                .toList();
        List<PhaseDto> scheduled = calculator.scheduleBackward(deadline, inputs).stream()
                .map(s -> new PhaseDto(s.id(), s.name(), s.kind(), s.durationDays(), s.done(), s.start(), s.end(),
                        stateOf(s, today)))
                .toList();
        LocalDate computedStart = scheduled.isEmpty() ? null : scheduled.getFirst().start();
        boolean conflict = computedStart != null && project.getStartDate() != null
                && computedStart.isBefore(project.getStartDate());
        return new RetroPlanDto(deadline, project.getStartDate(), computedStart, conflict, total, scheduled);
    }

    @Transactional
    public RetroPlanDto addPhase(Long projectId, PhaseRequest request) {
        Project project = access.require(projectId, Permission.RETRO_EDIT, ModuleType.RETRO_PLANNING);
        int order = repository.findByProjectIdOrderBySortOrderAsc(projectId).size();
        RetroPhase phase = new RetroPhase(project, request.name(), request.kind(), request.durationDays(), order);
        phase.update(request.name(), request.kind(), request.durationDays(), request.done());
        repository.save(phase);
        return getPlan(projectId);
    }

    @Transactional
    public RetroPlanDto updatePhase(Long phaseId, PhaseRequest request) {
        RetroPhase phase = getEditable(phaseId);
        phase.update(request.name(), request.kind(), request.durationDays(), request.done());
        return getPlan(phase.getProject().getId());
    }

    @Transactional
    public RetroPlanDto deletePhase(Long phaseId) {
        RetroPhase phase = getEditable(phaseId);
        Long projectId = phase.getProject().getId();
        repository.delete(phase);
        repository.flush();
        List<RetroPhase> remaining = repository.findByProjectIdOrderBySortOrderAsc(projectId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).reorder(i);
        }
        return getPlan(projectId);
    }

    @Transactional
    public RetroPlanDto reorder(Long projectId, ReorderRequest request) {
        access.require(projectId, Permission.RETRO_EDIT);
        List<RetroPhase> phases = repository.findByProjectIdOrderBySortOrderAsc(projectId);
        Map<Long, RetroPhase> byId = phases.stream().collect(Collectors.toMap(RetroPhase::getId, Function.identity()));
        if (request.phaseIds().size() != phases.size() || !byId.keySet().containsAll(request.phaseIds())) {
            throw new BusinessRuleException("La liste des phases à réordonner est incomplète");
        }
        for (int i = 0; i < request.phaseIds().size(); i++) {
            byId.get(request.phaseIds().get(i)).reorder(i);
        }
        return getPlan(projectId);
    }

    /** Applique un gabarit de phases type (développement ou conseil) sur un rétro-planning vide. */
    @Transactional
    public RetroPlanDto applyTemplate(Long projectId, ProjectType type) {
        Project project = access.require(projectId, Permission.RETRO_EDIT, ModuleType.RETRO_PLANNING);
        if (!repository.findByProjectIdOrderBySortOrderAsc(projectId).isEmpty()) {
            throw new BusinessRuleException("Le rétro-planning contient déjà des phases");
        }
        List<PhaseRequest> template = switch (type) {
            case DEVELOPMENT -> List.of(
                    new PhaseRequest("Cadrage", PhaseKind.PHASE, 5, false),
                    new PhaseRequest("Conception", PhaseKind.PHASE, 10, false),
                    new PhaseRequest("Développement", PhaseKind.PHASE, 30, false),
                    new PhaseRequest("Recette", PhaseKind.PHASE, 10, false),
                    new PhaseRequest("Go / No-Go", PhaseKind.MILESTONE, 0, false),
                    new PhaseRequest("Mise en production", PhaseKind.PHASE, 2, false));
            case CONSULTING -> List.of(
                    new PhaseRequest("Kick-off", PhaseKind.MILESTONE, 0, false),
                    new PhaseRequest("Diagnostic", PhaseKind.PHASE, 10, false),
                    new PhaseRequest("Ateliers", PhaseKind.PHASE, 10, false),
                    new PhaseRequest("Recommandations", PhaseKind.PHASE, 8, false),
                    new PhaseRequest("Restitution COPIL", PhaseKind.MILESTONE, 0, false));
            case INTERNAL -> List.of(
                    new PhaseRequest("Préparation", PhaseKind.PHASE, 5, false),
                    new PhaseRequest("Réalisation", PhaseKind.PHASE, 15, false),
                    new PhaseRequest("Livraison", PhaseKind.MILESTONE, 0, false));
        };
        for (int i = 0; i < template.size(); i++) {
            PhaseRequest r = template.get(i);
            repository.save(new RetroPhase(project, r.name(), r.kind(), r.durationDays(), i));
        }
        return getPlan(projectId);
    }

    private RetroPhase getEditable(Long id) {
        RetroPhase phase = repository.findById(id).orElseThrow(() -> new NotFoundException("Phase", id));
        access.require(phase.getProject(), Permission.RETRO_EDIT);
        return phase;
    }

    private static PhaseState stateOf(ScheduledPhase s, LocalDate today) {
        if (s.done()) {
            return PhaseState.DONE;
        }
        if (s.end().isBefore(today)) {
            return PhaseState.LATE;
        }
        if (!s.start().isAfter(today)) {
            return PhaseState.IN_PROGRESS;
        }
        return PhaseState.UPCOMING;
    }
}
