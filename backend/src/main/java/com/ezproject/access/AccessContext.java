package com.ezproject.access;

import java.util.EnumSet;
import java.util.Optional;

import com.ezproject.project.Project;
import com.ezproject.user.AppUser;

/**
 * Contexte d'autorisation de l'utilisateur courant, chargé une fois par requête :
 * rattachements (équipes, projets) et matrice de permissions de son organisation.
 */
public final class AccessContext {

    private final AppUser user;
    private final AccessRules.UserFacts facts;
    private final PermissionMatrix matrix;

    AccessContext(AppUser user, AccessRules.UserFacts facts, PermissionMatrix matrix) {
        this.user = user;
        this.facts = facts;
        this.matrix = matrix;
    }

    public AppUser user() {
        return user;
    }

    public Optional<ProjectAccess> accessTo(Project project) {
        AccessRules.ProjectFacts pf = new AccessRules.ProjectFacts(project.getId(), project.getOrganization().getId(),
                project.getTeam() == null ? null : project.getTeam().getId(), project.getVisibility());
        return AccessRules.resolve(facts, pf).map(r -> new ProjectAccess(r.role(), r.admin(),
                r.admin() ? EnumSet.allOf(Permission.class) : EnumSet.copyOf(withNone(r.role()))));
    }

    public boolean canSee(Project project) {
        return accessTo(project).isPresent();
    }

    public boolean can(Project project, Permission permission) {
        return accessTo(project).map(a -> a.can(permission)).orElse(false);
    }

    private java.util.Collection<Permission> withNone(ProjectRole role) {
        var perms = matrix.roles().get(role);
        return perms == null || perms.isEmpty() ? EnumSet.noneOf(Permission.class) : perms;
    }
}
