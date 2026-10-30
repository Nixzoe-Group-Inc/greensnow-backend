package com.greensnow.worker.model.response;

import com.greensnow.worker.model.enums.CompanySize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployerProfileResponse {

    private String responseCode;
    private String responseMessage;

    private String profileId;
    private String userId;
    private String contactName;
    private String email;
    private String phoneNumber;

    private String companyName;
    private String registrationNumber;
    private CompanySize companySize;
    private String industry;
    private String website;
    private String description;

    private String address;
    private String city;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // read only to the employer. verified is the platform's badge and autoApproveJobs is the
    // switch that lets a trusted company's jobs skip the queue - neither is theirs to set
    private boolean verified;
    private boolean autoApproveJobs;

    private BigDecimal averageRating;
    private Integer totalRatings;
    private Integer jobsPosted;
    private Integer jobsCompleted;

    // what the company still has to fill in before workers see a whole profile. computed on
    // read rather than stored, so it cannot go stale against the fields it counts
    private Integer profileCompletionPercentage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
