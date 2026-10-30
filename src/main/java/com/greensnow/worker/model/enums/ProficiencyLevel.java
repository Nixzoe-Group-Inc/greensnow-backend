package com.greensnow.worker.model.enums;

import lombok.Getter;

@Getter
public enum ProficiencyLevel {

    BEGINNER(1),
    INTERMEDIATE(2),
    ADVANCED(3),
    EXPERT(4);

    private final int rank;

    ProficiencyLevel(int rank) {
        this.rank = rank;
    }

    public boolean meets(ProficiencyLevel required) {
        return required == null || this.rank >= required.rank;
    }
}
