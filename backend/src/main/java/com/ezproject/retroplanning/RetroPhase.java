package com.ezproject.retroplanning;

import com.ezproject.project.Project;

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
@Table(name = "retro_phase")
public class RetroPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseKind kind;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean done;

    protected RetroPhase() {
        // JPA
    }

    public RetroPhase(Project project, String name, PhaseKind kind, int durationDays, int sortOrder) {
        this.project = project;
        this.sortOrder = sortOrder;
        update(name, kind, durationDays, false);
    }

    public final void update(String name, PhaseKind kind, int durationDays, boolean done) {
        this.name = name;
        this.kind = kind;
        // Un jalon n'a pas de durée ; une phase dure au moins un jour ouvré
        this.durationDays = kind == PhaseKind.MILESTONE ? 0 : Math.max(1, durationDays);
        this.done = done;
    }

    void reorder(int newOrder) {
        this.sortOrder = newOrder;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getName() { return name; }
    public PhaseKind getKind() { return kind; }
    public int getDurationDays() { return durationDays; }
    public int getSortOrder() { return sortOrder; }
    public boolean isDone() { return done; }
}
