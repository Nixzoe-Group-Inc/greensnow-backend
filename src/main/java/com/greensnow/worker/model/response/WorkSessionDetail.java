package com.greensnow.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.greensnow.worker.model.enums.WorkType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkSessionDetail {

    private String sessionId;
    private LocalDate workDate;
    private WorkType workType;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMinutes;
    private Boolean geofenceVerified;
    private Boolean extension;
    private BigDecimal rateMultiplier;
    private BigDecimal earnings;
    private String notes;
}
