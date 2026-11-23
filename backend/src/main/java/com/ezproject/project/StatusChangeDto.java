package com.ezproject.project;

import java.time.Instant;

public record StatusChangeDto(Long id, ProjectStatus fromStatus, ProjectStatus toStatus, String reason,
                              String changedBy, Instant changedAt) {

    public static StatusChangeDto from(ProjectStatusChange c) {
        return new StatusChangeDto(c.getId(), c.getFromStatus(), c.getToStatus(), c.getReason(),
                c.getChangedBy() == null ? null : c.getChangedBy().getDisplayName(), c.getChangedAt());
    }
}
