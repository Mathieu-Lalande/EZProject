package com.ezproject.mfa;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.audit.AuditService;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.security.CurrentUser;
import com.ezproject.user.AppUser;
import com.ezproject.user.Role;
import com.ezproject.user.UserService;

/** Double authentification TOTP : enrôlement, confirmation, désactivation, vérification à la connexion. */
@Service
@Transactional
public class MfaService {

    /** Attribut de session : mot de passe validé, second facteur attendu. */
    public static final String PENDING_ATTRIBUTE = "EZ_MFA_PENDING";
    private static final String ISSUER = "EZProject";
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Setup(String secret, String otpauthUri) {
    }

    public record RecoveryCodes(List<String> recoveryCodes) {
    }

    private final CurrentUser currentUser;
    private final SecretCipher cipher;
    private final RecoveryCodeRepository recoveryCodes;
    private final PasswordEncoder encoder;
    private final UserService userService;
    private final AuditService audit;
    private final Clock clock;

    public MfaService(CurrentUser currentUser, SecretCipher cipher, RecoveryCodeRepository recoveryCodes,
                      PasswordEncoder encoder, UserService userService, AuditService audit, Clock clock) {
        this.currentUser = currentUser;
        this.cipher = cipher;
        this.recoveryCodes = recoveryCodes;
        this.encoder = encoder;
        this.userService = userService;
        this.audit = audit;
        this.clock = clock;
    }

    /** Étape 1 : génère un secret (non actif tant qu'il n'est pas confirmé par un code). */
    public Setup setup() {
        AppUser me = currentUser.get();
        if (me.isMfaEnabled()) {
            throw new BusinessRuleException("La double authentification est déjà active");
        }
        String secret = Totp.newSecret();
        me.prepareMfa(cipher.encrypt(secret));
        return new Setup(secret, Totp.otpauthUri(ISSUER, me.getEmail(), secret));
    }

    /** Étape 2 : confirme avec un premier code, active et renvoie les codes de secours (affichés une seule fois). */
    public RecoveryCodes enable(String code) {
        AppUser me = currentUser.get();
        if (me.getMfaSecret() == null || me.isMfaEnabled()) {
            throw new BusinessRuleException("Commencez par initialiser la double authentification");
        }
        OptionalLong step = Totp.verify(cipher.decrypt(me.getMfaSecret()), code, nowStep(), null);
        if (step.isEmpty()) {
            throw new BusinessRuleException("Code incorrect : vérifiez l'heure de votre téléphone");
        }
        me.acceptMfaStep(step.getAsLong());
        me.enableMfa();
        recoveryCodes.deleteAll(recoveryCodes.findByUserId(me.getId()));
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String c = recoveryCode();
            codes.add(c);
            recoveryCodes.save(new RecoveryCode(me, encoder.encode(c)));
        }
        audit.record("MFA_ENABLED", "USER", me.getId(), "Double authentification activée");
        return new RecoveryCodes(codes);
    }

    public void disable(String password) {
        AppUser me = currentUser.get();
        if (!encoder.matches(password, me.getPasswordHash())) {
            throw new BusinessRuleException("Mot de passe incorrect");
        }
        reset(me);
        audit.record("MFA_DISABLED", "USER", me.getId(), "Double authentification désactivée");
    }

    /** Téléphone perdu : un administrateur réinitialise la double authentification d'un membre. */
    public void adminReset(Long userId) {
        AppUser me = currentUser.get();
        if (!me.getRole().isAdmin()) {
            throw new AccessDeniedException("Réservé aux administrateurs");
        }
        AppUser target = userService.getInOrganization(userId, me.getOrganization().getId());
        if (target.getRole() == Role.OWNER && me.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Seul un propriétaire peut agir sur un propriétaire");
        }
        reset(target);
        audit.record("MFA_RESET", "USER", userId, "Double authentification réinitialisée pour " + target.getDisplayName());
    }

    /** Second facteur à la connexion : code TOTP (anti-rejeu) ou code de secours (usage unique). */
    public boolean verifyLogin(AppUser user, String code) {
        if (!user.isMfaEnabled() || code == null) {
            return false;
        }
        String clean = code.trim().toUpperCase().replace(" ", "");
        if (clean.matches("\\d{6}")) {
            OptionalLong step = Totp.verify(cipher.decrypt(user.getMfaSecret()), clean, nowStep(), user.getMfaLastStep());
            step.ifPresent(user::acceptMfaStep);
            return step.isPresent();
        }
        for (RecoveryCode rc : recoveryCodes.findByUserId(user.getId())) {
            if (!rc.isUsed() && encoder.matches(clean, rc.getCodeHash())) {
                rc.use();
                audit.record(user.getOrganization(), user, "MFA_RECOVERY_USED", "USER", user.getId(), "Code de secours utilisé");
                return true;
            }
        }
        return false;
    }

    private void reset(AppUser user) {
        user.disableMfa();
        recoveryCodes.deleteAll(recoveryCodes.findByUserId(user.getId()));
    }

    private long nowStep() {
        return Totp.stepAt(clock.instant().getEpochSecond());
    }

    private static String recoveryCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }
        return sb.toString();
    }
}
