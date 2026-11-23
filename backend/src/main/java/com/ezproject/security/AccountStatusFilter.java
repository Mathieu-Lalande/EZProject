package com.ezproject.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ezproject.user.AppUser;
import com.ezproject.user.AppUserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Contrôle à chaque requête l'état du compte de la session :
 * <ul>
 *   <li>désactivé ou supprimé : la session est invalidée immédiatement (401), sans attendre son expiration ;</li>
 *   <li>mot de passe provisoire : seules la lecture du profil, le changement de mot de passe et la déconnexion
 *       sont permis (403 PASSWORD_CHANGE_REQUIRED).</li>
 * </ul>
 */
public class AccountStatusFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_WHILE_PASSWORD_CHANGE = Set.of(
            "GET /api/auth/me", "PUT /api/account/password", "POST /api/auth/logout");

    private final AppUserRepository users;

    public AccountStatusFilter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }
        AppUser user = users.findByEmailIgnoreCase(auth.getName()).orElse(null);
        if (user == null || !user.isActive() || user.isDeleted()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            write(response, HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED", "Compte désactivé");
            return;
        }
        if (user.isMustChangePassword()
                && !ALLOWED_WHILE_PASSWORD_CHANGE.contains(request.getMethod() + " " + request.getRequestURI())) {
            write(response, HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
                    "Vous devez changer votre mot de passe provisoire");
            return;
        }
        chain.doFilter(request, response);
    }

    static void write(HttpServletResponse response, HttpStatus status, String code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"status\":" + status.value() + ",\"code\":\"" + code
                + "\",\"detail\":\"" + detail + "\"}");
    }
}
