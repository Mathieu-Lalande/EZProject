package com.ezproject.ratelimit;

import java.time.Duration;
import java.util.function.LongSupplier;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Limiteur en mémoire, borné (Caffeine : taille maximale + expiration) pour résister à un grand nombre de clés.
 * Une instance par nœud : en déploiement multi-instances, remplacer par un stockage partagé (ex. Redis).
 */
@Component
public class RateLimiter {

    /** Résultat d'une demande : autorisée, ou refusée avec délai d'attente conseillé. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
        static final Decision OK = new Decision(true, 0);
    }

    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    private final LongSupplier clock;

    public RateLimiter() {
        this(System::nanoTime);
    }

    RateLimiter(LongSupplier nanoClock) {
        this.clock = nanoClock;
    }

    public Decision tryConsume(String policy, RateLimitProperties.Limit limit, String key) {
        long now = clock.getAsLong();
        TokenBucket bucket = bucket(policy, limit, key, now);
        return bucket.tryConsume(now) ? Decision.OK : new Decision(false, bucket.secondsUntilAvailable(now));
    }

    /** Consulte sans consommer : la clé a-t-elle épuisé son quota ? */
    public Decision peek(String policy, RateLimitProperties.Limit limit, String key) {
        TokenBucket bucket = buckets.getIfPresent(policy + ':' + key);
        long now = clock.getAsLong();
        return bucket == null || !bucket.isEmpty(now) ? Decision.OK : new Decision(false, bucket.secondsUntilAvailable(now));
    }

    public void reset(String policy, String key) {
        buckets.invalidate(policy + ':' + key);
    }

    private TokenBucket bucket(String policy, RateLimitProperties.Limit limit, String key, long now) {
        return buckets.get(policy + ':' + key, k -> new TokenBucket(limit.capacity(), limit.window(), now));
    }
}
