package com.ezproject.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ezproject.retroplanning.PhaseKind;
import com.ezproject.retroplanning.RetroPlanningCalculator;

class HolidayCalendarTest {

    @Test
    void computesEasterBasedFrenchHolidays() {
        assertThat(HolidayCalendar.easterSunday(2026)).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(HolidayCalendar.easterSunday(2027)).isEqualTo(LocalDate.of(2027, 3, 28));
        assertThat(HolidayCalendar.frenchHolidays(2026)).contains(
                LocalDate.of(2026, 4, 6),   // lundi de Pâques
                LocalDate.of(2026, 5, 14),  // Ascension
                LocalDate.of(2026, 5, 25),  // lundi de Pentecôte
                LocalDate.of(2026, 7, 14), LocalDate.of(2026, 12, 25)).hasSize(11);
    }

    @Test
    void workingDaysExcludeWeekendsAndHolidaysOnlyForFrenchCalendar() {
        LocalDate christmas = LocalDate.of(2026, 12, 25); // vendredi
        assertThat(HolidayCalendar.FR.isWorkingDay(christmas)).isFalse();
        assertThat(HolidayCalendar.NONE.isWorkingDay(christmas)).isTrue();
        // Semaine du 21 au 27 décembre 2026 : 4 jours ouvrés en France
        assertThat(HolidayCalendar.FR.workingDaysBetween(LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 27))).isEqualTo(4);
    }

    @Test
    void retroPlanningSkipsHolidays() {
        var plan = new RetroPlanningCalculator().scheduleBackward(LocalDate.of(2026, 12, 25),
                List.of(new RetroPlanningCalculator.PhaseInput(1L, "Recette", PhaseKind.PHASE, 3, false),
                        new RetroPlanningCalculator.PhaseInput(2L, "MEP", PhaseKind.MILESTONE, 0, false)),
                HolidayCalendar.FR);
        assertThat(plan.get(1).start()).isEqualTo(LocalDate.of(2026, 12, 24));
        assertThat(plan.get(0).start()).isEqualTo(LocalDate.of(2026, 12, 22));
    }
}
