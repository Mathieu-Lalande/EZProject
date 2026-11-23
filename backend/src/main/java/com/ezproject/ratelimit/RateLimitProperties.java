package com.ezproject.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites anti-abus (configurables par environnement, voir application.yml).
 *
 * @param loginIp        tentatives de connexion par adresse IP
 * @param loginAccount   échecs de connexion par compte avant blocage temporaire (anti force brute distribuée)
 * @param signupIp       inscriptions par adresse IP
 * @param importUser     imports CSV par utilisateur (opération coûteuse)
 * @param apiUser        requêtes API par utilisateur authentifié
 * @param apiAnonymous   requêtes API anonymes par adresse IP
 */
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(boolean enabled, Limit loginIp, Limit loginAccount, Limit signupIp,
                                  Limit importUser, Limit apiUser, Limit apiAnonymous) {

    /** {@code capacity} requêtes autorisées par fenêtre {@code window}, avec recharge progressive. */
    public record Limit(int capacity, Duration window) {
    }
}
