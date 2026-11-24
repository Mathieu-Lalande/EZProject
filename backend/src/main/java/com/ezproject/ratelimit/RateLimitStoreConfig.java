package com.ezproject.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
class RateLimitStoreConfig {

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    RateLimitStore redisRateLimitStore(StringRedisTemplate redis) {
        return new RedisRateLimitStore(redis);
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitStore.class)
    RateLimitStore inMemoryRateLimitStore() {
        return new InMemoryRateLimitStore();
    }
}
