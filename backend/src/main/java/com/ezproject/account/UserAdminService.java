package com.ezproject.account;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.audit.AuditService;
import com.ezproject.common.BusinessRuleException;
import com.ezproject.security.CurrentUser;
import com.ezproject.user.AppUser;
import com.ezproject.user.AppUserRepository;
import com.ezproject.user.Role;
import com.ezproject.user.UserDto;
import com.ezproject.user.UserService;

/** Cycle de vie des comptes par un administrateur : mot de passe provisoire, désactivation, suppression. */
@Service
@Transactional
public class UserAdminService {

    public record TemporaryPassword(String temporaryPassword) {
    }

    private final CurrentUser currentUser;
    private final UserService userService;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final AccountService accountService;
    private final AuditService audit;

    public UserAdminService(CurrentUser currentUser, UserService userService, AppUserRepository users,
                            PasswordEncoder encoder, AccountService accountService, AuditService audit) {
        this.currentUser = currentUser;
        this.userService = userService;
        this.users = users;
        this.encoder = encoder;
        this.accountService = accountService;
        this.audit = audit;
    }

    /** Génère un mot de passe provisoire, affiché une seule fois à l'administrateur ; à changer à la connexion. */
    public TemporaryPassword resetPassword(Long userId) {
        AppUser target = manageable(userId);
        String temporary = PasswordPolicy.temporary();
        target.resetPassword(encoder.encode(temporary));
        audit.record("PASSWORD_RESET", "USER", target.getId(), "Mot de passe réinitialisé pour " + target.getDisplayName());
        return new TemporaryPassword(temporary);
    }

    /** Désactive (accès coupé immédiatement, sessions en cours comprises) ou réactive un compte. */
    public UserDto setActive(Long userId, boolean active) {
        AppUser target = manageable(userId);
        if (active && !target.isActive()) {
            var org = target.getOrganization();
            if (!org.getPlan().allowsMoreUsers(users.countByOrganizationIdAndActiveTrueAndDeletedAtIsNull(org.getId()))) {
                throw new BusinessRuleException("Limite d'utilisateurs atteinte pour l'offre " + org.getPlan());
            }
        }
        target.setActive(active);
        audit.record(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", target.getId(),
                (active ? "Réactivation de " : "Désactivation de ") + target.getDisplayName());
        return UserDto.from(target);
    }

    /** Suppression (anonymisation RGPD) d'un compte par un administrateur. */
    public void delete(Long userId) {
        AppUser target = manageable(userId);
        audit.record("USER_DELETED", "USER", target.getId(), "Suppression du compte de " + target.getDisplayName());
        accountService.anonymize(target);
    }

    private AppUser manageable(Long userId) {
        AppUser me = currentUser.get();
        if (!me.getRole().isAdmin()) {
            throw new AccessDeniedException("Réservé aux administrateurs");
        }
        AppUser target = userService.getInOrganization(userId, me.getOrganization().getId());
        if (target.getId().equals(me.getId())) {
            throw new BusinessRuleException("Utilisez « Mon compte » pour agir sur votre propre compte");
        }
        if (target.isDeleted()) {
            throw new BusinessRuleException("Ce compte a été supprimé");
        }
        if (target.getRole() == Role.OWNER && me.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Seul un propriétaire peut gérer le compte d'un propriétaire");
        }
        return target;
    }
}
