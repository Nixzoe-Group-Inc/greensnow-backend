package com.greensnow.worker.model.entity;

import com.greensnow.worker.model.enums.ExecutionStatus;
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
@Table(name = "job_executions")
public class JobExecution extends BaseEntity {

    private String jobId;

    private String employeeProfileId;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    private LocalDateTime acceptedAt;

    private LocalDateTime travelStartedAt;

    private LocalDateTime arrivedAt;

    private LocalDateTime workStartedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private String cancellationReason;

    private boolean arrivalVerified;

    private boolean completionVerified;

    private BigDecimal currentLatitude;

    private BigDecimal currentLongitude;

    private Integer distanceToSiteMeters;

    private boolean insideGeofence;

    private LocalDateTime lastLocationAt;

    private boolean trackingEnabled;

    private LocalDateTime scheduledEndAt;

    private Integer totalWorkedMinutes;

    private Integer totalBreakMinutes;

    private Integer totalTravelMinutes;

    private Integer extensionCount;

    private BigDecimal agreedRate;

    private BigDecimal baseEarnings;

    private BigDecimal extensionEarnings;

    private BigDecimal platformCommission;

    private BigDecimal netPayout;

}
