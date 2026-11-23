package com.ezproject.time;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.ezproject.project.Project;
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

/** Temps passé par une personne sur un projet (en jours, au pas d'une heure : 0,125 j). */
@Entity
@Table(name = "time_entry")
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private BigDecimal days;

    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TimeEntry() {
        // JPA
    }

    public TimeEntry(Project project, Task task, AppUser user, LocalDate workDate, BigDecimal days, String comment) {
        this.project = project;
        this.user = user;
        this.createdAt = Instant.now();
        update(task, workDate, days, comment);
    }

    public final void update(Task newTask, LocalDate newDate, BigDecimal newDays, String newComment) {
        this.task = newTask;
        this.workDate = newDate;
        this.days = newDays;
        this.comment = newComment;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public Task getTask() { return task; }
    public AppUser getUser() { return user; }
    public LocalDate getWorkDate() { return workDate; }
    public BigDecimal getDays() { return days; }
    public String getComment() { return comment; }
}
