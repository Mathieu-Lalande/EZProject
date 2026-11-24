package com.ezproject.ratelimit;

/** Stockage des seaux à jetons : en mémoire (une instance) ou Redis (plusieurs instances). */
public interface RateLimitStore {

    RateLimiter.Decision tryConsume(String key, RateLimitProperties.Limit limit);

    RateLimiter.Decision peek(String key, RateLimitProperties.Limit limit);

    void reset(String key);
}
