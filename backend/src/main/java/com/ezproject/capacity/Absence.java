package com.ezproject.capacity;

import java.time.Instant;
import java.time.LocalDate;

import com.ezproject.common.BusinessRuleException;
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
@Table(name = "absence")
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceKind kind;

    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Absence() {
        // JPA
    }

    public Absence(AppUser user, LocalDate startDate, LocalDate endDate, AbsenceKind kind, String comment) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("La fin de l'absence doit suivre son début");
        }
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.kind = kind;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public AbsenceKind getKind() { return kind; }
    public String getComment() { return comment; }
}
