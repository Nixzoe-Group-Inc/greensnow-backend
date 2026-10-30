package com.ayitech.worker.model.entity;

import com.ayitech.worker.model.enums.AvailabilityStatus;
import com.ayitech.worker.model.enums.EducationLevel;
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
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee_profiles")
public class EmployeeProfile extends BaseEntity {

    private String userId;

    private String headline;

    private String bio;

    private Integer yearsOfExperience;

    @Enumerated(EnumType.STRING)
    private EducationLevel educationLevel;

    @Enumerated(EnumType.STRING)
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

    private LocalDateTime lastActiveAt;

}
