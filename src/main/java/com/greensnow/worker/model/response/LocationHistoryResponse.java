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
public class LocationHistoryResponse {

    private String responseCode;
    private String responseMessage;
    private String executionId;
    private Integer totalPoints;
    private LocationPointDetail latest;
    private List<LocationPointDetail> points;
}
