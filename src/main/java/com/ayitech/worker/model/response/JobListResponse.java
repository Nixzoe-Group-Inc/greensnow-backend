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
public class JobListResponse {

    private String responseCode;
    private String responseMessage;
    private Integer totalJobs;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private List<JobDetail> jobs;
}
