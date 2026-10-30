package com.ayitech.worker.model.request;

import com.ayitech.worker.model.enums.ProficiencyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSkillRequest {

    private String skillId;
    private ProficiencyLevel proficiencyLevel;
    private Integer yearsOfExperience;
    private Boolean certified;
    private String certificationName;
    private String certificationUrl;
    private LocalDate certificationExpiresOn;
}
