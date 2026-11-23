package com.ezproject.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUser() {
        // JPA
    }

    public AppUser(String email, String displayName, String passwordHash, Role role, String jobTitle) {
        this.email = email.toLowerCase();
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.jobTitle = jobTitle;
        this.createdAt = Instant.now();
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getJobTitle() { return jobTitle; }
    public Instant getCreatedAt() { return createdAt; }
}
