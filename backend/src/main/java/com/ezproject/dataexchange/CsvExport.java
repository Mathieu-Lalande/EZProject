package com.ezproject.dataexchange;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Écriture CSV compatible Excel FR (séparateur ';', BOM UTF-8), protégée contre l'injection de formules
 * (OWASP "CSV Injection") : une cellule commençant par = + - @ tabulation ou retour chariot est préfixée par '.
 */
final class CsvExport {

    static final String BOM = "﻿";

    private CsvExport() {
    }

    static String line(List<?> cells) {
        return cells.stream().map(CsvExport::cell).collect(Collectors.joining(";")) + "\r\n";
    }

    static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        boolean number = value instanceof Number;
        if (!number && !s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
