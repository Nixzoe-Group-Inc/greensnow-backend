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
public class JobExtensionListResponse {

    private String responseCode;
    private String responseMessage;
    private Integer totalExtensions;
    private List<JobExtensionDetail> extensions;
}
