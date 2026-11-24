package com.ezproject.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.Socket;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ezproject.support.DemoData;

/**
 * Mode multi-instances (app.redis.enabled=true). Ignoré si aucun Redis n'écoute sur localhost:6379
 * (la CI démarre un service Redis).
 */
@SpringBootTest(properties = {"app.redis.enabled=true", "spring.data.redis.host=localhost", "spring.data.redis.port=6379"})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RedisIntegrationTest {

    @BeforeAll
    static void requireRedis() {
        try (Socket ignored = new Socket("localhost", 6379)) {
            // Redis disponible
        } catch (Exception e) {
            Assumptions.abort("Redis indisponible sur localhost:6379");
        }
    }

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    RateLimitStore store;

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoData demo;

    @Test
    void countersAreSharedBetweenInstances() {
        assertThat(store).isInstanceOf(RedisRateLimitStore.class);
        var limit = new RateLimitProperties.Limit(3, Duration.ofMinutes(1));
        String key = "test:" + UUID.randomUUID();
        RedisRateLimitStore instanceA = new RedisRateLimitStore(redis);
        RedisRateLimitStore instanceB = new RedisRateLimitStore(redis);

        assertThat(instanceA.tryConsume(key, limit).allowed()).isTrue();
        assertThat(instanceB.tryConsume(key, limit).allowed()).isTrue();
        assertThat(instanceA.tryConsume(key, limit).allowed()).isTrue();
        RateLimiter.Decision refused = instanceB.tryConsume(key, limit);
        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfterSeconds()).isBetween(1L, 20L);
        assertThat(instanceA.peek(key, limit).allowed()).isFalse();
        instanceB.reset(key);
        assertThat(instanceA.peek(key, limit).allowed()).isTrue();
    }

    @Test
    void pokerEventsAreRelayedThroughRedis() throws Exception {
        long portail = demo.project("PORTAIL");
        MvcResult stream = mvc.perform(get("/api/projects/" + portail + "/poker/stream").with(user("sarah@ezproject.local")))
                .andReturn();
        mvc.perform(post("/api/projects/" + portail + "/poker/rounds").with(user("hugo@ezproject.local")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"storyTitle\":\"Relais Redis\"}")).andExpect(status().isCreated());
        long deadline = System.currentTimeMillis() + 3000;
        while (!stream.getResponse().getContentAsString().contains("event:poker") && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(stream.getResponse().getContentAsString()).contains("event:poker");
    }
}
