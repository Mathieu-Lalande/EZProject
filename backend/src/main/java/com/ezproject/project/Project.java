package com.ezproject.project;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import com.ezproject.common.BusinessRuleException;
import com.ezproject.user.AppUser;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    private String client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Health health;

    @Column(nullable = false)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "budget_days")
    private Integer budgetDays;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_module", joinColumns = @JoinColumn(name = "project_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "module")
    private Set<ModuleType> modules = EnumSet.noneOf(ModuleType.class);

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
        // JPA
    }

    public Project(String code, String name, ProjectType type) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.status = ProjectStatus.PLANNED;
        this.health = Health.GREEN;
        this.color = "#6366f1";
        this.modules = type.defaultModules();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Met à jour les informations descriptives en garantissant la cohérence des dates. */
    public void update(String name, String description, String client, ProjectType type, ProjectStatus status,
                       Health health, String color, AppUser owner, LocalDate startDate, LocalDate endDate,
                       Integer budgetDays) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("La date de fin doit être postérieure à la date de début");
        }
        this.name = name;
        this.description = description;
        this.client = client;
        this.type = type;
        this.status = status;
        this.health = health;
        this.color = color;
        this.owner = owner;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budgetDays = budgetDays;
        touch();
    }

    public void replaceModules(Set<ModuleType> newModules) {
        this.modules.clear();
        this.modules.addAll(newModules);
        touch();
    }

    public boolean hasModule(ModuleType module) {
        return modules.contains(module);
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getClient() { return client; }
    public ProjectType getType() { return type; }
    public ProjectStatus getStatus() { return status; }
    public Health getHealth() { return health; }
    public String getColor() { return color; }
    public AppUser getOwner() { return owner; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Integer getBudgetDays() { return budgetDays; }
    public Set<ModuleType> getModules() { return Set.copyOf(modules); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
