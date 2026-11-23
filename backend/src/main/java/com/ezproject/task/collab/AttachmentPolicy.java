package com.ezproject.task.collab;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import com.ezproject.common.BusinessRuleException;

/**
 * Contrôle des fichiers joints (OWASP « File Upload ») :
 * liste blanche d'extensions, type MIME déduit de l'extension (jamais celui annoncé par le client),
 * vérification de la signature binaire, nom de fichier assaini, taille bornée.
 * Exclus volontairement : SVG, HTML, scripts et exécutables (risque XSS / exécution).
 */
final class AttachmentPolicy {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final int MAX_PER_TASK = 20;

    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("md", "text/markdown"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet"),
            Map.entry("zip", "application/zip"));

    record Checked(String filename, String contentType) {
    }

    private AttachmentPolicy() {
    }

    static Checked check(String originalName, byte[] content) {
        if (content.length == 0) {
            throw new BusinessRuleException("Fichier vide");
        }
        if (content.length > MAX_BYTES) {
            throw new BusinessRuleException("Fichier trop volumineux (5 Mo maximum)");
        }
        String name = sanitize(originalName);
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        String type = TYPES.get(ext);
        if (type == null) {
            throw new BusinessRuleException("Type de fichier non autorisé. Acceptés : " + String.join(", ", TYPES.keySet().stream().sorted().toList()));
        }
        if (!signatureMatches(ext, content)) {
            throw new BusinessRuleException("Le contenu du fichier ne correspond pas à son extension");
        }
        return new Checked(name, type);
    }

    static String sanitize(String original) {
        String name = original == null ? "fichier" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}\"<>:|?*]", "_").trim();
        if (name.isEmpty() || name.startsWith(".")) {
            name = "fichier" + name;
        }
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }

    private static boolean signatureMatches(String ext, byte[] c) {
        return switch (ext) {
            case "pdf" -> startsWith(c, "%PDF".getBytes(StandardCharsets.US_ASCII));
            case "png" -> startsWith(c, new byte[] {(byte) 0x89, 'P', 'N', 'G'});
            case "jpg", "jpeg" -> startsWith(c, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "gif" -> startsWith(c, "GIF8".getBytes(StandardCharsets.US_ASCII));
            case "webp" -> startsWith(c, "RIFF".getBytes(StandardCharsets.US_ASCII)) && c.length > 12
                    && new String(c, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            case "docx", "xlsx", "pptx", "odt", "ods", "zip" -> startsWith(c, new byte[] {'P', 'K', 3, 4});
            // Texte : pas d'octet nul (fichier binaire déguisé)
            default -> {
                for (int i = 0; i < Math.min(c.length, 8192); i++) {
                    if (c[i] == 0) {
                        yield false;
                    }
                }
                yield true;
            }
        };
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        return content.length >= prefix.length && Arrays.equals(content, 0, prefix.length, prefix, 0, prefix.length);
    }
}
