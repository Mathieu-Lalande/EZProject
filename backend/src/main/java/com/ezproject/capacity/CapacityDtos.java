package com.ezproject.capacity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class CapacityDtos {

    private CapacityDtos() {
    }

    public record AbsenceRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotNull AbsenceKind kind,
                                 Long userId, @Size(max = 200) String comment) {
    }

    /** {@code kind} et {@code comment} sont masqués (null) pour les autres utilisateurs que l'intéressé et les admins. */
    public record AbsenceDto(Long id, Long userId, String userName, LocalDate startDate, LocalDate endDate,
                             AbsenceKind kind, String comment, boolean editable) {
    }

    public record AllocationRequest(@NotNull Long userId, @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                                    @NotNull @DecimalMin("0.5") @DecimalMax("7") BigDecimal daysPerWeek) {
    }

    public record AllocationDto(Long id, Long projectId, String projectCode, String projectName, String projectColor,
                                Long userId, String userName, LocalDate startDate, LocalDate endDate,
                                BigDecimal daysPerWeek) {
    }

    /** Une semaine pour une personne : capacité, absences, charge affectée, taux d'occupation. */
    public record WeekLoad(LocalDate weekStart, BigDecimal capacity, BigDecimal absent, BigDecimal available,
                           BigDecimal allocated, Integer loadPct) {
    }

    public record PersonCapacity(Long userId, String displayName, String jobTitle, BigDecimal daysPerWeek,
                                 List<WeekLoad> weeks, List<AllocationDto> allocations) {
    }

    public record CapacityDto(List<LocalDate> weeks, List<PersonCapacity> people) {
    }
}
