package com.ezproject.report;

import java.time.Instant;
import java.time.LocalDate;

import com.ezproject.project.Health;
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

/** Point d'avancement publié (météo hebdomadaire / COPIL) : immuable, constitue l'historique. */
@Entity
@Table(name = "status_report")
public class StatusReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Health health;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Trend trend;

    @Column(nullable = false)
    private String summary;

    private String achievements;

    @Column(name = "next_steps")
    private String nextSteps;

    @Column(name = "attention_points")
    private String attentionPoints;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StatusReport() {
        // JPA
    }

    public StatusReport(Project project, AppUser author, LocalDate reportDate, Health health, Trend trend,
                        String summary, String achievements, String nextSteps, String attentionPoints) {
        this.project = project;
        this.author = author;
        this.reportDate = reportDate;
        this.health = health;
        this.trend = trend;
        this.summary = summary;
        this.achievements = achievements;
        this.nextSteps = nextSteps;
        this.attentionPoints = attentionPoints;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public AppUser getAuthor() { return author; }
    public LocalDate getReportDate() { return reportDate; }
    public Health getHealth() { return health; }
    public Trend getTrend() { return trend; }
    public String getSummary() { return summary; }
    public String getAchievements() { return achievements; }
    public String getNextSteps() { return nextSteps; }
    public String getAttentionPoints() { return attentionPoints; }
}
