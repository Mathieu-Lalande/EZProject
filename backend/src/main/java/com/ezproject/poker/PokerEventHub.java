package com.ezproject.poker;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ezproject.common.BusinessRuleException;

/**
 * Diffusion temps réel (Server-Sent Events) des changements de planning poker.
 * Le flux ne transporte qu'un signal « tour X modifié » : chaque client relit l'état via l'API REST,
 * avec ses propres droits — aucune carte non révélée ne transite par le flux.
 * En mémoire, par instance : en déploiement multi-instances, relayer les événements (ex. Redis Pub/Sub).
 */
@Component
public class PokerEventHub {

    private static final long TIMEOUT_MS = 30 * 60 * 1000L;
    private static final int MAX_PER_PROJECT = 200;

    private final Map<Long, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long projectId) {
        Set<SseEmitter> set = emitters.computeIfAbsent(projectId, id -> new CopyOnWriteArraySet<>());
        if (set.size() >= MAX_PER_PROJECT) {
            throw new BusinessRuleException("Trop de participants connectés à ce planning poker");
        }
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        set.add(emitter);
        Runnable remove = () -> set.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        send(emitter, "ready", "{}");
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChange(PokerChangedEvent event) {
        Set<SseEmitter> set = emitters.get(event.projectId());
        if (set != null) {
            set.forEach(e -> send(e, "poker", "{\"roundId\":" + event.roundId() + "}"));
        }
    }

    /** Battement toutes les 25 s : garde la connexion ouverte à travers les proxys et purge les clients partis. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.values().forEach(set -> set.forEach(e -> send(e, "ping", "{}")));
    }

    int connections(Long projectId) {
        return emitters.getOrDefault(projectId, Set.of()).size();
    }

    private void send(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }
}
