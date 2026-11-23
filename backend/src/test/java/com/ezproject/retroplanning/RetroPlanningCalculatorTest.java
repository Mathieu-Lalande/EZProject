package com.ezproject.retroplanning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ezproject.retroplanning.RetroPlanningCalculator.PhaseInput;
import com.ezproject.retroplanning.RetroPlanningCalculator.ScheduledPhase;

class RetroPlanningCalculatorTest {

    private final RetroPlanningCalculator calculator = new RetroPlanningCalculator();

    @Test
    void schedulesPhasesBackwardFromDeadlineInWorkingDays() {
        // Vendredi 2026-12-18
        LocalDate deadline = LocalDate.of(2026, 12, 18);
        List<ScheduledPhase> plan = calculator.scheduleBackward(deadline, List.of(
                new PhaseInput(1L, "Dev", PhaseKind.PHASE, 10, false),
                new PhaseInput(2L, "Recette", PhaseKind.PHASE, 5, false),
                new PhaseInput(3L, "MEP", PhaseKind.MILESTONE, 0, false)));

        assertThat(plan).extracting(ScheduledPhase::name).containsExactly("Dev", "Recette", "MEP");
        // Jalon à l'échéance
        assertThat(plan.get(2).start()).isEqualTo(deadline);
        // Recette : 5 jours ouvrés du lundi 14 au vendredi 18
        assertThat(plan.get(1).start()).isEqualTo(LocalDate.of(2026, 12, 14));
        assertThat(plan.get(1).end()).isEqualTo(deadline);
        // Dev : 10 jours ouvrés finissant le vendredi 11, donc démarrage le lundi 30 novembre
        assertThat(plan.get(0).end()).isEqualTo(LocalDate.of(2026, 12, 11));
        assertThat(plan.get(0).start()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void deadlineOnWeekendIsMovedToPreviousFriday() {
        LocalDate sunday = LocalDate.of(2026, 12, 20);
        List<ScheduledPhase> plan = calculator.scheduleBackward(sunday,
                List.of(new PhaseInput(1L, "Livraison", PhaseKind.MILESTONE, 0, false)));

        assertThat(plan.getFirst().start()).isEqualTo(LocalDate.of(2026, 12, 18));
    }

    @Test
    void emptyPlanReturnsEmptyList() {
        assertThat(calculator.scheduleBackward(LocalDate.of(2026, 12, 18), List.of())).isEmpty();
    }
}
