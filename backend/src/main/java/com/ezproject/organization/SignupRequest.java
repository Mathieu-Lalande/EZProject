package com.ezproject.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(min = 2, max = 150) String organizationName,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 12, max = 128, message = "12 caractères minimum") String password) {
}
