package com.greensnow.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.greensnow.worker.model.enums.LocationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationPointDetail {

    private String pointId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracyMeters;
    private BigDecimal speedKmh;
    private BigDecimal headingDegrees;
    private LocationType locationType;
    private Integer distanceToSiteMeters;
    private Boolean insideGeofence;
    private Integer batteryLevel;
    private String networkType;
    private LocalDateTime recordedAt;
}
