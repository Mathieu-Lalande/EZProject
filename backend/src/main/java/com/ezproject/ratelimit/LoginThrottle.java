package com.ezproject.ratelimit;

import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Protection contre la force brute sur un compte : après N échecs, le compte est temporairement bloqué
 * (quelle que soit l'adresse IP). Le compteur est tenu aussi pour les emails inexistants, afin de ne pas
 * révéler quels comptes existent. Un succès remet le compteur à zéro.
 */
@Component
public class LoginThrottle {

    static final String POLICY = "login-account";

    private final RateLimiter limiter;
    private final RateLimitProperties properties;

    public LoginThrottle(RateLimiter limiter, RateLimitProperties properties) {
        this.limiter = limiter;
        this.properties = properties;
    }

    public RateLimiter.Decision check(String email) {
        if (!properties.enabled() || email == null) {
            return RateLimiter.Decision.OK;
        }
        return limiter.peek(POLICY, properties.loginAccount(), normalize(email));
    }

    public void recordFailure(String email) {
        if (properties.enabled() && email != null) {
            limiter.tryConsume(POLICY, properties.loginAccount(), normalize(email));
        }
    }

    public void recordSuccess(String email) {
        if (email != null) {
            limiter.reset(POLICY, normalize(email));
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
