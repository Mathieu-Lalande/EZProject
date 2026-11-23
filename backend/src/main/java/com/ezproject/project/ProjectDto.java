package com.ezproject.project;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import com.ezproject.access.Permission;
import com.ezproject.access.ProjectAccess;
import com.ezproject.access.ProjectRole;
import com.ezproject.access.Visibility;
import com.ezproject.user.UserDto;

/**
 * Projet tel que vu par l'utilisateur courant : inclut son rôle et ses permissions,
 * pour que l'interface n'affiche que les actions autorisées (le backend reste l'autorité).
 */
public record ProjectDto(
        Long id,
        String code,
        String name,
        String description,
        String client,
        ProjectType type,
        ProjectStatus status,
        String statusReason,
        ProjectPriority priority,
        Health health,
        String color,
        UserDto owner,
        TeamRef team,
        Visibility visibility,
        LocalDate startDate,
        LocalDate endDate,
        Integer budgetDays,
        Set<ModuleType> modules,
        ProjectRole myRole,
        Set<Permission> myPermissions) {

    public record TeamRef(Long id, String name, String color) {
    }

    public static ProjectDto from(Project p, ProjectAccess access) {
        var team = p.getTeam();
        return new ProjectDto(p.getId(), p.getCode(), p.getName(), p.getDescription(), p.getClient(), p.getType(),
                p.getStatus(), p.getStatusReason(), p.getPriority(), p.getHealth(), p.getColor(),
                p.getOwner() == null ? null : UserDto.from(p.getOwner()),
                team == null ? null : new TeamRef(team.getId(), team.getName(), team.getColor()),
                p.getVisibility(), p.getStartDate(), p.getEndDate(), p.getBudgetDays(),
                new TreeSet<>(p.getModules()), access.admin() ? null : access.role(),
                new TreeSet<>(access.permissions()));
    }
}
