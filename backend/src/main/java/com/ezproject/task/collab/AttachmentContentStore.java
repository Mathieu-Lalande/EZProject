package com.ezproject.task.collab;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Lecture / écriture du contenu binaire des pièces jointes (hors JPA, pour ne jamais le charger en liste). */
@Component
class AttachmentContentStore {

    private final JdbcTemplate jdbc;

    AttachmentContentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void write(Long attachmentId, byte[] content) {
        jdbc.update("UPDATE task_attachment SET content = ? WHERE id = ?", ps -> {
            ps.setBytes(1, content);
            ps.setLong(2, attachmentId);
        });
    }

    byte[] read(Long attachmentId) {
        return jdbc.query("SELECT content FROM task_attachment WHERE id = ?",
                rs -> rs.next() ? rs.getBytes(1) : null, attachmentId);
    }
}
