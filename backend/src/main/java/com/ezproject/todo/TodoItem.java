package com.ezproject.todo;

import java.time.Instant;
import java.time.LocalDate;

import com.ezproject.project.Project;
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
 * Todo léger. Sans projet : todo personnel, visible uniquement par son propriétaire.
 * Avec projet : checklist partagée de l'équipe projet.
 */
@Entity
@Table(name = "todo_item")
public class TodoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TodoItem() {
        // JPA
    }

    public TodoItem(AppUser owner, Project project, String title, LocalDate dueDate) {
        this.owner = owner;
        this.project = project;
        this.title = title;
        this.dueDate = dueDate;
        this.createdAt = Instant.now();
    }

    public void update(String title, boolean done, LocalDate dueDate) {
        this.title = title;
        this.done = done;
        this.dueDate = dueDate;
    }

    public boolean isPersonal() {
        return project == null;
    }

    public boolean isVisibleTo(AppUser user) {
        return !isPersonal() || owner.getId().equals(user.getId());
    }

    public Long getId() { return id; }
    public AppUser getOwner() { return owner; }
    public Project getProject() { return project; }
    public String getTitle() { return title; }
    public boolean isDone() { return done; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getCreatedAt() { return createdAt; }
}
