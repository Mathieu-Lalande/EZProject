package com.ezproject.risk;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.audit.AuditService;
import com.ezproject.common.NotFoundException;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.risk.RiskDtos.DecisionDto;
import com.ezproject.risk.RiskDtos.DecisionRequest;
import com.ezproject.risk.RiskDtos.RiskDto;
import com.ezproject.risk.RiskDtos.RiskRequest;
import com.ezproject.user.AppUser;
import com.ezproject.user.UserService;

/** Registre des risques et des décisions d'un projet. */
@Service
@Transactional(readOnly = true)
public class RiskService {

    private final RiskRepository risks;
    private final DecisionRepository decisions;
    private final ProjectAccessService access;
    private final UserService userService;
    private final AuditService audit;

    public RiskService(RiskRepository risks, DecisionRepository decisions, ProjectAccessService access,
                       UserService userService, AuditService audit) {
        this.risks = risks;
        this.decisions = decisions;
        this.access = access;
        this.userService = userService;
        this.audit = audit;
    }

    /** Risques triés : actifs d'abord, par criticité décroissante. */
    public List<RiskDto> risks(Long projectId) {
        access.require(projectId, Permission.RISK_VIEW);
        return risks.findByProjectId(projectId).stream()
                .sorted(Comparator.comparing((Risk r) -> !r.getStatus().isActive())
                        .thenComparing(Comparator.comparingInt(Risk::score).reversed()))
                .map(RiskDto::from).toList();
    }

    /** Nombre de risques critiques actifs par projet (vue portefeuille). */
    public Map<Long, Long> criticalByProject(List<Long> projectIds) {
        Map<Long, Long> result = new HashMap<>();
        if (!projectIds.isEmpty()) {
            risks.findByProjectIdIn(projectIds).stream().filter(Risk::isCritical)
                    .forEach(r -> result.merge(r.getProject().getId(), 1L, Long::sum));
        }
        return result;
    }

    @Transactional
    public RiskDto createRisk(Long projectId, RiskRequest request) {
        Project project = access.require(projectId, Permission.RISK_EDIT, ModuleType.RISKS);
        Risk risk = new Risk(project);
        apply(risk, request, project);
        risks.save(risk);
        audit.record("RISK_CREATED", "RISK", risk.getId(), project.getCode() + " : " + risk.getTitle()
                + " (criticité " + risk.score() + ")");
        return RiskDto.from(risk);
    }

    @Transactional
    public RiskDto updateRisk(Long riskId, RiskRequest request) {
        Risk risk = risks.findById(riskId).orElseThrow(() -> new NotFoundException("Risque", riskId));
        access.require(risk.getProject(), Permission.RISK_EDIT);
        RiskStatus before = risk.getStatus();
        apply(risk, request, risk.getProject());
        if (before != risk.getStatus()) {
            audit.record("RISK_STATUS", "RISK", riskId, risk.getProject().getCode() + " : " + risk.getTitle() + " → "
                    + risk.getStatus());
        }
        return RiskDto.from(risk);
    }

    @Transactional
    public void deleteRisk(Long riskId) {
        Risk risk = risks.findById(riskId).orElseThrow(() -> new NotFoundException("Risque", riskId));
        access.require(risk.getProject(), Permission.RISK_EDIT);
        audit.record("RISK_DELETED", "RISK", riskId, risk.getProject().getCode() + " : " + risk.getTitle());
        risks.delete(risk);
    }

    public List<DecisionDto> decisions(Long projectId) {
        access.require(projectId, Permission.RISK_VIEW);
        return decisions.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(DecisionDto::from).toList();
    }

    @Transactional
    public DecisionDto createDecision(Long projectId, DecisionRequest request) {
        Project project = access.require(projectId, Permission.RISK_EDIT, ModuleType.RISKS);
        Decision decision = new Decision(project, access.context().user());
        decision.update(request.title().trim(), request.description(), request.decidedOn(), request.decidedBy(),
                Objects.requireNonNullElse(request.status(), DecisionStatus.PROPOSED));
        decisions.save(decision);
        audit.record("DECISION_CREATED", "RISK", decision.getId(), project.getCode() + " : " + decision.getTitle()
                + " (" + decision.getStatus() + ")");
        return DecisionDto.from(decision);
    }

    @Transactional
    public DecisionDto updateDecision(Long decisionId, DecisionRequest request) {
        Decision decision = decisions.findById(decisionId).orElseThrow(() -> new NotFoundException("Décision", decisionId));
        access.require(decision.getProject(), Permission.RISK_EDIT);
        decision.update(request.title().trim(), request.description(), request.decidedOn(), request.decidedBy(),
                Objects.requireNonNullElse(request.status(), decision.getStatus()));
        audit.record("DECISION_UPDATED", "RISK", decisionId, decision.getProject().getCode() + " : "
                + decision.getTitle() + " (" + decision.getStatus() + ")");
        return DecisionDto.from(decision);
    }

    @Transactional
    public void deleteDecision(Long decisionId) {
        Decision decision = decisions.findById(decisionId).orElseThrow(() -> new NotFoundException("Décision", decisionId));
        access.require(decision.getProject(), Permission.RISK_EDIT);
        decisions.delete(decision);
    }

    private void apply(Risk risk, RiskRequest r, Project project) {
        AppUser owner = r.ownerId() == null ? null
                : userService.getInOrganization(r.ownerId(), project.getOrganization().getId());
        risk.update(r.title().trim(), r.description(), r.probability(), r.impact(),
                Objects.requireNonNullElse(r.status(), risk.getStatus()), owner, r.mitigation(), r.dueDate());
    }
}
