package com.ezproject.project;

import java.util.EnumSet;
import java.util.Set;

/** Nature du projet : détermine les modules activés par défaut. */
public enum ProjectType {
    DEVELOPMENT(EnumSet.of(ModuleType.BOARD, ModuleType.TODOS, ModuleType.RETRO_PLANNING, ModuleType.POKER,
            ModuleType.TIME_TRACKING, ModuleType.RISKS)),
    CONSULTING(EnumSet.of(ModuleType.BOARD, ModuleType.TODOS, ModuleType.RETRO_PLANNING, ModuleType.TIME_TRACKING,
            ModuleType.RISKS)),
    INTERNAL(EnumSet.of(ModuleType.BOARD, ModuleType.TODOS));

    private final Set<ModuleType> defaultModules;

    ProjectType(Set<ModuleType> defaultModules) {
        this.defaultModules = defaultModules;
    }

    public Set<ModuleType> defaultModules() {
        return EnumSet.copyOf(defaultModules);
    }
}
