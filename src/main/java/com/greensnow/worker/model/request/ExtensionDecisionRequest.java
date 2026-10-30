package com.greensnow.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtensionDecisionRequest {

    // the employer may grant fewer hours than were asked for, which is why this is not a bare
    // yes. an approval that silently granted the full ask would commit the company to a cost
    // it never agreed to
    private Integer approvedHours;
    private String notes;
    private String reason;
}
