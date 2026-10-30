package com.greensnow.worker.model.entity;

import com.greensnow.worker.model.enums.MilestoneStatus;
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
@Table(name = "job_milestones")
public class JobMilestone extends BaseEntity {

    private String jobExecutionId;

    private String title;

    private String description;

    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    private MilestoneStatus status;

    private Integer weightPercentage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private BigDecimal completedLatitude;

    private BigDecimal completedLongitude;

    private String proofPhotoUrl;

    private String employeeNotes;

    private boolean employerApproved;

    private LocalDateTime employerApprovedAt;

    private String blockedReason;

}
