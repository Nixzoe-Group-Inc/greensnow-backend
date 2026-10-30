package com.ayitech.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveMonitorResponse {

    private String responseCode;
    private String responseMessage;
    // the moment the picture was taken. a monitor is only ever a snapshot, and a screen that
    // does not say when it was drawn is one somebody will read as "now" an hour later
    private LocalDateTime asOf;
    private Integer totalLive;
    private Integer travellingToSite;
    private Integer onSite;
    private Integer working;
    private Integer paused;
    private Integer overrunning;
    private Integer outsideGeofence;
    private Integer awaitingExtensionDecision;
    private Integer signalLost;
    private List<JobExecutionDetail> workers;
}
