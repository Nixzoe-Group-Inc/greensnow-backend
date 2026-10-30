package com.ayitech.worker.model.enums;

import lombok.Getter;

@Getter
public enum ExperienceLevel {

    ENTRY(0, 2),
    MID(2, 6),
    SENIOR(6, 12),
    LEAD(12, Integer.MAX_VALUE);

    private final int minYears;
    private final int maxYears;

    ExperienceLevel(int minYears, int maxYears) {
        this.minYears = minYears;
        this.maxYears = maxYears;
    }

    public boolean covers(int years) {
        return years >= minYears && years < maxYears;
    }

    public static ExperienceLevel forYears(int years) {
        for (ExperienceLevel level : values()) {
            if (level.covers(years)) {
                return level;
            }
        }
        return LEAD;
    }
}
