package com.ayitech.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ayitech.worker.model.enums.ExtensionStatus;
import com.ayitech.worker.model.enums.RequestedByParty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExtensionDetail {

    private String extensionId;
    private String executionId;
    private String jobId;
    private String jobTitle;
    private String workerName;
    private Integer sequenceNumber;
    private ExtensionStatus status;
    private RequestedByParty requestedBy;
    private String reason;
    private Integer requestedHours;
    private Integer approvedHours;
    private LocalDateTime originalEndAt;
    private LocalDateTime proposedEndAt;
    private LocalDateTime newEndAt;
    private BigDecimal rateMultiplier;
    // what the extra hours will cost if granted. the employer is agreeing to money rather
    // than to time, so the figure travels with the request instead of being worked out
    // afterwards from the rate and the hours
    private BigDecimal estimatedAdditionalCost;
    private BigDecimal actualAdditionalCost;
    private Integer workCompletedPercentage;
    private LocalDateTime requestedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
    private String rejectionReason;
    private String employerNotes;
}
