package com.ezproject.task.collab;

import java.time.Instant;

import com.ezproject.task.Task;
import com.ezproject.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Métadonnées d'une pièce jointe. Le contenu binaire (colonne {@code content}) n'est volontairement pas mappé :
 * il est lu et écrit par {@link AttachmentContentStore}, pour ne jamais être chargé avec les listes.
 */
@Entity
@Table(name = "task_attachment")
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private AppUser uploader;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TaskAttachment() {
        // JPA
    }

    public TaskAttachment(Task task, AppUser uploader, String filename, String contentType, long sizeBytes) {
        this.task = task;
        this.uploader = uploader;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public AppUser getUploader() { return uploader; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
}
