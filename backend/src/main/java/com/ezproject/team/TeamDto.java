package com.ezproject.team;

import java.util.List;

import com.ezproject.access.ProjectRole;

public record TeamDto(Long id, String name, String description, String color, List<Member> members,
                      boolean canManage) {

    public record Member(Long userId, String displayName, String email, String jobTitle, ProjectRole role) {
    }
}
