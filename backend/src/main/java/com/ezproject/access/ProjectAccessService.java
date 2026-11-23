package com.ezproject.access;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.project.ModuleType;
import com.ezproject.project.Project;
import com.ezproject.project.ProjectRepository;
import com.ezproject.security.CurrentUser;
import com.ezproject.team.TeamMembership;
import com.ezproject.team.TeamMembershipRepository;
import com.ezproject.user.AppUser;

/**
 * Point d'entrée unique des contrôles d'accès aux projets.
 * <ul>
 *   <li>Projet d'un autre locataire ou non visible : 404 (on ne révèle pas son existence).</li>
 *   <li>Projet visible mais permission manquante : 403.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ProjectAccessService {

    private final ProjectRepository projects;
    private final TeamMembershipRepository teamMemberships;
    private final ProjectMemberRepository projectMembers;
    private final PermissionMatrixService matrixService;
    private final CurrentUser currentUser;

    public ProjectAccessService(ProjectRepository projects, TeamMembershipRepository teamMemberships,
                                ProjectMemberRepository projectMembers, PermissionMatrixService matrixService,
                                CurrentUser currentUser) {
        this.projects = projects;
        this.teamMemberships = teamMemberships;
        this.projectMembers = projectMembers;
        this.matrixService = matrixService;
        this.currentUser = currentUser;
    }

    public AccessContext context() {
        AppUser user = currentUser.get();
        Map<Long, ProjectRole> teamRoles = teamMemberships.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(m -> m.getTeam().getId(), TeamMembership::getRole, ProjectRole::max));
        Map<Long, ProjectRole> projectRoles = projectMembers.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(m -> m.getProject().getId(), ProjectMember::getRole, ProjectRole::max));
        AccessRules.UserFacts facts = new AccessRules.UserFacts(user.getOrganization().getId(), user.getRole(),
                teamRoles, projectRoles);
        return new AccessContext(user, facts, matrixService.matrixOf(user.getOrganization()));
    }

    /** Projet visible par l'utilisateur courant, sinon 404. */
    public Project requireVisible(Long projectId) {
        Project project = projects.findById(projectId).orElseThrow(() -> new NotFoundException("Projet", projectId));
        if (!context().canSee(project)) {
            throw new NotFoundException("Projet", projectId);
        }
        return project;
    }

    /** Projet sur lequel l'utilisateur courant dispose de la permission, sinon 404 / 403. */
    public Project require(Long projectId, Permission permission) {
        Project project = projects.findById(projectId).orElseThrow(() -> new NotFoundException("Projet", projectId));
        require(project, permission);
        return project;
    }

    /** Comme {@link #require(Long, Permission)}, en vérifiant aussi que le module est activé sur le projet. */
    public Project require(Long projectId, Permission permission, ModuleType module) {
        Project project = require(projectId, permission);
        if (!project.hasModule(module)) {
            throw new BusinessRuleException("Le module " + module + " n'est pas activé sur ce projet");
        }
        return project;
    }

    public void require(Project project, Permission permission) {
        ProjectAccess access = context().accessTo(project)
                .orElseThrow(() -> new NotFoundException("Projet", project.getId()));
        if (!access.can(permission)) {
            throw new AccessDeniedException("Permission manquante : " + permission);
        }
    }

    public ProjectAccess accessTo(Project project) {
        return context().accessTo(project).orElseThrow(() -> new NotFoundException("Projet", project.getId()));
    }
}
