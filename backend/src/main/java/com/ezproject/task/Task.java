package com.ezproject.task;

import java.time.Instant;
import java.time.LocalDate;

import com.ezproject.project.Project;
import com.ezproject.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Task() {
        // JPA
    }

    public Task(Project project, String title, TaskStatus status, int position) {
        this.project = project;
        this.title = title;
        this.status = status;
        this.priority = Priority.MEDIUM;
        this.position = position;
        this.createdAt = Instant.now();
        if (status == TaskStatus.DONE) {
            this.completedAt = this.createdAt;
        }
    }

    public void update(String title, String description, Priority priority, AppUser assignee,
                       Integer storyPoints, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignee = assignee;
        this.storyPoints = storyPoints;
        this.dueDate = dueDate;
    }

    /** Change de colonne en tenant à jour la date de complétion. */
    public void moveTo(TaskStatus newStatus, int newPosition) {
        if (newStatus == TaskStatus.DONE && this.status != TaskStatus.DONE) {
            this.completedAt = Instant.now();
        } else if (newStatus != TaskStatus.DONE) {
            this.completedAt = null;
        }
        this.status = newStatus;
        this.position = newPosition;
    }

    void reposition(int newPosition) {
        this.position = newPosition;
    }

    public void estimate(Integer points) {
        this.storyPoints = points;
    }

    public boolean isOverdue(LocalDate today) {
        return status != TaskStatus.DONE && dueDate != null && dueDate.isBefore(today);
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public AppUser getAssignee() { return assignee; }
    public Integer getStoryPoints() { return storyPoints; }
    public LocalDate getDueDate() { return dueDate; }
    public int getPosition() { return position; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
