package com.ezproject.risk;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class RiskDtos {

    private RiskDtos() {
    }

    public record RiskRequest(@NotBlank @Size(max = 200) String title, @Size(max = 2000) String description,
                              @Min(1) @Max(4) int probability, @Min(1) @Max(4) int impact, RiskStatus status,
                              Long ownerId, @Size(max = 2000) String mitigation, LocalDate dueDate) {
    }

    public record RiskDto(Long id, String title, String description, int probability, int impact, int score,
                          boolean critical, RiskStatus status, Long ownerId, String ownerName, String mitigation,
                          LocalDate dueDate, Instant updatedAt) {

        static RiskDto from(Risk r) {
            return new RiskDto(r.getId(), r.getTitle(), r.getDescription(), r.getProbability(), r.getImpact(),
                    r.score(), r.isCritical(), r.getStatus(), r.getOwner() == null ? null : r.getOwner().getId(),
                    r.getOwner() == null ? null : r.getOwner().getDisplayName(), r.getMitigation(), r.getDueDate(),
                    r.getUpdatedAt());
        }
    }

    public record DecisionRequest(@NotBlank @Size(max = 200) String title, @Size(max = 2000) String description,
                                  LocalDate decidedOn, @Size(max = 150) String decidedBy, DecisionStatus status) {
    }

    public record DecisionDto(Long id, String title, String description, LocalDate decidedOn, String decidedBy,
                              DecisionStatus status, Instant createdAt) {

        static DecisionDto from(Decision d) {
            return new DecisionDto(d.getId(), d.getTitle(), d.getDescription(), d.getDecidedOn(), d.getDecidedBy(),
                    d.getStatus(), d.getCreatedAt());
        }
    }
}
