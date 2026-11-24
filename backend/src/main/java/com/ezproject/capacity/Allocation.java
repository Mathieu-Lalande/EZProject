package com.ezproject.capacity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.ezproject.common.BusinessRuleException;
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

/** Staffing : une personne est affectée à un projet X jours par semaine sur une période. */
@Entity
@Table(name = "allocation")
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "days_per_week", nullable = false)
    private BigDecimal daysPerWeek;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Allocation() {
        // JPA
    }

    public Allocation(Project project, AppUser user, LocalDate startDate, LocalDate endDate, BigDecimal daysPerWeek) {
        this.project = project;
        this.user = user;
        this.createdAt = Instant.now();
        update(startDate, endDate, daysPerWeek);
    }

    public final void update(LocalDate start, LocalDate end, BigDecimal perWeek) {
        if (end.isBefore(start)) {
            throw new BusinessRuleException("La fin de l'affectation doit suivre son début");
        }
        this.startDate = start;
        this.endDate = end;
        this.daysPerWeek = perWeek;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public AppUser getUser() { return user; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getDaysPerWeek() { return daysPerWeek; }
}
