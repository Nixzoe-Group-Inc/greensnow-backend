package com.greensnow.worker.model.entity;

import com.greensnow.worker.model.enums.ApplicationStatus;
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
@Table(name = "job_applications")
public class JobApplication extends BaseEntity {

    private String jobId;

    private String employeeProfileId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

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
