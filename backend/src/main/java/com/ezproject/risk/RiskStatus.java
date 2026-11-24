package com.ezproject.risk;

public enum RiskStatus {
    OPEN, MITIGATING, OCCURRED, CLOSED;

    public boolean isActive() {
        return this == OPEN || this == MITIGATING;
    }
}
