package com.ezproject.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.ezproject.access.AccessRules.ProjectFacts;
import com.ezproject.access.AccessRules.Resolution;
import com.ezproject.access.AccessRules.UserFacts;
import com.ezproject.user.Role;

class AccessRulesTest {

    private static final long ORG = 1L;
    private static final long TEAM = 10L;
    private static final long PROJECT = 100L;

    private static ProjectFacts project(Visibility v) {
        return new ProjectFacts(PROJECT, ORG, TEAM, v);
    }

    private static UserFacts user(Role role, ProjectRole teamRole, ProjectRole explicitRole) {
        return new UserFacts(ORG, role,
                teamRole == null ? Map.of() : Map.of(TEAM, teamRole),
                explicitRole == null ? Map.of() : Map.of(PROJECT, explicitRole));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void noAccessAcrossOrganizations(Role role) {
        UserFacts other = new UserFacts(2L, role, Map.of(TEAM, ProjectRole.LEAD), Map.of(PROJECT, ProjectRole.LEAD));
        assertThat(AccessRules.resolve(other, project(Visibility.ORGANIZATION))).isEmpty();
    }

    @Test
    void adminsHaveFullAccessEvenOnRestrictedProjects() {
        assertThat(AccessRules.resolve(user(Role.ADMIN, null, null), project(Visibility.RESTRICTED)))
                .contains(new Resolution(ProjectRole.LEAD, true));
    }

    @Test
    void managersLeadEveryProject() {
        assertThat(AccessRules.resolve(user(Role.MANAGER, null, null), project(Visibility.RESTRICTED)))
                .contains(new Resolution(ProjectRole.LEAD, false));
    }

    @Test
    void memberGetsTeamRoleOnTeamProject() {
        assertThat(AccessRules.resolve(user(Role.MEMBER, ProjectRole.CONTRIBUTOR, null), project(Visibility.TEAM)))
                .contains(new Resolution(ProjectRole.CONTRIBUTOR, false));
    }

    @Test
    void memberOutsideTeamCannotSeeTeamProjectButReadsOrganizationProject() {
        assertThat(AccessRules.resolve(user(Role.MEMBER, null, null), project(Visibility.TEAM))).isEmpty();
        assertThat(AccessRules.resolve(user(Role.MEMBER, null, null), project(Visibility.ORGANIZATION)))
                .contains(new Resolution(ProjectRole.VIEWER, false));
    }

    @Test
    void restrictedProjectIgnoresTeamMembership() {
        assertThat(AccessRules.resolve(user(Role.MEMBER, ProjectRole.LEAD, null), project(Visibility.RESTRICTED))).isEmpty();
        assertThat(AccessRules.resolve(user(Role.MEMBER, ProjectRole.LEAD, ProjectRole.VIEWER), project(Visibility.RESTRICTED)))
                .contains(new Resolution(ProjectRole.VIEWER, false));
    }

    @Test
    void highestRoleWinsBetweenTeamAndExplicitMembership() {
        assertThat(AccessRules.resolve(user(Role.MEMBER, ProjectRole.VIEWER, ProjectRole.LEAD), project(Visibility.TEAM)))
                .contains(new Resolution(ProjectRole.LEAD, false));
    }

    @Test
    void guestOnlySeesProjectsWhereExplicitlyAddedAndIsCappedToContributor() {
        assertThat(AccessRules.resolve(user(Role.GUEST, null, null), project(Visibility.ORGANIZATION))).isEmpty();
        assertThat(AccessRules.resolve(user(Role.GUEST, null, ProjectRole.LEAD), project(Visibility.TEAM)))
                .contains(new Resolution(ProjectRole.CONTRIBUTOR, false));
    }
}
