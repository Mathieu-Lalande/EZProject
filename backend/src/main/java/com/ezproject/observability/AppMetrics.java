package com.ezproject.observability;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Métriques métier et sécurité exposées à Prometheus (préfixe ezproject_), en complément des métriques
 * techniques fournies par Spring Boot (HTTP, JVM, pool de connexions…).
 */
@Component
public class AppMetrics {

    private final MeterRegistry registry;

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Connexions par résultat : success, failure, mfa_required. */
    public void login(String outcome) {
        registry.counter("ezproject.auth.login", "outcome", outcome).increment();
    }

    public void mfa(String outcome) {
        registry.counter("ezproject.auth.mfa", "outcome", outcome).increment();
    }

    /** Requêtes refusées par la limitation anti-abus, par politique (login-ip, api-user…). */
    public void rateLimited(String policy) {
        registry.counter("ezproject.ratelimit.rejected", "policy", policy).increment();
    }

    public void imported(String kind, boolean applied) {
        registry.counter("ezproject.import", "kind", kind, "applied", String.valueOf(applied)).increment();
    }

    public MeterRegistry registry() {
        return registry;
    }
}
