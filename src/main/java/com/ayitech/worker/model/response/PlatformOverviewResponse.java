package com.ayitech.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// counted on read rather than kept on a row. these are small counts against indexed columns and
// a stored tally that drifts is worse than no tally at all - the queue depth in particular is
// the number the approver decides their day on
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformOverviewResponse {

    private String responseCode;
    private String responseMessage;

    private Long workers;
    private Long employers;
    private Long verifiedEmployers;

    private Long jobsAwaitingApproval;
    private Long jobsApproved;
    private Long jobsRejected;
    private Long jobsWithChangesRequested;
}
