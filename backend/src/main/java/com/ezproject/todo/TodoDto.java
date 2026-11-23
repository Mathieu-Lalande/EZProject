package com.ezproject.todo;

import java.time.LocalDate;

public record TodoDto(Long id, String title, boolean done, LocalDate dueDate, Long projectId, String ownerName) {

    public static TodoDto from(TodoItem t) {
        return new TodoDto(t.getId(), t.getTitle(), t.isDone(), t.getDueDate(),
                t.getProject() == null ? null : t.getProject().getId(), t.getOwner().getDisplayName());
    }
}
