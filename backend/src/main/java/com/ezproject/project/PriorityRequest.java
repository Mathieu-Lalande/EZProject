package com.ezproject.project;

import jakarta.validation.constraints.NotNull;

public record PriorityRequest(@NotNull ProjectPriority priority) {
}
