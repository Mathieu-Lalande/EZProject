package com.ezproject.time;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contrats d'API du module temps passé / budget. */
public final class TimeDtos {

    private TimeDtos() {
    }

    public record EntryRequest(
            @NotNull LocalDate date,
            @NotNull @DecimalMin(value = "0.125", message = "1 heure minimum (0,125 j)") @DecimalMax("1") BigDecimal days,
            Long taskId,
            Long userId,
            @Size(max = 300) String comment) {
    }

    public record EntryDto(Long id, LocalDate date, BigDecimal days, Long userId, String userName, Long taskId,
                           String taskTitle, String comment, boolean editable) {
    }

    public record UserDaysDto(Long userId, String name, BigDecimal days) {
    }

    public record MonthDaysDto(String month, BigDecimal days) {
    }

    /**
     * Synthèse budgétaire. {@code projectedExhaustion} : date à laquelle le budget sera consommé au rythme
     * des 4 dernières semaines (null si pas de budget ou pas d'activité).
     */
    public record SummaryDto(Integer budgetDays, BigDecimal consumedDays, BigDecimal remainingDays,
                             Integer consumptionPct, BigDecimal weeklyBurn, LocalDate projectedExhaustion,
                             boolean overBudget, List<UserDaysDto> byUser, List<MonthDaysDto> byMonth) {
    }

    public record WeekCell(@NotNull Long projectId, @NotNull LocalDate date,
                           @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal days) {
    }

    public record WeekRequest(@NotNull LocalDate weekStart, @NotNull @Size(max = 200) List<WeekCell> cells) {
    }

    public record WeekProject(Long projectId, String code, String name, String color) {
    }

    /** Feuille de temps hebdomadaire de l'utilisateur courant. */
    public record WeekDto(LocalDate weekStart, List<LocalDate> days, List<Boolean> workingDays,
                          List<WeekProject> projects, List<WeekCell> cells, List<EntryDto> taskEntries) {
    }
}
