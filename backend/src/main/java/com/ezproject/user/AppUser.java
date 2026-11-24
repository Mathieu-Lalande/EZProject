package com.ezproject.user;

import java.math.BigDecimal;
import java.time.Instant;

import com.ezproject.organization.Organization;

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
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

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

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Jours travaillés par semaine (temps partiel : 4, 4.5…) : base du calcul de capacité. */
    @Column(name = "capacity_days_per_week", nullable = false)
    private BigDecimal capacityDaysPerWeek = BigDecimal.valueOf(5);

    /** Secret TOTP chiffré (AES-GCM) ; présent dès l'initialisation, actif seulement après confirmation. */
    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    /** Dernier pas de temps TOTP accepté : un code ne peut pas être rejoué. */
    @Column(name = "mfa_last_step")
    private Long mfaLastStep;

    protected AppUser() {
        // JPA
    }

    public AppUser(Organization organization, String email, String displayName, String passwordHash, Role role,
                   String jobTitle) {
        this.organization = organization;
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

    public void updateProfile(String newDisplayName, String newJobTitle) {
        this.displayName = newDisplayName;
        this.jobTitle = newJobTitle;
    }

    /** Mot de passe choisi par l'utilisateur lui-même. */
    public void changePassword(String newHash) {
        this.passwordHash = newHash;
        this.mustChangePassword = false;
    }

    /** Mot de passe provisoire défini par un administrateur : à changer à la prochaine connexion. */
    public void resetPassword(String temporaryHash) {
        this.passwordHash = temporaryHash;
        this.mustChangePassword = true;
    }

    public void prepareMfa(String encryptedSecret) {
        this.mfaSecret = encryptedSecret;
        this.mfaEnabled = false;
        this.mfaLastStep = null;
    }

    public void enableMfa() {
        this.mfaEnabled = true;
    }

    public void disableMfa() {
        this.mfaSecret = null;
        this.mfaEnabled = false;
        this.mfaLastStep = null;
    }

    public void acceptMfaStep(long step) {
        this.mfaLastStep = step;
    }

    public void changeCapacity(BigDecimal daysPerWeek) {
        this.capacityDaysPerWeek = daysPerWeek;
    }

    public void setActive(boolean value) {
        this.active = value;
    }

    /**
     * Droit à l'effacement (RGPD) : les données personnelles sont supprimées, l'historique des projets reste
     * cohérent (les tâches et journaux pointent vers un « Utilisateur supprimé »).
     */
    public void anonymize(String unusablePasswordHash) {
        this.email = "supprime-" + id + "@deleted.invalid";
        this.displayName = "Utilisateur supprimé";
        this.jobTitle = null;
        this.passwordHash = unusablePasswordHash;
        this.active = false;
        this.mustChangePassword = false;
        this.deletedAt = Instant.now();
        disableMfa();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean belongsTo(Organization org) {
        return organization.getId().equals(org.getId());
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public String getJobTitle() { return jobTitle; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public Instant getDeletedAt() { return deletedAt; }
    public BigDecimal getCapacityDaysPerWeek() { return capacityDaysPerWeek; }
    public String getMfaSecret() { return mfaSecret; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public Long getMfaLastStep() { return mfaLastStep; }
}
