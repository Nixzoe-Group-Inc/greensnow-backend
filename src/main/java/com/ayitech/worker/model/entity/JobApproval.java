package com.ayitech.worker.model.entity;

import com.ayitech.worker.model.enums.ApprovalAction;
import com.ayitech.worker.model.enums.ApprovalStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_approvals")
public class JobApproval extends BaseEntity {

    private String jobId;

    private String reviewerId;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus status;

    @Enumerated(EnumType.STRING)
    private ApprovalAction action;

    private String comments;

    private String rejectionReason;

    private LocalDateTime reviewedAt;

    private Long reviewDurationSeconds;

}
