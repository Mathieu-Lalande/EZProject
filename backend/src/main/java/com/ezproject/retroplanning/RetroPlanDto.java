package com.ezproject.retroplanning;

import java.time.LocalDate;
import java.util.List;

/**
 * @param deadline          échéance du projet (date de fin)
 * @param computedStart     date à laquelle il faut démarrer pour tenir l'échéance
 * @param startConflict     vrai si le démarrage calculé précède la date de début prévue du projet
 */
public record RetroPlanDto(
        LocalDate deadline,
        LocalDate projectStart,
        LocalDate computedStart,
        boolean startConflict,
        int totalWorkingDays,
        List<PhaseDto> phases) {

    public record PhaseDto(Long id, String name, PhaseKind kind, int durationDays, boolean done,
                           LocalDate start, LocalDate end, PhaseState state) {
    }
}
