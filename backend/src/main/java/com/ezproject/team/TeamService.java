package com.ezproject.team;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.access.ProjectRole;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.common.NotFoundException;
import com.ezproject.security.CurrentUser;
import com.ezproject.user.AppUser;
import com.ezproject.user.Role;
import com.ezproject.user.UserService;

@Service
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teams;
    private final TeamMembershipRepository memberships;
    private final UserService userService;
    private final CurrentUser currentUser;

    public TeamService(TeamRepository teams, TeamMembershipRepository memberships, UserService userService,
                       CurrentUser currentUser) {
        this.teams = teams;
        this.memberships = memberships;
        this.userService = userService;
        this.currentUser = currentUser;
    }

    public List<TeamDto> findAll() {
        AppUser me = currentUser.get();
        List<Team> list = teams.findByOrganizationIdOrderByNameAsc(me.getOrganization().getId());
        Map<Long, List<TeamMembership>> byTeam = memberships.findByTeamIdIn(list.stream().map(Team::getId).toList())
                .stream().collect(Collectors.groupingBy(m -> m.getTeam().getId()));
        return list.stream().map(t -> toDto(t, byTeam.getOrDefault(t.getId(), List.of()), me)).toList();
    }

    /** Équipe de l'organisation courante (404 si elle appartient à un autre locataire). */
    public Team getInOrganization(Long teamId, Long organizationId) {
        return teams.findByIdAndOrganizationId(teamId, organizationId)
                .orElseThrow(() -> new NotFoundException("Équipe", teamId));
    }

    /** Rôle de l'utilisateur dans l'équipe, s'il en est membre. */
    public ProjectRole roleIn(Long teamId, Long userId) {
        return memberships.findByTeamIdAndUserId(teamId, userId).map(TeamMembership::getRole).orElse(null);
    }

    @Transactional
    public TeamDto create(TeamRequest request) {
        AppUser me = currentUser.get();
        requireOrgManager(me);
        Long orgId = me.getOrganization().getId();
        if (teams.existsByOrganizationIdAndNameIgnoreCase(orgId, request.name())) {
            throw new BusinessRuleException("Une équipe porte déjà ce nom");
        }
        Team team = teams.save(new Team(me.getOrganization(), request.name(), request.description(), color(request)));
        return toDto(team, List.of(), me);
    }

    @Transactional
    public TeamDto update(Long teamId, TeamRequest request) {
        AppUser me = currentUser.get();
        Team team = getInOrganization(teamId, me.getOrganization().getId());
        requireCanManage(me, team);
        teams.findByOrganizationIdAndNameIgnoreCase(me.getOrganization().getId(), request.name())
                .filter(other -> !other.getId().equals(teamId))
                .ifPresent(other -> { throw new BusinessRuleException("Une équipe porte déjà ce nom"); });
        team.update(request.name(), request.description(), color(request));
        return reload(team, me);
    }

    @Transactional
    public void delete(Long teamId) {
        AppUser me = currentUser.get();
        requireOrgManager(me);
        teams.delete(getInOrganization(teamId, me.getOrganization().getId()));
    }

    @Transactional
    public TeamDto putMember(Long teamId, Long userId, ProjectRole role) {
        AppUser me = currentUser.get();
        Team team = getInOrganization(teamId, me.getOrganization().getId());
        requireCanManage(me, team);
        AppUser user = userService.getInOrganization(userId, me.getOrganization().getId());
        if (user.getRole() == Role.GUEST) {
            throw new BusinessRuleException("Un invité ne peut pas rejoindre une équipe : ajoutez-le directement aux projets");
        }
        memberships.findByTeamIdAndUserId(teamId, userId)
                .ifPresentOrElse(m -> m.changeRole(role), () -> memberships.save(new TeamMembership(team, user, role)));
        return reload(team, me);
    }

    @Transactional
    public TeamDto removeMember(Long teamId, Long userId) {
        AppUser me = currentUser.get();
        Team team = getInOrganization(teamId, me.getOrganization().getId());
        requireCanManage(me, team);
        memberships.findByTeamIdAndUserId(teamId, userId).ifPresent(memberships::delete);
        memberships.flush();
        return reload(team, me);
    }

    private TeamDto reload(Team team, AppUser me) {
        return toDto(team, memberships.findByTeamIdIn(List.of(team.getId())), me);
    }

    private boolean canManage(AppUser me, Team team) {
        return me.getRole().seesWholePortfolio() || roleIn(team.getId(), me.getId()) == ProjectRole.LEAD;
    }

    private void requireCanManage(AppUser me, Team team) {
        if (!canManage(me, team)) {
            throw new AccessDeniedException("Seuls les managers et le responsable de l'équipe peuvent la modifier");
        }
    }

    private static void requireOrgManager(AppUser me) {
        if (!me.getRole().seesWholePortfolio()) {
            throw new AccessDeniedException("Réservé aux administrateurs et managers");
        }
    }

    private static String color(TeamRequest r) {
        return Objects.requireNonNullElse(r.color(), "#6366f1");
    }

    private TeamDto toDto(Team team, List<TeamMembership> members, AppUser me) {
        List<TeamDto.Member> list = members.stream()
                .sorted(Comparator.comparing((TeamMembership m) -> m.getRole()).reversed()
                        .thenComparing(m -> m.getUser().getDisplayName()))
                .map(m -> new TeamDto.Member(m.getUser().getId(), m.getUser().getDisplayName(), m.getUser().getEmail(),
                        m.getUser().getJobTitle(), m.getRole()))
                .toList();
        boolean manage = me.getRole().seesWholePortfolio()
                || members.stream().anyMatch(m -> m.getUser().getId().equals(me.getId()) && m.getRole() == ProjectRole.LEAD);
        return new TeamDto(team.getId(), team.getName(), team.getDescription(), team.getColor(), list, manage);
    }
}
