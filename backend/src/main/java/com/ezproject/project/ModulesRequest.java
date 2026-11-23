package com.ezproject.project;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

public record ModulesRequest(@NotNull Set<ModuleType> modules) {
}
