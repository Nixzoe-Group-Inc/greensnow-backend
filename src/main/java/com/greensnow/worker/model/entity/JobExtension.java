package com.greensnow.worker.model.entity;

import com.greensnow.worker.model.enums.ExtensionStatus;
import com.greensnow.worker.model.enums.RequestedByParty;
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
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_extensions")
public class JobExtension extends BaseEntity {

    private String jobExecutionId;

    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    private ExtensionStatus status;

    @Enumerated(EnumType.STRING)
    private RequestedByParty requestedBy;

    private String reason;

    private Integer requestedHours;

    private Integer approvedHours;

    private LocalDateTime originalEndAt;

    private LocalDateTime proposedEndAt;

    private LocalDateTime newEndAt;

    private BigDecimal rateMultiplier;

    private BigDecimal estimatedAdditionalCost;

    private BigDecimal actualAdditionalCost;

    private Integer workCompletedPercentage;

    private LocalDateTime requestedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime respondedAt;

    private String respondedById;

    private String rejectionReason;

    private String employerNotes;

}
