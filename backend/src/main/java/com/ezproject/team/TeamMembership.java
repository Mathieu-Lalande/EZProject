package com.ezproject.team;

import com.ezproject.access.ProjectRole;
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

/** Appartenance d'un utilisateur à une équipe, avec le rôle qu'il aura sur les projets de l'équipe. */
@Entity
@Table(name = "team_member")
public class TeamMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;

    protected TeamMembership() {
        // JPA
    }

    public TeamMembership(Team team, AppUser user, ProjectRole role) {
        this.team = team;
        this.user = user;
        this.role = role;
    }

    public void changeRole(ProjectRole newRole) {
        this.role = newRole;
    }

    public Long getId() { return id; }
    public Team getTeam() { return team; }
    public AppUser getUser() { return user; }
    public ProjectRole getRole() { return role; }
}
