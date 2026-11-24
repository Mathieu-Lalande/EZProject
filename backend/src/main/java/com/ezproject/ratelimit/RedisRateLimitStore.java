package com.ezproject.ratelimit;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Seaux à jetons dans Redis, partagés entre instances. La recharge et la consommation sont atomiques
 * (script Lua exécuté côté serveur, horloge Redis : pas de dérive entre instances).
 */
public class RedisRateLimitStore implements RateLimitStore {

    private static final String PREFIX = "ezproject:ratelimit:";

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>("""
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local window_ms = tonumber(ARGV[2])
            local consume = tonumber(ARGV[3])
            local t = redis.call('TIME')
            local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
            local rate = capacity / window_ms
            local data = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(data[1]) or capacity
            local ts = tonumber(data[2]) or now
            tokens = math.min(capacity, tokens + math.max(0, now - ts) * rate)
            local allowed = 0
            if tokens >= 1 then
              allowed = 1
              if consume == 1 then tokens = tokens - 1 end
            end
            redis.call('HSET', key, 'tokens', tostring(tokens), 'ts', tostring(now))
            redis.call('PEXPIRE', key, window_ms * 2)
            local retry = 0
            if allowed == 0 then retry = math.ceil((1 - tokens) / rate) end
            return {allowed, retry}
            """, List.class);

    private final StringRedisTemplate redis;

    public RedisRateLimitStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public RateLimiter.Decision tryConsume(String key, RateLimitProperties.Limit limit) {
        return run(key, limit, true);
    }

    @Override
    public RateLimiter.Decision peek(String key, RateLimitProperties.Limit limit) {
        return run(key, limit, false);
    }

    @Override
    public void reset(String key) {
        redis.delete(PREFIX + key);
    }

    private RateLimiter.Decision run(String key, RateLimitProperties.Limit limit, boolean consume) {
        List<?> result = redis.execute(SCRIPT, List.of(PREFIX + key), String.valueOf(limit.capacity()),
                String.valueOf(limit.window().toMillis()), consume ? "1" : "0");
        boolean allowed = ((Number) result.get(0)).longValue() == 1;
        long retryMs = ((Number) result.get(1)).longValue();
        return allowed ? RateLimiter.Decision.OK : new RateLimiter.Decision(false, Math.max(1, (retryMs + 999) / 1000));
    }
}
