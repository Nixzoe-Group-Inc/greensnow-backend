package com.greensnow.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.greensnow.worker.model.enums.MilestoneStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneDetail {

    private String milestoneId;
    private String executionId;
    private String title;
    private String description;
    private Integer displayOrder;
    private MilestoneStatus status;
    private Integer weightPercentage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private BigDecimal completedLatitude;
    private BigDecimal completedLongitude;
    private String proofPhotoUrl;
    private String employeeNotes;
    private Boolean employerApproved;
    private LocalDateTime employerApprovedAt;
    private String blockedReason;
}
