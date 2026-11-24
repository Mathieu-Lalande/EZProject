package com.ezproject.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calendrier des jours non travaillés d'une organisation : week-ends, plus les jours fériés du pays choisi.
 * FR : jours fériés légaux de France métropolitaine (Code du travail, art. L3133-1), dont les fêtes mobiles
 * calculées à partir de Pâques.
 */
public enum HolidayCalendar {
    NONE, FR;

    private static final Map<Integer, Set<LocalDate>> FR_CACHE = new ConcurrentHashMap<>();
    private static final Set<MonthDay> FR_FIXED = Set.of(
            MonthDay.of(1, 1), MonthDay.of(5, 1), MonthDay.of(5, 8), MonthDay.of(7, 14),
            MonthDay.of(8, 15), MonthDay.of(11, 1), MonthDay.of(11, 11), MonthDay.of(12, 25));

    public boolean isWorkingDay(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) {
            return false;
        }
        return this != FR || !frenchHolidays(date.getYear()).contains(date);
    }

    /** Nombre de jours ouvrés entre deux dates incluses. */
    public int workingDaysBetween(LocalDate from, LocalDate to) {
        int count = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            if (isWorkingDay(d)) {
                count++;
            }
        }
        return count;
    }

    static Set<LocalDate> frenchHolidays(int year) {
        return FR_CACHE.computeIfAbsent(year, y -> {
            Set<LocalDate> days = new HashSet<>();
            FR_FIXED.forEach(md -> days.add(md.atYear(y)));
            LocalDate easter = easterSunday(y);
            days.add(easter.plusDays(1));   // Lundi de Pâques
            days.add(easter.plusDays(39));  // Ascension
            days.add(easter.plusDays(50));  // Lundi de Pentecôte
            return Set.copyOf(days);
        });
    }

    /** Dimanche de Pâques (calendrier grégorien, algorithme de Meeus/Jones/Butcher). */
    static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
