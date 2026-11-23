package com.ezproject.dataexchange;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Conversion des valeurs CSV : codes techniques ou libellés français usuels. */
final class ValueParser {

    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ValueParser() {
    }

    static <E extends Enum<E>> E enumValue(Class<E> type, String raw, Map<String, E> synonyms, String field) {
        if (raw == null) {
            return null;
        }
        String key = CsvTable.normalize(raw);
        E synonym = synonyms.get(key);
        if (synonym != null) {
            return synonym;
        }
        for (E e : type.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(key)) {
                return e;
            }
        }
        throw new IllegalArgumentException(field + " inconnu : « " + raw + " »");
    }

    static LocalDate date(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return raw.contains("/") ? LocalDate.parse(raw, FR) : LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " : date invalide « " + raw + " » (jj/mm/aaaa ou aaaa-mm-jj)");
        }
    }

    static Integer integer(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(raw.replace(" ", "").replace(",", ".")));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " : nombre invalide « " + raw + " »");
        }
    }
}
