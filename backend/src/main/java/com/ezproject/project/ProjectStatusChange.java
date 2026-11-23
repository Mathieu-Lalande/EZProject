package com.ezproject.project;

import java.time.Instant;

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

/** Entrée du journal des statuts d'un projet (qui, quand, pourquoi). Immuable. */
@Entity
@Table(name = "project_status_change")
public class ProjectStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private ProjectStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private ProjectStatus toStatus;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private AppUser changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ProjectStatusChange() {
        // JPA
    }

    public ProjectStatusChange(Project project, ProjectStatus from, ProjectStatus to, String reason, AppUser by) {
        this.project = project;
        this.fromStatus = from;
        this.toStatus = to;
        this.reason = reason;
        this.changedBy = by;
        this.changedAt = Instant.now();
    }

    public Long getId() { return id; }
    public ProjectStatus getFromStatus() { return fromStatus; }
    public ProjectStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public AppUser getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
