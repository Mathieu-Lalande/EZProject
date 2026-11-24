package com.ezproject.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        boolean overdue,
        List<String> labels,
        Counts counts) {

    /** Compteurs affichés sur la carte Kanban. */
    public record Counts(long comments, long attachments, long checklistDone, long checklistTotal) {
        public static final Counts NONE = new Counts(0, 0, 0, 0);
    }

    public static TaskDto from(Task t, LocalDate today) {
        return from(t, today, Counts.NONE);
    }

    public static TaskDto from(Task t, LocalDate today, Counts counts) {
        var p = t.getProject();
        return new TaskDto(t.getId(), p.getId(), p.getCode(), p.getName(), p.getColor(), t.getTitle(),
                t.getDescription(), t.getStatus(), t.getPriority(),
                t.getAssignee() == null ? null : UserDto.from(t.getAssignee()),
                t.getStoryPoints(), t.getDueDate(), t.getPosition(), t.getCompletedAt(), t.isOverdue(today),
                t.getLabels().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), counts);
    }
}
