package com.greensnow.worker.model.response;

import com.greensnow.worker.model.enums.ApplicationStatus;
import com.greensnow.worker.model.enums.AvailabilityStatus;
import com.greensnow.worker.model.enums.EducationLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// the employer's view of one application. the worker's side of the same row is
// JobApplicationDetail, which names the job - here the job is already known and what the
// employer is reading is the person
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicantDetail {

    private String applicationId;
    private String jobId;
    private ApplicationStatus status;

    private String employeeProfileId;
    private String workerName;
    private String headline;
    private String bio;
    private Integer yearsOfExperience;
    private EducationLevel educationLevel;
    private AvailabilityStatus availabilityStatus;

    // the worker's own contact details, released only once the application is ACCEPTED. a
    // shortlist is not a hire, and an employer who could read a phone number off every
    // applicant would have no reason to accept anybody through the platform at all
    private String email;
    private String phoneNumber;

    private boolean hasOwnTools;
    private boolean hasTransport;
    private boolean idVerified;
    private boolean backgroundChecked;

    private BigDecimal averageRating;
    private Integer totalRatings;
    private Integer jobsCompleted;
    private BigDecimal completionRate;

    // frozen as they were on the day the worker applied. the profile moves underneath them,
    // so recomputing now would not answer the question the application was accepted on
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

    private List<EmployeeSkillDetail> skills;
}
