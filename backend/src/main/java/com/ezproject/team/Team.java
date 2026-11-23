package com.ezproject.team;

import java.time.Instant;

import com.ezproject.organization.Organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Équipe d'une organisation (ex. Delivery Web, Conseil SI, PMO). Un projet appartient à une équipe. */
@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String color;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Team() {
        // JPA
    }

    public Team(Organization organization, String name, String description, String color) {
        this.organization = organization;
        this.createdAt = Instant.now();
        update(name, description, color);
    }

    public final void update(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getColor() { return color; }
}
