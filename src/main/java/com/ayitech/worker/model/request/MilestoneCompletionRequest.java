package com.ayitech.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneCompletionRequest {

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String proofPhotoUrl;
    private String notes;
    private String blockedReason;
}
