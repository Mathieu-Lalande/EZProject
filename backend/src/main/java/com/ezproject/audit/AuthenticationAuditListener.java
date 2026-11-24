package com.ezproject.audit;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ezproject.user.AppUserRepository;

/** Trace les connexions réussies dans le journal de l'organisation. */
@Component
class AuthenticationAuditListener {

    private final AuditService audit;
    private final AppUserRepository users;

    AuthenticationAuditListener(AuditService audit, AppUserRepository users) {
        this.audit = audit;
        this.users = users;
    }

    @EventListener
    @Transactional
    public void onSuccess(AuthenticationSuccessEvent event) {
        users.findByEmailIgnoreCase(event.getAuthentication().getName())
                .ifPresent(u -> audit.record(u.getOrganization(), u, "LOGIN", "USER", u.getId(), "Connexion"));
    }
}
