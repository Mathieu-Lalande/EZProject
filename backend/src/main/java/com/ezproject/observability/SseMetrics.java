package com.ezproject.observability;

import org.springframework.stereotype.Component;

import com.ezproject.poker.PokerEventHub;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/** Jauge du nombre de connexions temps réel (SSE) ouvertes. */
@Component
class SseMetrics {

    SseMetrics(MeterRegistry registry, PokerEventHub hub) {
        Gauge.builder("ezproject.sse.connections", hub, PokerEventHub::totalConnections)
                .description("Connexions SSE ouvertes (planning poker)")
                .register(registry);
    }
}
