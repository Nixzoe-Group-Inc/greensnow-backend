package com.greensnow.worker.model.response;

import com.greensnow.worker.model.enums.ApprovalAction;
import com.greensnow.worker.model.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// one entry in a job's approval history. the reviewer's id is not carried: the employer needs
// to read what was said about their job, not who on the platform said it
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApprovalDetail {

    private String approvalId;
    private String jobId;
    private ApprovalStatus status;
    private ApprovalAction action;
    private String comments;
    private String rejectionReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
