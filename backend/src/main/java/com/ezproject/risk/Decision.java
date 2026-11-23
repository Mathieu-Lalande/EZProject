package com.ezproject.risk;

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

/** Décision projet (relevé de décisions COPIL / COPROJ). */
@Entity
@Table(name = "decision")
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "decided_on")
    private LocalDate decidedOn;

    /** Instance ou personne ayant décidé (texte libre : « COPIL du 12/03 », « Client »…). */
    @Column(name = "decided_by")
    private String decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Decision() {
        // JPA
    }

    public Decision(Project project, AppUser createdBy) {
        this.project = project;
        this.createdBy = createdBy;
        this.status = DecisionStatus.PROPOSED;
        this.createdAt = Instant.now();
    }

    public void update(String title, String description, LocalDate decidedOn, String decidedBy, DecisionStatus status) {
        this.title = title;
        this.description = description;
        this.decidedOn = decidedOn;
        this.decidedBy = decidedBy;
        this.status = status;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getDecidedOn() { return decidedOn; }
    public String getDecidedBy() { return decidedBy; }
    public DecisionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
