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

@Entity
@Table(name = "task_comment")
public class TaskComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(nullable = false)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    protected TaskComment() {
        // JPA
    }

    public TaskComment(Task task, AppUser author, String body) {
        this.task = task;
        this.author = author;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public void edit(String newBody) {
        this.body = newBody;
        this.editedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Task getTask() { return task; }
    public AppUser getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEditedAt() { return editedAt; }
}
