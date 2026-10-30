package com.greensnow.worker.model.entity;

import jakarta.persistence.Entity;
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
@Table(name = "job_performance_metrics")
public class JobPerformanceMetrics extends BaseEntity {

    private String jobExecutionId;

    private Integer estimatedMinutes;

    private Integer actualMinutes;

    private Integer varianceMinutes;

    private BigDecimal efficiencyScore;

    private BigDecimal punctualityScore;

    private Integer arrivalDelayMinutes;

    private Integer travelMinutes;

    private Integer breakMinutes;

    private Integer idleMinutes;

    private Integer onSiteMinutes;

    private Integer geofenceExits;

    private Integer signalLosses;

    private BigDecimal distanceTravelledKm;

    private Integer extensionsRequested;

    private Integer extensionsApproved;

    private Integer milestonesTotal;

    private Integer milestonesCompleted;

    private boolean completedOnTime;

    private BigDecimal employerRating;

    private BigDecimal employeeRating;

}
