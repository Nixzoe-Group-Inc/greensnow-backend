package com.greensnow.worker.model.response;

import com.greensnow.worker.model.enums.ProficiencyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequirementDetail {

    private String skillId;
    private String skillName;
    private ProficiencyLevel minimumProficiency;
    private Integer minimumYears;
    private boolean mandatory;
    private boolean certificationRequired;
    private BigDecimal weight;

    // read against the worker asking, so a job that is out of reach says which skill put it
    // there rather than just scoring low
    private boolean met;
    private ProficiencyLevel heldProficiency;
    private Integer heldYears;
}
