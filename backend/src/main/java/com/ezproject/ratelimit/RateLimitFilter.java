package com.ezproject.ratelimit;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limitation anti-abus, appliquée dans la chaîne Spring Security avant l'authentification :
 * <ul>
 *   <li>connexion : quota par IP + blocage temporaire du compte après échecs répétés ;</li>
 *   <li>inscription : quota par IP ;</li>
 *   <li>import CSV : quota par utilisateur ;</li>
 *   <li>reste de l'API : quota par utilisateur (ou par IP si anonyme).</li>
 * </ul>
 * Réponse 429 au format Problem Details avec en-tête Retry-After.
 * <p>
 * L'IP est {@code request.getRemoteAddr()} : derrière un reverse proxy de confiance, activer
 * {@code server.forward-headers-strategy=native} pour qu'elle reflète le client réel (jamais X-Forwarded-For brut,
 * falsifiable).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter limiter;
    private final LoginThrottle loginThrottle;
    private final RateLimitProperties props;
    private final com.ezproject.observability.AppMetrics metrics;

    public RateLimitFilter(RateLimiter limiter, LoginThrottle loginThrottle, RateLimitProperties props,
                           com.ezproject.observability.AppMetrics metrics) {
        this.metrics = metrics;
        this.limiter = limiter;
        this.loginThrottle = loginThrottle;
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !props.enabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        boolean post = HttpMethod.POST.matches(request.getMethod());
        String ip = request.getRemoteAddr();
        RateLimiter.Decision decision;
        String policy;

        if (post && uri.equals("/api/auth/login")) {
            policy = "login-ip";
            decision = limiter.tryConsume("login-ip", props.loginIp(), ip);
            if (decision.allowed()) {
                policy = "login-account";
                decision = loginThrottle.check(request.getParameter("email"));
            }
        } else if (post && uri.equals("/api/auth/signup")) {
            policy = "signup-ip";
            decision = limiter.tryConsume("signup-ip", props.signupIp(), ip);
        } else {
            String user = authenticatedUser();
            if (user == null) {
                policy = "api-ip";
                decision = limiter.tryConsume("api-ip", props.apiAnonymous(), ip);
            } else if (post && uri.startsWith("/api/import/")) {
                policy = "import-user";
                decision = limiter.tryConsume("import-user", props.importUser(), user);
            } else {
                policy = "api-user";
                decision = limiter.tryConsume("api-user", props.apiUser(), user);
            }
        }

        if (decision.allowed()) {
            chain.doFilter(request, response);
        } else {
            metrics.rateLimited(policy);
            reject(response, decision.retryAfterSeconds());
        }
    }

    private static String authenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    private static void reject(HttpServletResponse response, long retryAfter) throws IOException {
        long minutes = Math.max(1, (retryAfter + 59) / 60);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                + "\"detail\":\"Trop de tentatives. Réessayez dans " + minutes + " minute" + (minutes > 1 ? "s" : "")
                + ".\"}");
    }
}
