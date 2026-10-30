package com.greensnow.worker.model.request;

import com.greensnow.worker.model.enums.CompanySize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// verified, autoApproveJobs, averageRating, totalRatings, jobsPosted and jobsCompleted are
// deliberately absent. they are the platform's to set - an employer who could send them would
// mark their own company verified and let their own jobs skip approval
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployerProfileRequest {

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
}
