package com.ayitech.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneListResponse {

    private String responseCode;
    private String responseMessage;
    private String executionId;
    private Integer totalMilestones;
    private Integer completedMilestones;
    private Integer progressPercentage;
    private List<MilestoneDetail> milestones;
}
