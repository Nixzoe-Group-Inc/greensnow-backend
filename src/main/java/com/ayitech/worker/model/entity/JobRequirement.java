package com.ayitech.worker.model.entity;

import com.ayitech.worker.model.enums.ProficiencyLevel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_requirements")
public class JobRequirement extends BaseEntity {

    private String jobId;

    private String skillId;

    @Enumerated(EnumType.STRING)
    private ProficiencyLevel minimumProficiency;

    private Integer minimumYears;

    private boolean mandatory;

    private boolean certificationRequired;

    private BigDecimal weight;

}
