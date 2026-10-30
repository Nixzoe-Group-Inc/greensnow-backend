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
public class ApplicantListResponse {

    private String responseCode;
    private String responseMessage;
    private String jobId;
    private String jobTitle;
    private Integer workersNeeded;
    private Integer workersAssigned;
    private Integer vacancies;
    private Integer totalApplicants;
    private List<ApplicantDetail> applicants;
}
