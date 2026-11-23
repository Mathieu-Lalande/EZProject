package com.ezproject.security;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ezproject.team.TeamMembershipRepository;
import com.ezproject.user.AppUser;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUser currentUser;
    private final TeamMembershipRepository memberships;

    public AuthController(CurrentUser currentUser, TeamMembershipRepository memberships) {
        this.currentUser = currentUser;
        this.memberships = memberships;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public MeDto me() {
        AppUser u = currentUser.get();
        var org = u.getOrganization();
        return new MeDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getJobTitle(),
                new MeDto.Org(org.getId(), org.getName(), org.getSlug(), org.getPlan()),
                memberships.findByUserId(u.getId()).stream()
                        .map(m -> new MeDto.TeamRole(m.getTeam().getId(), m.getTeam().getName(), m.getRole()))
                        .toList(),
                u.isMustChangePassword());
    }
}
