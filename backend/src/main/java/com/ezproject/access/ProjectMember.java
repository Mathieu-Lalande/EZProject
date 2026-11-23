package com.ezproject.access;

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

/** Accès explicite d'un utilisateur à un projet (membre transverse, invité client…). */
@Entity
@Table(name = "project_member")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;

    protected ProjectMember() {
        // JPA
    }

    public ProjectMember(Project project, AppUser user, ProjectRole role) {
        this.project = project;
        this.user = user;
        this.role = role;
    }

    public void changeRole(ProjectRole newRole) {
        this.role = newRole;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public AppUser getUser() { return user; }
    public ProjectRole getRole() { return role; }
}
