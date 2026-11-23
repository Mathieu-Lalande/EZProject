package com.ezproject.risk;

import java.time.Instant;
import java.time.LocalDate;

import com.ezproject.common.BusinessRuleException;
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

/** Risque projet : criticité = probabilité × impact (échelles de 1 à 4). */
@Entity
@Table(name = "risk")
public class Risk {

    public static final int CRITICAL_SCORE = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private int probability;

    @Column(nullable = false)
    private int impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    private String mitigation;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Risk() {
        // JPA
    }

    public Risk(Project project) {
        this.project = project;
        this.status = RiskStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public void update(String title, String description, int probability, int impact, RiskStatus status,
                       AppUser owner, String mitigation, LocalDate dueDate) {
        if (probability < 1 || probability > 4 || impact < 1 || impact > 4) {
            throw new BusinessRuleException("Probabilité et impact attendus entre 1 et 4");
        }
        this.title = title;
        this.description = description;
        this.probability = probability;
        this.impact = impact;
        this.status = status;
        this.owner = owner;
        this.mitigation = mitigation;
        this.dueDate = dueDate;
        this.updatedAt = Instant.now();
    }

    public int score() {
        return probability * impact;
    }

    public boolean isCritical() {
        return status.isActive() && score() >= CRITICAL_SCORE;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getProbability() { return probability; }
    public int getImpact() { return impact; }
    public RiskStatus getStatus() { return status; }
    public AppUser getOwner() { return owner; }
    public String getMitigation() { return mitigation; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getUpdatedAt() { return updatedAt; }
}
