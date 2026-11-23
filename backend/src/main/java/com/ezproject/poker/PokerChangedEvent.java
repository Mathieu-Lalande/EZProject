package com.ezproject.poker;

/** Un tour de poker a changé (vote, révélation, clôture…) : publié après validation de la transaction. */
public record PokerChangedEvent(Long projectId, Long roundId) {
}
