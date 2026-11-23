package com.ezproject.poker;

import java.util.List;

/**
 * Vue d'un tour. Tant que le tour n'est pas révélé, les cartes des autres participants sont masquées
 * (card = null) : seul le fait d'avoir voté est exposé, pour ne pas biaiser l'estimation.
 */
public record RoundDto(
        Long id,
        Long taskId,
        String storyTitle,
        RoundStatus status,
        String finalEstimate,
        String myCard,
        List<VoteView> votes,
        Stats stats) {

    public record VoteView(Long userId, String displayName, String card) {
    }

    /** Statistiques disponibles une fois les cartes révélées. */
    public record Stats(Double average, String suggested, boolean consensus) {
    }
}
