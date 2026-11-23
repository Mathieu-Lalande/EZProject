package com.ezproject.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TeamRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Couleur hexadécimale attendue") String color) {
}
