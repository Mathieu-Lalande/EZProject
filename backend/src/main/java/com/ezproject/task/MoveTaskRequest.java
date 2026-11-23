package com.ezproject.task;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveTaskRequest(@NotNull TaskStatus status, @Min(0) int position) {
}
