package com.ayitech.worker.model.entity;

import com.ayitech.worker.model.enums.LocationType;
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
@Table(name = "location_tracking")
public class LocationTracking extends BaseEntity {

    private String jobExecutionId;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private BigDecimal accuracyMeters;

    private BigDecimal altitudeMeters;

    private BigDecimal speedKmh;

    private BigDecimal headingDegrees;

    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    private Integer distanceToSiteMeters;

    private boolean insideGeofence;

    private Integer batteryLevel;

    private String networkType;

    private String deviceId;

    private LocalDateTime recordedAt;

}
