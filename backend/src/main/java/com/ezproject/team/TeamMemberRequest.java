package com.ezproject.team;

import com.ezproject.access.ProjectRole;

import jakarta.validation.constraints.NotNull;

public record TeamMemberRequest(@NotNull ProjectRole role) {
}
