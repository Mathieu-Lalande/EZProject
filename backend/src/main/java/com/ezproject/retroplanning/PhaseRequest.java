package com.ezproject.retroplanning;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PhaseRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull PhaseKind kind,
        @Min(0) @Max(1000) int durationDays,
        boolean done) {
}
