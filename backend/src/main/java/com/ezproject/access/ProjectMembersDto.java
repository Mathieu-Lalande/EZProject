package com.ezproject.access;

import java.util.List;

import com.ezproject.user.Role;

/** Qui a accès au projet : membres hérités de l'équipe et membres ajoutés explicitement. */
public record ProjectMembersDto(String teamName, Visibility visibility, List<Member> teamMembers,
                                List<Member> explicitMembers) {

    public record Member(Long userId, String displayName, String email, Role orgRole, ProjectRole role) {
    }
}
