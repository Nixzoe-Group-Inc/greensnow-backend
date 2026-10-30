package com.greensnow.worker.model.entity;

import com.greensnow.worker.model.enums.WorkType;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "work_sessions")
public class WorkSession extends BaseEntity {

    private String jobExecutionId;

    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    private WorkType workType;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Integer durationMinutes;

    private BigDecimal startLatitude;

    private BigDecimal startLongitude;

    private BigDecimal endLatitude;

    private BigDecimal endLongitude;

    private boolean geofenceVerified;

    private boolean extension;

    private BigDecimal rateMultiplier;

    private BigDecimal earnings;

    private String notes;

}
