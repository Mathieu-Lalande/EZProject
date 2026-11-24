package com.ezproject.poker;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class PokerController {

    private final PokerService service;
    private final PokerEventHub hub;
    private final com.ezproject.access.ProjectAccessService access;

    public PokerController(PokerService service, PokerEventHub hub, com.ezproject.access.ProjectAccessService access) {
        this.service = service;
        this.hub = hub;
        this.access = access;
    }

    /** Flux temps réel des changements du planning poker du projet (Server-Sent Events). */
    @GetMapping(path = "/api/projects/{projectId}/poker/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(@PathVariable Long projectId) {
        access.require(projectId, com.ezproject.access.Permission.POKER_VIEW);
        return hub.subscribe(projectId);
    }

    @GetMapping("/api/poker/deck")
    public List<String> deck() {
        return PokerDeck.CARDS;
    }

    @GetMapping("/api/projects/{projectId}/poker/rounds")
    public List<RoundDto> rounds(@PathVariable Long projectId) {
        return service.findByProject(projectId);
    }

    @PostMapping("/api/projects/{projectId}/poker/rounds")
    @ResponseStatus(HttpStatus.CREATED)
    public RoundDto start(@PathVariable Long projectId, @Valid @RequestBody NewRoundRequest request) {
        return service.start(projectId, request);
    }

    @GetMapping("/api/poker/rounds/{roundId}")
    public RoundDto get(@PathVariable Long roundId) {
        return service.get(roundId);
    }

    @PostMapping("/api/poker/rounds/{roundId}/vote")
    public RoundDto vote(@PathVariable Long roundId, @Valid @RequestBody VoteRequest request) {
        return service.vote(roundId, request);
    }

    @PostMapping("/api/poker/rounds/{roundId}/reveal")
    public RoundDto reveal(@PathVariable Long roundId) {
        return service.reveal(roundId);
    }

    @PostMapping("/api/poker/rounds/{roundId}/restart")
    public RoundDto restart(@PathVariable Long roundId) {
        return service.restart(roundId);
    }

    @PostMapping("/api/poker/rounds/{roundId}/close")
    public RoundDto close(@PathVariable Long roundId, @Valid @RequestBody VoteRequest request) {
        return service.close(roundId, request);
    }
}
