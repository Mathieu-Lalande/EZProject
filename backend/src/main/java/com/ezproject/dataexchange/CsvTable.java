package com.ezproject.dataexchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.ezproject.common.BusinessRuleException;

/**
 * Lecture tolérante d'un CSV (export Excel FR avec ';' ou CSV standard avec ',') :
 * BOM UTF-8 retiré, en-têtes normalisés (minuscules, sans accents, espaces -> '_').
 */
final class CsvTable {

    static final int MAX_ROWS = 5000;

    record Row(long line, Map<String, String> values) {

        String get(String... aliases) {
            for (String alias : aliases) {
                String v = values.get(alias);
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
            return null;
        }
    }

    private CsvTable() {
    }

    static List<Row> parse(InputStream in) {
        String content;
        try {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessRuleException("Fichier illisible");
        }
        if (content.startsWith("﻿")) {
            content = content.substring(1);
        }
        String firstLine = content.lines().findFirst().orElse("");
        char delimiter = count(firstLine, ';') > count(firstLine, ',') ? ';' : ',';

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
        List<Row> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new StringReader(content), format)) {
            List<String> headers = parser.getHeaderNames().stream().map(CsvTable::normalize).toList();
            for (CSVRecord record : parser) {
                if (rows.size() >= MAX_ROWS) {
                    throw new BusinessRuleException("Fichier trop volumineux : " + MAX_ROWS + " lignes maximum");
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < headers.size() && i < record.size(); i++) {
                    values.put(headers.get(i), record.get(i));
                }
                rows.add(new Row(record.getRecordNumber() + 1, values));
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            throw new BusinessRuleException("CSV invalide : vérifiez l'en-tête et les guillemets");
        }
        return rows;
    }

    static String normalize(String header) {
        return Normalizer.normalize(header, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    private static long count(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }
}
