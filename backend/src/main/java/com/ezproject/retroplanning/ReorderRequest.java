package com.ezproject.retroplanning;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record ReorderRequest(@NotNull List<Long> phaseIds) {
}
