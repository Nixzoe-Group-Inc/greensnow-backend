package com.greensnow.worker.model.entity;

import com.greensnow.worker.model.enums.CompanySize;
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

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employer_profiles")
public class EmployerProfile extends BaseEntity {

    private String userId;

    private String companyName;

    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    private CompanySize companySize;

    private String industry;

    private String website;

    private String description;

    private String address;

    private String city;

    private String country;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private boolean verified;

    private boolean autoApproveJobs;

    private BigDecimal averageRating;

    private Integer totalRatings;

    private Integer jobsPosted;

    private Integer jobsCompleted;

}
