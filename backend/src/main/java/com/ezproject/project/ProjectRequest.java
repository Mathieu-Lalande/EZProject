package com.ezproject.project;

import java.time.LocalDate;

import com.ezproject.access.Visibility;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{2,12}$", message = "2 à 12 caractères majuscules ou chiffres") String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 4000) String description,
        @Size(max = 150) String client,
        @NotNull ProjectType type,
        ProjectStatus status,
        @Size(max = 500) String statusReason,
        ProjectPriority priority,
        Health health,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur hexadécimale attendue") String color,
        Long ownerId,
        Long teamId,
        Visibility visibility,
        LocalDate startDate,
        LocalDate endDate,
        @Min(0) @Max(100000) Integer budgetDays) {
}
