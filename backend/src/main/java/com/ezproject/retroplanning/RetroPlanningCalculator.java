package com.ezproject.retroplanning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ezproject.calendar.HolidayCalendar;

/**
 * Calcul de rétro-planning : on part de l'échéance et on remonte le temps, phase par phase,
 * en jours ouvrés (hors week-ends et jours fériés du calendrier choisi). Sans état, donc trivialement testable.
 */
@Component
public class RetroPlanningCalculator {

    public record PhaseInput(Long id, String name, PhaseKind kind, int durationDays, boolean done) {
    }

    public record ScheduledPhase(Long id, String name, PhaseKind kind, int durationDays, boolean done,
                                 LocalDate start, LocalDate end) {
    }

    /**
     * @param deadline échéance (la dernière phase se termine au plus tard ce jour-là)
     * @param phases   phases dans l'ordre chronologique
     * @return phases datées, dans l'ordre chronologique
     */
    public List<ScheduledPhase> scheduleBackward(LocalDate deadline, List<PhaseInput> phases) {
        return scheduleBackward(deadline, phases, HolidayCalendar.NONE);
    }

    public List<ScheduledPhase> scheduleBackward(LocalDate deadline, List<PhaseInput> phases, HolidayCalendar calendar) {
        List<ScheduledPhase> result = new ArrayList<>(phases.size());
        LocalDate cursor = previousOrSameWorkingDay(deadline, calendar);
        for (int i = phases.size() - 1; i >= 0; i--) {
            PhaseInput p = phases.get(i);
            if (p.kind() == PhaseKind.MILESTONE) {
                result.add(new ScheduledPhase(p.id(), p.name(), p.kind(), 0, p.done(), cursor, cursor));
                continue;
            }
            LocalDate end = cursor;
            LocalDate start = minusWorkingDays(end, p.durationDays() - 1, calendar);
            result.add(new ScheduledPhase(p.id(), p.name(), p.kind(), p.durationDays(), p.done(), start, end));
            cursor = minusWorkingDays(start, 1, calendar);
        }
        Collections.reverse(result);
        return result;
    }

    static LocalDate previousOrSameWorkingDay(LocalDate date, HolidayCalendar calendar) {
        LocalDate d = date;
        while (!calendar.isWorkingDay(d)) {
            d = d.minusDays(1);
        }
        return d;
    }

    static LocalDate minusWorkingDays(LocalDate from, int workingDays, HolidayCalendar calendar) {
        LocalDate d = previousOrSameWorkingDay(from, calendar);
        int remaining = workingDays;
        while (remaining > 0) {
            d = d.minusDays(1);
            if (calendar.isWorkingDay(d)) {
                remaining--;
            }
        }
        return d;
    }
}
