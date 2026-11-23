package com.ezproject.dataexchange;

import java.util.List;

/**
 * Résultat d'un import. En cas d'erreur, rien n'est enregistré (tout ou rien) ;
 * en simulation (dryRun), le fichier est entièrement vérifié sans rien enregistrer.
 */
public record ImportReport(boolean dryRun, boolean applied, int rows, int created, int updated, int teamsCreated,
                           List<RowError> errors, List<String> preview) {

    public record RowError(long line, String message) {
    }
}
