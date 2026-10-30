package com.greensnow.worker.model.enums;

public enum ExecutionStatus {
    NOT_STARTED,
    TRAVELING_TO_SITE,
    ON_SITE,
    IN_PROGRESS,
    PAUSED,
    EXTENDED,
    COMPLETED,
    CANCELLED;

    public boolean isLive() {
        return this == TRAVELING_TO_SITE
                || this == ON_SITE
                || this == IN_PROGRESS
                || this == PAUSED
                || this == EXTENDED;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
