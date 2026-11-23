package com.ezproject.access;

import java.util.Map;
import java.util.Optional;

import com.ezproject.user.Role;

/**
 * Règles de résolution du rôle effectif d'un utilisateur sur un projet. Fonction pure, sans accès base :
 * toute la politique d'autorisation est lisible (et testée) ici.
 */
public final class AccessRules {

    /** Données minimales d'un projet nécessaires à la décision. */
    public record ProjectFacts(Long id, Long organizationId, Long teamId, Visibility visibility) {
    }

    /** Données minimales de l'utilisateur : rôle d'organisation et rattachements. */
    public record UserFacts(Long organizationId, Role role, Map<Long, ProjectRole> teamRoles,
                            Map<Long, ProjectRole> projectRoles) {
    }

    /** Résultat : rôle projet effectif, ou accès administrateur (toutes permissions). */
    public record Resolution(ProjectRole role, boolean admin) {
    }

    private AccessRules() {
    }

    public static Optional<Resolution> resolve(UserFacts user, ProjectFacts project) {
        // Isolation stricte entre locataires
        if (!user.organizationId().equals(project.organizationId())) {
            return Optional.empty();
        }
        if (user.role().isAdmin()) {
            return Optional.of(new Resolution(ProjectRole.LEAD, true));
        }
        if (user.role() == Role.MANAGER) {
            return Optional.of(new Resolution(ProjectRole.LEAD, false));
        }

        ProjectRole explicit = user.projectRoles().get(project.id());
        ProjectRole fromTeam = project.teamId() == null ? null : user.teamRoles().get(project.teamId());

        ProjectRole role = switch (user.role()) {
            // Un invité n'accède qu'aux projets où il est explicitement ajouté, au plus en contributeur
            case GUEST -> explicit == ProjectRole.LEAD ? ProjectRole.CONTRIBUTOR : explicit;
            default -> switch (project.visibility()) {
                case RESTRICTED -> explicit;
                case TEAM -> ProjectRole.max(explicit, fromTeam);
                case ORGANIZATION -> ProjectRole.max(ProjectRole.max(explicit, fromTeam), ProjectRole.VIEWER);
            };
        };
        return Optional.ofNullable(role).map(r -> new Resolution(r, false));
    }
}
