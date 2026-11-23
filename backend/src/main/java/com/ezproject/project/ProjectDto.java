package com.ezproject.project;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import com.ezproject.user.UserDto;

public record ProjectDto(
        Long id,
        String code,
        String name,
        String description,
        String client,
        ProjectType type,
        ProjectStatus status,
        Health health,
        String color,
        UserDto owner,
        LocalDate startDate,
        LocalDate endDate,
        Integer budgetDays,
        Set<ModuleType> modules) {

    public static ProjectDto from(Project p) {
        return new ProjectDto(p.getId(), p.getCode(), p.getName(), p.getDescription(), p.getClient(), p.getType(),
                p.getStatus(), p.getHealth(), p.getColor(), p.getOwner() == null ? null : UserDto.from(p.getOwner()),
                p.getStartDate(), p.getEndDate(), p.getBudgetDays(), new TreeSet<>(p.getModules()));
    }
}
