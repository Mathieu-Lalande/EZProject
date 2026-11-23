package com.ezproject.organization;

import java.time.Instant;

import com.ezproject.calendar.HolidayCalendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Locataire (tenant) SaaS : toutes les données métier appartiennent à une organisation. */
@Entity
@Table(name = "organization")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Column(name = "custom_permissions", nullable = false)
    private boolean customPermissions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_calendar", nullable = false)
    private HolidayCalendar holidayCalendar = HolidayCalendar.FR;

    protected Organization() {
        // JPA
    }

    public Organization(String name, String slug, Plan plan) {
        this.name = name;
        this.slug = slug;
        this.plan = plan;
        this.createdAt = Instant.now();
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public void changePlan(Plan newPlan) {
        this.plan = newPlan;
    }

    public void changeHolidayCalendar(HolidayCalendar calendar) {
        this.holidayCalendar = calendar;
    }

    public void markPermissionsCustomized(boolean customized) {
        this.customPermissions = customized;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Plan getPlan() { return plan; }
    public boolean hasCustomPermissions() { return customPermissions; }
    public Instant getCreatedAt() { return createdAt; }
    public HolidayCalendar getHolidayCalendar() { return holidayCalendar; }
}
