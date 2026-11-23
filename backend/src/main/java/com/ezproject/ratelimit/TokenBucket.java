package com.ezproject.ratelimit;

import java.time.Duration;

/**
 * Seau à jetons : {@code capacity} jetons, rechargés en continu sur {@code window}.
 * Autorise de courtes rafales tout en plafonnant le débit moyen. Thread-safe.
 */
final class TokenBucket {

    private final double capacity;
    private final double refillPerNano;
    private double tokens;
    private long lastRefill;

    TokenBucket(int capacity, Duration window, long now) {
        this.capacity = capacity;
        this.refillPerNano = capacity / (double) window.toNanos();
        this.tokens = capacity;
        this.lastRefill = now;
    }

    synchronized boolean tryConsume(long now) {
        refill(now);
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    synchronized boolean isEmpty(long now) {
        refill(now);
        return tokens < 1;
    }

    /** Secondes avant qu'un jeton soit de nouveau disponible. */
    synchronized long secondsUntilAvailable(long now) {
        refill(now);
        if (tokens >= 1) {
            return 0;
        }
        return Math.max(1, (long) Math.ceil((1 - tokens) / refillPerNano / 1_000_000_000d));
    }

    private void refill(long now) {
        long elapsed = Math.max(0, now - lastRefill);
        tokens = Math.min(capacity, tokens + elapsed * refillPerNano);
        lastRefill = now;
    }
}
