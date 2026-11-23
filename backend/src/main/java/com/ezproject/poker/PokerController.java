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

    public PokerController(PokerService service) {
        this.service = service;
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
