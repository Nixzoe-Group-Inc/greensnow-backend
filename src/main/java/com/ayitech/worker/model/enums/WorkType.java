package com.ayitech.worker.model.enums;

public enum WorkType {
    ACTIVE_WORK,
    BREAK,
    TRAVEL,
    WAITING;

    public boolean isBillable() {
        return this == ACTIVE_WORK;
    }
}
