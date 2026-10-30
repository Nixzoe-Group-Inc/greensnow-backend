package com.greensnow.worker.model.response;

import com.greensnow.worker.model.enums.ProficiencyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeSkillDetail {

    private String employeeSkillId;
    private String skillId;
    private String skillName;
    private String skillCategory;
    private ProficiencyLevel proficiencyLevel;
    private Integer yearsOfExperience;
    private boolean certified;
    private String certificationName;
    private String certificationUrl;
    private LocalDate certificationExpiresOn;
    private boolean certificationExpired;
    private boolean verified;
}
