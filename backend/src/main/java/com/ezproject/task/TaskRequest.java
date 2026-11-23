package com.ezproject.task;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        TaskStatus status,
        Priority priority,
        Long assigneeId,
        @Min(0) @Max(1000) Integer storyPoints,
        LocalDate dueDate) {
}
