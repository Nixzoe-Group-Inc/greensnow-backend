package com.ayitech.worker.model.request;

import com.ayitech.worker.model.enums.ExperienceLevel;
import com.ayitech.worker.model.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// there is no status field. a job's status is a transition the platform makes - an employer
// posts a draft, submits it, and an approver moves it on. an employer who could send APPROVED
// would approve their own job, which is the one thing the whole approval step exists to stop
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequest {

    private String title;
    private String description;
    private JobType jobType;
    private ExperienceLevel experienceLevel;

    private String siteAddress;
    private BigDecimal siteLatitude;
    private BigDecimal siteLongitude;
    private Integer siteRadiusMeters;

    private BigDecimal hourlyRate;
    private BigDecimal dailyRate;
    private BigDecimal totalBudget;
    private String currency;

    private LocalDate scheduledStartDate;
    private LocalDate scheduledEndDate;
    private Integer estimatedDurationHours;
    private Integer maxDailyHours;
    private Integer extensionsAllowed;
    private Integer workersNeeded;

    private Boolean toolsProvided;
    private Boolean transportProvided;
    private Boolean urgent;

    // sent whole or not at all. a list that arrives replaces what the job carried, because a
    // per-requirement patch would need ids the employer has no reason to hold
    private List<JobRequirementRequest> requirements;
}
