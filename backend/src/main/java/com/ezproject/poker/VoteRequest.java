package com.ezproject.poker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoteRequest(@NotBlank @Size(max = 10) String card) {
}
