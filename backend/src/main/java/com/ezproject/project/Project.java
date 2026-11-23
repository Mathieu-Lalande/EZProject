package com.ezproject.project;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.ezproject.access.Visibility;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.organization.Organization;
import com.ezproject.team.Team;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false)
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

    @Column(name = "status_reason")
    private String statusReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

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

    public Project(Organization organization, Team team, String code, String name, ProjectType type) {
        this.organization = organization;
        this.team = team;
        this.code = code;
        this.name = name;
        this.type = type;
        this.status = ProjectStatus.PLANNED;
        this.priority = ProjectPriority.P3;
        this.visibility = Visibility.ORGANIZATION;
        this.health = Health.GREEN;
        this.color = "#6366f1";
        this.modules = type.defaultModules();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** Met à jour les informations descriptives en garantissant la cohérence des dates. */
    public void update(String name, String description, String client, ProjectType type, ProjectPriority priority,
                       Health health, String color, AppUser owner, LocalDate startDate, LocalDate endDate,
                       Integer budgetDays) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("La date de fin doit être postérieure à la date de début");
        }
        this.name = name;
        this.description = description;
        this.client = client;
        this.type = type;
        this.priority = priority;
        this.health = health;
        this.color = color;
        this.owner = owner;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budgetDays = budgetDays;
        touch();
    }

    public void assign(Team newTeam, Visibility newVisibility) {
        this.team = newTeam;
        this.visibility = newVisibility;
        touch();
    }

    /** Météo publiée dans un rapport d'avancement : devient la météo courante du projet. */
    public void changeHealth(Health newHealth) {
        this.health = newHealth;
        touch();
    }

    public void prioritize(ProjectPriority newPriority) {
        this.priority = newPriority;
        touch();
    }

    /**
     * Change le statut et produit l'entrée de journal correspondante.
     * Un passage en pause ou une annulation exige un motif.
     *
     * @return l'événement à persister, ou {@code null} si le statut est inchangé
     */
    public ProjectStatusChange changeStatus(ProjectStatus newStatus, String reason, AppUser by) {
        if (newStatus == status) {
            return null;
        }
        if (newStatus.requiresReason() && !StringUtils.hasText(reason)) {
            throw new BusinessRuleException("Un motif est obligatoire pour mettre en pause ou annuler un projet");
        }
        ProjectStatusChange change = new ProjectStatusChange(this, status, newStatus,
                StringUtils.hasText(reason) ? reason.trim() : null, by);
        this.status = newStatus;
        this.statusReason = newStatus.requiresReason() ? reason.trim() : null;
        touch();
        return change;
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
    public Organization getOrganization() { return organization; }
    public Team getTeam() { return team; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getClient() { return client; }
    public ProjectType getType() { return type; }
    public ProjectStatus getStatus() { return status; }
    public String getStatusReason() { return statusReason; }
    public ProjectPriority getPriority() { return priority; }
    public Visibility getVisibility() { return visibility; }
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
