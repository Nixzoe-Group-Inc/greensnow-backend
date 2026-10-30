package com.greensnow.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtensionRequest {

    private Integer requestedHours;
    private String reason;
    // how far through the work the worker reckons they are. it is their own estimate and
    // nothing verifies it, but an employer deciding on an extension with no idea whether the
    // job is half done or nearly finished is deciding blind
    private Integer workCompletedPercentage;
}
