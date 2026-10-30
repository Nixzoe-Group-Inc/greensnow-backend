package com.greensnow.worker.model.entity;

import com.greensnow.worker.model.enums.ProficiencyLevel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee_skills")
public class EmployeeSkill extends BaseEntity {

    private String employeeProfileId;

    private String skillId;

    @Enumerated(EnumType.STRING)
    private ProficiencyLevel proficiencyLevel;

    private Integer yearsOfExperience;

    private boolean certified;

    private String certificationName;

    private String certificationUrl;

    private LocalDate certificationExpiresOn;

    private boolean verified;

}
