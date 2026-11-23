package com.ezproject.organization;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.ProjectRole;
import com.ezproject.audit.AuditService;
import com.ezproject.project.ProjectRepository;
import com.ezproject.security.CurrentUser;
import com.ezproject.team.Team;
import com.ezproject.team.TeamMembership;
import com.ezproject.team.TeamMembershipRepository;
import com.ezproject.team.TeamRepository;
import com.ezproject.user.AppUser;
import com.ezproject.user.Role;
import com.ezproject.user.UserService;

@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository repository;
    private final UserService userService;
    private final TeamRepository teams;
    private final TeamMembershipRepository memberships;
    private final ProjectRepository projects;
    private final CurrentUser currentUser;
    private final AuditService audit;

    public OrganizationService(OrganizationRepository repository, UserService userService, TeamRepository teams,
                               TeamMembershipRepository memberships, ProjectRepository projects,
                               CurrentUser currentUser, AuditService audit) {
        this.audit = audit;
        this.repository = repository;
        this.userService = userService;
        this.teams = teams;
        this.memberships = memberships;
        this.projects = projects;
        this.currentUser = currentUser;
    }

    /**
     * Inscription self-service : crée l'organisation (offre FREE), son propriétaire et une première équipe.
     */
    @Transactional
    public Organization signup(SignupRequest request) {
        Organization org = repository.save(new Organization(request.organizationName().trim(),
                uniqueSlug(request.organizationName()), Plan.FREE));
        AppUser owner = userService.register(org, request.email(), request.displayName(), request.password(),
                Role.OWNER, null);
        Team team = teams.save(new Team(org, "Équipe principale", null, "#6366f1"));
        memberships.save(new TeamMembership(team, owner, ProjectRole.LEAD));
        audit.record(org, owner, "ORGANIZATION_CREATED", "ORGANIZATION", org.getId(), "Création de l'organisation " + org.getName());
        return org;
    }

    public OrganizationDto current() {
        return toDto(currentUser.organization());
    }

    @Transactional
    public OrganizationDto rename(String name) {
        AppUser me = currentUser.get();
        if (!me.getRole().isAdmin()) {
            throw new AccessDeniedException("Réservé aux administrateurs");
        }
        audit.record("ORGANIZATION_RENAMED", "ORGANIZATION", me.getOrganization().getId(),
                me.getOrganization().getName() + " → " + name.trim());
        me.getOrganization().rename(name.trim());
        return toDto(me.getOrganization());
    }

    /** Changement d'offre (sans facturation dans cette version : réservé au propriétaire). */
    @Transactional
    public OrganizationDto changePlan(Plan plan) {
        AppUser me = currentUser.get();
        if (me.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Réservé au propriétaire de l'organisation");
        }
        audit.record("PLAN_CHANGED", "ORGANIZATION", me.getOrganization().getId(),
                me.getOrganization().getPlan() + " → " + plan);
        me.getOrganization().changePlan(plan);
        return toDto(me.getOrganization());
    }

    private OrganizationDto toDto(Organization org) {
        Plan plan = org.getPlan();
        return new OrganizationDto(org.getId(), org.getName(), org.getSlug(), plan,
                new OrganizationDto.Usage(projects.countByOrganizationId(org.getId()), plan.maxProjects(),
                        userService.countInOrganization(org.getId()), plan.maxUsers(), plan.allowsCustomPermissions()));
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "organisation";
        }
        base = base.length() > 50 ? base.substring(0, 50) : base;
        String slug = base;
        int i = 2;
        while (repository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
