package com.ezproject.poker;

import java.util.List;
import java.util.OptionalInt;

/** Jeu de cartes Fibonacci modifié, standard du planning poker. */
public final class PokerDeck {

    public static final List<String> CARDS = List.of("0", "1", "2", "3", "5", "8", "13", "21", "40", "?", "☕");

    private PokerDeck() {
    }

    public static boolean isValid(String card) {
        return CARDS.contains(card);
    }

    /** Valeur numérique d'une carte, vide pour "?" et "☕". */
    public static OptionalInt numericValue(String card) {
        try {
            return OptionalInt.of(Integer.parseInt(card));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
