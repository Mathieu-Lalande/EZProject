package com.ezproject.access;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.audit.AuditService;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.project.Project;
import com.ezproject.team.TeamMembershipRepository;
import com.ezproject.user.AppUser;
import com.ezproject.user.Role;
import com.ezproject.user.UserService;

@Service
@Transactional(readOnly = true)
public class ProjectMembersService {

    private final ProjectAccessService access;
    private final ProjectMemberRepository projectMembers;
    private final TeamMembershipRepository teamMemberships;
    private final UserService userService;
    private final AuditService audit;

    public ProjectMembersService(ProjectAccessService access, ProjectMemberRepository projectMembers,
                                 TeamMembershipRepository teamMemberships, UserService userService, AuditService audit) {
        this.audit = audit;
        this.access = access;
        this.projectMembers = projectMembers;
        this.teamMemberships = teamMemberships;
        this.userService = userService;
    }

    public ProjectMembersDto list(Long projectId) {
        return toDto(access.requireVisible(projectId));
    }

    @Transactional
    public ProjectMembersDto put(Long projectId, Long userId, ProjectRole role) {
        Project project = access.require(projectId, Permission.PROJECT_MEMBERS);
        AppUser user = userService.getInOrganization(userId, project.getOrganization().getId());
        if (user.getRole() == Role.GUEST && role == ProjectRole.LEAD) {
            throw new BusinessRuleException("Un invité ne peut pas être responsable d'un projet");
        }
        if (!user.isActive()) {
            throw new BusinessRuleException("Ce compte est désactivé");
        }
        projectMembers.findByProjectIdAndUserId(projectId, userId)
                .ifPresentOrElse(m -> m.changeRole(role), () -> projectMembers.save(new ProjectMember(project, user, role)));
        projectMembers.flush();
        audit.record("PROJECT_MEMBER", "PROJECT", projectId, user.getDisplayName() + " → " + role + " sur " + project.getCode());
        return toDto(project);
    }

    @Transactional
    public ProjectMembersDto remove(Long projectId, Long userId) {
        Project project = access.require(projectId, Permission.PROJECT_MEMBERS);
        projectMembers.findByProjectIdAndUserId(projectId, userId).ifPresent(m -> {
            audit.record("PROJECT_MEMBER_REMOVED", "PROJECT", projectId, m.getUser().getDisplayName() + " retiré de " + project.getCode());
            projectMembers.delete(m);
        });
        projectMembers.flush();
        return toDto(project);
    }

    private ProjectMembersDto toDto(Project project) {
        List<ProjectMembersDto.Member> team = project.getTeam() == null ? List.of()
                : teamMemberships.findByTeamIdIn(List.of(project.getTeam().getId())).stream()
                        .map(m -> member(m.getUser(), m.getRole()))
                        .sorted(ORDER).toList();
        List<ProjectMembersDto.Member> explicit = projectMembers.findByProjectId(project.getId()).stream()
                .map(m -> member(m.getUser(), m.getRole()))
                .sorted(ORDER).toList();
        return new ProjectMembersDto(project.getTeam() == null ? null : project.getTeam().getName(),
                project.getVisibility(), team, explicit);
    }

    private static final Comparator<ProjectMembersDto.Member> ORDER =
            Comparator.comparing(ProjectMembersDto.Member::role).reversed()
                    .thenComparing(ProjectMembersDto.Member::displayName);

    private static ProjectMembersDto.Member member(AppUser u, ProjectRole role) {
        return new ProjectMembersDto.Member(u.getId(), u.getDisplayName(), u.getEmail(), u.getRole(), role);
    }
}
