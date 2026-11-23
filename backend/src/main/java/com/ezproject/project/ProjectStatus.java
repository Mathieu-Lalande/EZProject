package com.ezproject.project;

public enum ProjectStatus {
    PLANNED, ACTIVE, ON_HOLD, COMPLETED, CANCELLED;

    public boolean isOpen() {
        return this == PLANNED || this == ACTIVE || this == ON_HOLD;
    }
}
