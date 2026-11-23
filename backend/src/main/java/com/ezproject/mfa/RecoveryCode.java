package com.ezproject.mfa;

import java.time.Instant;

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

/** Code de secours à usage unique (stocké haché, comme un mot de passe). */
@Entity
@Table(name = "mfa_recovery_code")
public class RecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;

    protected RecoveryCode() {
        // JPA
    }

    public RecoveryCode(AppUser user, String codeHash) {
        this.user = user;
        this.codeHash = codeHash;
    }

    public void use() {
        this.usedAt = Instant.now();
    }

    public String getCodeHash() { return codeHash; }
    public boolean isUsed() { return usedAt != null; }
}
