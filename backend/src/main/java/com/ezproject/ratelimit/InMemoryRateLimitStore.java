package com.ezproject.ratelimit;

import java.time.Duration;
import java.util.function.LongSupplier;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/** Seaux en mémoire, bornés (Caffeine : taille maximale + expiration). Valable pour une seule instance. */
public class InMemoryRateLimitStore implements RateLimitStore {

    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    private final LongSupplier clock;

    public InMemoryRateLimitStore() {
        this(System::nanoTime);
    }

    InMemoryRateLimitStore(LongSupplier nanoClock) {
        this.clock = nanoClock;
    }

    @Override
    public RateLimiter.Decision tryConsume(String key, RateLimitProperties.Limit limit) {
        long now = clock.getAsLong();
        TokenBucket bucket = buckets.get(key, k -> new TokenBucket(limit.capacity(), limit.window(), now));
        return bucket.tryConsume(now) ? RateLimiter.Decision.OK
                : new RateLimiter.Decision(false, bucket.secondsUntilAvailable(now));
    }

    @Override
    public RateLimiter.Decision peek(String key, RateLimitProperties.Limit limit) {
        TokenBucket bucket = buckets.getIfPresent(key);
        long now = clock.getAsLong();
        return bucket == null || !bucket.isEmpty(now) ? RateLimiter.Decision.OK
                : new RateLimiter.Decision(false, bucket.secondsUntilAvailable(now));
    }

    @Override
    public void reset(String key) {
        buckets.invalidate(key);
    }
}
