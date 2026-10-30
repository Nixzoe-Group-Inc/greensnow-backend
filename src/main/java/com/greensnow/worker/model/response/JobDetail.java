package com.greensnow.worker.model.response;

import com.greensnow.worker.model.enums.ExperienceLevel;
import com.greensnow.worker.model.enums.JobStatus;
import com.greensnow.worker.model.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDetail {

    private String jobId;
    private String employerProfileId;
    private String title;
    private String description;
    private JobType jobType;
    private ExperienceLevel experienceLevel;
    private JobStatus status;

    private String siteAddress;
    private BigDecimal siteLatitude;
    private BigDecimal siteLongitude;

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
    private Integer workersAssigned;
    private Integer vacancies;

    private boolean toolsProvided;
    private boolean transportProvided;
    private boolean urgent;

    private LocalDateTime publishedAt;
    private Integer applicationCount;

    // how this job sits against the worker who asked for it. null on a listing nobody is
    // scored against; never stored on the job itself, because it is different per worker
    private BigDecimal distanceKm;
    private BigDecimal matchScore;
    private BigDecimal skillScore;
    private BigDecimal experienceScore;
    private BigDecimal locationScore;
    private BigDecimal educationScore;
    private Boolean eligible;
    private String ineligibilityReason;
    private Boolean alreadyApplied;

    private List<JobRequirementDetail> requirements;
}
