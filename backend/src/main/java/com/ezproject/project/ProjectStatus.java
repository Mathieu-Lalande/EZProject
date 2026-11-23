package com.ezproject.project;

public enum ProjectStatus {
    PLANNED, ACTIVE, ON_HOLD, COMPLETED, CANCELLED;

    public boolean isOpen() {
        return this == PLANNED || this == ACTIVE || this == ON_HOLD;
    }

    /** Une mise en pause ou une annulation doit être justifiée (traçabilité des arbitrages). */
    public boolean requiresReason() {
        return this == ON_HOLD || this == CANCELLED;
    }
}
