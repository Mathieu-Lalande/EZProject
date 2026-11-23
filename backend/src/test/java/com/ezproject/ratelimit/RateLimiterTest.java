package com.ezproject.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final AtomicLong now = new AtomicLong();
    private final RateLimiter limiter = new RateLimiter(now::get);
    private final RateLimitProperties.Limit threePerMinute = new RateLimitProperties.Limit(3, Duration.ofMinutes(1));

    @Test
    void allowsBurstUpToCapacityThenRejectsWithRetryAfter() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryConsume("p", threePerMinute, "k").allowed()).isTrue();
        }
        RateLimiter.Decision rejected = limiter.tryConsume("p", threePerMinute, "k");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(20);
    }

    @Test
    void refillsProgressively() {
        for (int i = 0; i < 3; i++) {
            limiter.tryConsume("p", threePerMinute, "k");
        }
        now.addAndGet(Duration.ofSeconds(20).toNanos());
        assertThat(limiter.tryConsume("p", threePerMinute, "k").allowed()).isTrue();
        assertThat(limiter.tryConsume("p", threePerMinute, "k").allowed()).isFalse();
    }

    @Test
    void keysAndPoliciesAreIndependentAndResettable() {
        for (int i = 0; i < 3; i++) {
            limiter.tryConsume("p", threePerMinute, "a");
        }
        assertThat(limiter.tryConsume("p", threePerMinute, "b").allowed()).isTrue();
        assertThat(limiter.tryConsume("other", threePerMinute, "a").allowed()).isTrue();
        assertThat(limiter.peek("p", threePerMinute, "a").allowed()).isFalse();
        limiter.reset("p", "a");
        assertThat(limiter.peek("p", threePerMinute, "a").allowed()).isTrue();
    }
}
