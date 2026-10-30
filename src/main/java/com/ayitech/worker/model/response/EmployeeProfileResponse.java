package com.ayitech.worker.model.response;

import com.ayitech.worker.model.enums.AvailabilityStatus;
import com.ayitech.worker.model.enums.EducationLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfileResponse {

    private String responseCode;
    private String responseMessage;

    private String profileId;
    private String userId;
    private String fullName;
    private String email;
    private String phoneNumber;

    private String headline;
    private String bio;
    private Integer yearsOfExperience;
    private EducationLevel educationLevel;
    private AvailabilityStatus availabilityStatus;

    private BigDecimal hourlyRate;
    private BigDecimal dailyRate;

    private String baseAddress;
    private BigDecimal baseLatitude;
    private BigDecimal baseLongitude;
    private Integer serviceRadiusKm;

    private boolean hasOwnTools;
    private boolean hasTransport;
    private boolean idVerified;
    private boolean backgroundChecked;

    private BigDecimal averageRating;
    private Integer totalRatings;
    private Integer jobsCompleted;
    private Integer jobsCancelled;
    private BigDecimal completionRate;

    // what the worker still has to fill in before employers see a whole profile. computed on
    // read rather than stored, so it cannot go stale against the fields it counts
    private Integer profileCompletionPercentage;

    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer totalSkills;
    private List<EmployeeSkillDetail> skills;
}
