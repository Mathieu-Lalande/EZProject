package com.ezproject.project;

import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.AccessContext;
import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccessService;
import com.ezproject.access.ProjectRole;
import com.ezproject.access.Visibility;
import com.ezproject.audit.AuditService;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.organization.Organization;
import com.ezproject.security.CurrentUser;
import com.ezproject.team.Team;
import com.ezproject.team.TeamService;
import com.ezproject.user.AppUser;
import com.ezproject.user.UserService;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private static final String DEFAULT_COLOR = "#6366f1";

    private final ProjectRepository repository;
    private final ProjectStatusChangeRepository history;
    private final ProjectAccessService access;
    private final UserService userService;
    private final TeamService teamService;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public ProjectService(ProjectRepository repository, ProjectStatusChangeRepository history,
                          ProjectAccessService access, UserService userService, TeamService teamService,
                          CurrentUser currentUser, AuditService audit) {
        this.audit = audit;
        this.repository = repository;
        this.history = history;
        this.access = access;
        this.userService = userService;
        this.teamService = teamService;
        this.currentUser = currentUser;
    }

    /** Projets visibles par l'utilisateur courant dans son organisation. */
    public List<ProjectDto> findAll() {
        AccessContext ctx = access.context();
        return repository.findByOrganizationIdOrderByNameAsc(ctx.user().getOrganization().getId()).stream()
                .flatMap(p -> ctx.accessTo(p).map(a -> ProjectDto.from(p, a)).stream())
                .toList();
    }

    public ProjectDto get(Long id) {
        Project p = access.requireVisible(id);
        return ProjectDto.from(p, access.accessTo(p));
    }

    public List<StatusChangeDto> history(Long id) {
        access.requireVisible(id);
        return history.findByProjectIdOrderByChangedAtDesc(id).stream().map(StatusChangeDto::from).toList();
    }

    /**
     * Création : managers et administrateurs sur toute équipe ; un membre uniquement s'il est responsable
     * (LEAD) de l'équipe choisie. Les invités ne créent jamais de projet.
     */
    @Transactional
    public ProjectDto create(ProjectRequest request) {
        AppUser me = currentUser.get();
        Organization org = me.getOrganization();
        Team team = request.teamId() == null ? null : teamService.getInOrganization(request.teamId(), org.getId());
        boolean allowed = me.getRole().seesWholePortfolio()
                || (team != null && teamService.roleIn(team.getId(), me.getId()) == ProjectRole.LEAD);
        if (!allowed) {
            throw new AccessDeniedException("Création réservée aux managers et aux responsables d'équipe");
        }
        if (!org.getPlan().allowsMoreProjects(repository.countByOrganizationId(org.getId()))) {
            throw new BusinessRuleException("Limite de projets atteinte pour l'offre " + org.getPlan()
                    + " : passez à une offre supérieure");
        }
        if (repository.existsByOrganizationIdAndCodeIgnoreCase(org.getId(), request.code())) {
            throw new BusinessRuleException("Le code projet " + request.code() + " est déjà utilisé");
        }
        Project project = new Project(org, team, request.code(), request.name(), request.type());
        project.assign(team, Objects.requireNonNullElse(request.visibility(), Visibility.ORGANIZATION));
        applyDetails(project, request, org);
        repository.save(project);
        record(project.changeStatus(Objects.requireNonNullElse(request.status(), ProjectStatus.PLANNED),
                request.statusReason(), me));
        if (project.getStatus() == ProjectStatus.PLANNED) {
            history.save(new ProjectStatusChange(project, null, ProjectStatus.PLANNED, "Création du projet", me));
        }
        audit.record("PROJECT_CREATED", "PROJECT", project.getId(), "Création de " + project.getCode() + " — " + project.getName());
        return ProjectDto.from(project, access.accessTo(project));
    }

    @Transactional
    public ProjectDto update(Long id, ProjectRequest request) {
        Project project = access.require(id, Permission.PROJECT_EDIT);
        AppUser me = currentUser.get();
        if (!project.getCode().equalsIgnoreCase(request.code())) {
            throw new BusinessRuleException("Le code projet ne peut pas être modifié");
        }
        Long newTeamId = request.teamId();
        Long currentTeamId = project.getTeam() == null ? null : project.getTeam().getId();
        Visibility newVisibility = Objects.requireNonNullElse(request.visibility(), project.getVisibility());
        if (!Objects.equals(newTeamId, currentTeamId) || newVisibility != project.getVisibility()) {
            // Changer d'équipe ou de visibilité modifie qui voit le projet : permission de gestion des accès
            access.require(project, Permission.PROJECT_MEMBERS);
            Team team = newTeamId == null ? null : teamService.getInOrganization(newTeamId, me.getOrganization().getId());
            project.assign(team, newVisibility);
        }
        applyDetails(project, request, me.getOrganization());
        if (request.status() != null) {
            record(project.changeStatus(request.status(), request.statusReason(), me));
        }
        audit.record("PROJECT_UPDATED", "PROJECT", project.getId(), "Modification de " + project.getCode());
        return ProjectDto.from(project, access.accessTo(project));
    }

    @Transactional
    public ProjectDto changeStatus(Long id, StatusRequest request) {
        Project project = access.require(id, Permission.PROJECT_EDIT);
        record(project.changeStatus(request.status(), request.reason(), currentUser.get()));
        audit.record("PROJECT_STATUS", "PROJECT", project.getId(), project.getCode() + " → " + request.status()
                + (request.reason() == null || request.reason().isBlank() ? "" : " : " + request.reason()));
        return ProjectDto.from(project, access.accessTo(project));
    }

    @Transactional
    public ProjectDto changePriority(Long id, ProjectPriority priority) {
        Project project = access.require(id, Permission.PROJECT_EDIT);
        project.prioritize(priority);
        audit.record("PROJECT_PRIORITY", "PROJECT", project.getId(), project.getCode() + " → " + priority);
        return ProjectDto.from(project, access.accessTo(project));
    }

    @Transactional
    public ProjectDto updateModules(Long id, ModulesRequest request) {
        Project project = access.require(id, Permission.PROJECT_MODULES);
        project.replaceModules(request.modules());
        audit.record("PROJECT_MODULES", "PROJECT", project.getId(), project.getCode() + " modules : " + request.modules());
        return ProjectDto.from(project, access.accessTo(project));
    }

    @Transactional
    public void delete(Long id) {
        Project project = access.require(id, Permission.PROJECT_DELETE);
        audit.record("PROJECT_DELETED", "PROJECT", id, "Suppression de " + project.getCode() + " — " + project.getName());
        repository.delete(project);
    }

    private void record(ProjectStatusChange change) {
        if (change != null) {
            history.save(change);
        }
    }

    private void applyDetails(Project project, ProjectRequest r, Organization org) {
        AppUser owner = r.ownerId() == null ? null : userService.getInOrganization(r.ownerId(), org.getId());
        project.update(r.name(), r.description(), r.client(), r.type(),
                Objects.requireNonNullElse(r.priority(), project.getPriority()),
                Objects.requireNonNullElse(r.health(), project.getHealth()),
                Objects.requireNonNullElse(r.color(), DEFAULT_COLOR),
                owner, r.startDate(), r.endDate(), r.budgetDays());
    }
}
