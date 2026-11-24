package com.ezproject.security;

import java.util.List;

import com.ezproject.access.ProjectRole;
import com.ezproject.organization.Plan;
import com.ezproject.user.Role;

/** Profil de l'utilisateur connecté et de son organisation. */
public record MeDto(Long id, String email, String displayName, Role role, String jobTitle, Org organization,
                    List<TeamRole> teams, boolean mustChangePassword, boolean mfaEnabled) {

    public record Org(Long id, String name, String slug, Plan plan) {
    }

    public record TeamRole(Long teamId, String teamName, ProjectRole role) {
    }
}
