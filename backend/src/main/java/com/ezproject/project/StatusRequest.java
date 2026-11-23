package com.ezproject.project;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusRequest(@NotNull ProjectStatus status, @Size(max = 500) String reason) {
}
