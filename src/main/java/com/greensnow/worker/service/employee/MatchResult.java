package com.greensnow.worker.service.employee;

import com.greensnow.worker.model.response.JobRequirementDetail;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

// how one job sits against one worker. never stored on the job, because the same job scores
// differently for every worker who looks at it - what IS stored is a copy taken on the day an
// application is made, which is what the employer is answering
@Getter
@Builder
public class MatchResult {

    private final BigDecimal matchScore;
    private final BigDecimal skillScore;
    private final BigDecimal experienceScore;
    private final BigDecimal locationScore;
    private final BigDecimal educationScore;
    private final BigDecimal distanceKm;

    private final boolean eligible;
    private final String ineligibilityReason;

    private final List<JobRequirementDetail> requirements;
}
