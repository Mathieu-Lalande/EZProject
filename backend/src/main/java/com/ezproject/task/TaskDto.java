package com.ezproject.task;

import java.time.Instant;
import java.time.LocalDate;

import com.ezproject.user.UserDto;

public record TaskDto(
        Long id,
        Long projectId,
        String projectCode,
        String projectName,
        String projectColor,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        UserDto assignee,
        Integer storyPoints,
        LocalDate dueDate,
        int position,
        Instant completedAt,
        boolean overdue) {

    public static TaskDto from(Task t, LocalDate today) {
        var p = t.getProject();
        return new TaskDto(t.getId(), p.getId(), p.getCode(), p.getName(), p.getColor(), t.getTitle(),
                t.getDescription(), t.getStatus(), t.getPriority(),
                t.getAssignee() == null ? null : UserDto.from(t.getAssignee()),
                t.getStoryPoints(), t.getDueDate(), t.getPosition(), t.getCompletedAt(), t.isOverdue(today));
    }
}
