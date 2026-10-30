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
public class WorkSessionListResponse {

    private String responseCode;
    private String responseMessage;
    private String executionId;
    private Integer totalSessions;
    private Integer totalWorkedMinutes;
    private Integer totalBreakMinutes;
    private Integer totalTravelMinutes;
    private List<WorkSessionDetail> sessions;
}
