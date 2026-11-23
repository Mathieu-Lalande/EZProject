package com.ezproject.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.ezproject.organization.Organization;
import com.ezproject.user.AppUser;
import com.ezproject.user.UserService;

/** Accès à l'utilisateur authentifié et à son organisation (le locataire courant). */
@Component
public class CurrentUser {

    private final UserService userService;

    public CurrentUser(UserService userService) {
        this.userService = userService;
    }

    public AppUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Aucun utilisateur authentifié");
        }
        return userService.getByEmail(auth.getName());
    }

    public Organization organization() {
        return get().getOrganization();
    }
}
