package com.ayitech.worker.model.response;

import com.ayitech.worker.model.enums.ApplicationStatus;
import com.ayitech.worker.model.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationDetail {

    private String applicationId;
    private String jobId;
    private String jobTitle;
    private JobStatus jobStatus;
    private String siteAddress;
    private LocalDate scheduledStartDate;
    private String employerProfileId;

    private ApplicationStatus status;

    // the scores the application was accepted or refused on, frozen as they were on the day.
    // the profile moves underneath them, so recomputing later would not answer the same question
    private BigDecimal matchScore;
    private BigDecimal skillScore;
    private BigDecimal experienceScore;
    private BigDecimal locationScore;
    private BigDecimal educationScore;
    private BigDecimal distanceKm;

    private String coverNote;
    private BigDecimal proposedRate;
    private LocalDateTime appliedAt;
    private LocalDateTime respondedAt;
    private String rejectionReason;
}
