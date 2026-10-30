package com.greensnow.worker.model.request;

import com.greensnow.worker.model.enums.ProficiencyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// the booleans are boxed rather than primitive so an unsent field reads as "not said" instead
// of as false. a requirement silently turned optional is one a worker can be matched without
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequirementRequest {

    private String skillId;
    private ProficiencyLevel minimumProficiency;
    private Integer minimumYears;
    private Boolean mandatory;
    private Boolean certificationRequired;
    private BigDecimal weight;
}
