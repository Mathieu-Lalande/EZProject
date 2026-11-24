package com.ezproject.ratelimit;

import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Limiteur à seaux à jetons. Le stockage est en mémoire par défaut (une instance) ou dans Redis
 * (app.redis.enabled=true) pour partager les compteurs entre plusieurs instances.
 */
@Component
public class RateLimiter {

    /** Résultat d'une demande : autorisée, ou refusée avec délai d'attente conseillé. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
        static final Decision OK = new Decision(true, 0);
    }

    private final RateLimitStore store;

    @Autowired
    public RateLimiter(RateLimitStore store) {
        this.store = store;
    }

    /** Pour les tests : stockage en mémoire avec horloge simulée. */
    RateLimiter(LongSupplier nanoClock) {
        this(new InMemoryRateLimitStore(nanoClock));
    }

    public Decision tryConsume(String policy, RateLimitProperties.Limit limit, String key) {
        return store.tryConsume(policy + ':' + key, limit);
    }

    /** Consulte sans consommer : la clé a-t-elle épuisé son quota ? */
    public Decision peek(String policy, RateLimitProperties.Limit limit, String key) {
        return store.peek(policy + ':' + key, limit);
    }

    public void reset(String policy, String key) {
        store.reset(policy + ':' + key);
    }
}
