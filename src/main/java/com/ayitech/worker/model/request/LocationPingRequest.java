package com.ayitech.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationPingRequest {

    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracyMeters;
    private BigDecimal altitudeMeters;
    private BigDecimal speedKmh;
    private BigDecimal headingDegrees;
    // the phone's own battery and signal, carried so a gap in the stream can be told apart
    // from a worker who switched tracking off. a dashboard that cannot make that distinction
    // shows "went dark" for a flat battery and sends somebody out to check on them
    private Integer batteryLevel;
    private String networkType;
    private String deviceId;
}
