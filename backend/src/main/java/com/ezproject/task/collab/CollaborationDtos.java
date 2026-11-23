package com.ezproject.task.collab;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CollaborationDtos {

    private CollaborationDtos() {
    }

    public record CommentRequest(@NotBlank @Size(max = 4000) String body) {
    }

    public record ChecklistRequest(@NotBlank @Size(max = 300) String title, boolean done) {
    }

    public record CommentDto(Long id, String body, Long authorId, String authorName, Instant createdAt,
                             Instant editedAt, boolean editable) {
    }

    public record ChecklistDto(Long id, String title, boolean done) {
    }

    public record AttachmentDto(Long id, String filename, String contentType, long sizeBytes, String uploaderName,
                                Instant createdAt, boolean deletable) {
    }

    /** Détail collaboratif d'une tâche (chargé à l'ouverture de la tâche). */
    public record TaskDetailsDto(List<CommentDto> comments, List<ChecklistDto> checklist,
                                 List<AttachmentDto> attachments, boolean canComment, boolean canEdit) {
    }
}
