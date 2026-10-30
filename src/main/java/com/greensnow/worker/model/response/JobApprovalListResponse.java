package com.greensnow.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApprovalListResponse {

    private String responseCode;
    private String responseMessage;
    private String jobId;
    private Integer totalEntries;
    private List<JobApprovalDetail> approvals;
}
