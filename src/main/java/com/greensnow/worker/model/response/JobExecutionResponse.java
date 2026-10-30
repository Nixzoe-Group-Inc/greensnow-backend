package com.greensnow.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecutionResponse {

    private String responseCode;
    private String responseMessage;
    private JobExecutionDetail execution;
}
