package com.ayitech.worker.model.entity;

import com.ayitech.worker.model.enums.ExperienceLevel;
import com.ayitech.worker.model.enums.JobStatus;
import com.ayitech.worker.model.enums.JobType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {

    private String employerProfileId;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

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

    private Integer workersAssigned;

    private boolean toolsProvided;

    private boolean transportProvided;

    private boolean urgent;

    private LocalDateTime submittedForApprovalAt;

    private LocalDateTime approvedAt;

    private LocalDateTime publishedAt;

    private LocalDateTime closedAt;

    private Integer viewCount;

    private Integer applicationCount;

}
