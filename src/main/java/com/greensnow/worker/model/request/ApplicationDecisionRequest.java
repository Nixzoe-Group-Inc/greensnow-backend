package com.greensnow.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// one body for shortlist, accept and refuse. which of the three is being done is the route,
// never a field, so nothing on the wire can turn a refusal into an acceptance
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDecisionRequest {

    private String reason;
}
