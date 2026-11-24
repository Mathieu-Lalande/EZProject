package com.ezproject.poker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Relais des changements de planning poker, après validation de la transaction :
 * localement (une instance) ou via Redis Pub/Sub pour toucher les clients connectés aux autres instances.
 */
@Component
public class PokerEventRelay {

    static final String CHANNEL = "ezproject:poker";

    private final PokerEventHub hub;
    private final StringRedisTemplate redis;
    private final boolean redisEnabled;

    public PokerEventRelay(PokerEventHub hub, org.springframework.beans.factory.ObjectProvider<StringRedisTemplate> redis,
                           @Value("${app.redis.enabled:false}") boolean redisEnabled) {
        this.hub = hub;
        this.redis = redisEnabled ? redis.getIfAvailable() : null;
        this.redisEnabled = redisEnabled && this.redis != null;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChange(PokerChangedEvent event) {
        if (redisEnabled) {
            redis.convertAndSend(CHANNEL, event.projectId() + ":" + event.roundId());
        } else {
            hub.broadcast(event);
        }
    }

    /** Abonnement Redis : chaque instance rediffuse localement les événements publiés par toutes les instances. */
    @Configuration
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    static class RedisSubscription {

        @Bean
        RedisMessageListenerContainer pokerListener(RedisConnectionFactory factory, PokerEventHub hub) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(factory);
            container.addMessageListener((message, pattern) -> {
                String[] parts = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8).split(":");
                if (parts.length == 2) {
                    hub.broadcast(new PokerChangedEvent(Long.valueOf(parts[0]), Long.valueOf(parts[1])));
                }
            }, new ChannelTopic(CHANNEL));
            return container;
        }
    }
}
