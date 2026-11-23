package com.ezproject.audit;

import java.time.Instant;

import com.ezproject.organization.Organization;
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

/** Entrée du journal d'audit : qui a fait quoi, sur quel élément, quand. Immuable. */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    /** Nom au moment de l'action : reste lisible si le compte est supprimé ensuite. */
    @Column(name = "actor_name")
    private String actorName;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEvent() {
        // JPA
    }

    AuditEvent(Organization organization, AppUser actor, String action, String entityType, Long entityId,
               String summary) {
        this.organization = organization;
        this.actor = actor;
        this.actorName = actor == null ? null : actor.getDisplayName();
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.summary = summary == null || summary.length() <= 500 ? summary : summary.substring(0, 497) + "…";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getActorName() { return actorName; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }
}
