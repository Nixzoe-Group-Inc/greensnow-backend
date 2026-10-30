package com.ayitech.worker.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ayitech.worker.model.enums.ExecutionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecutionDetail {

    private String executionId;
    private String jobId;
    private String jobTitle;
    private String employeeProfileId;
    // who is doing the work, carried for the employer's side. the worker's own reads already
    // know whose job it is, but a monitor showing a row per execution with no name on it is
    // a list of ids
    private String workerName;
    private String workerPhoneNumber;
    private ExecutionStatus status;

    private String siteAddress;
    private BigDecimal siteLatitude;
    private BigDecimal siteLongitude;
    private Integer siteRadiusMeters;

    private LocalDateTime acceptedAt;
    private LocalDateTime travelStartedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime workStartedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private Boolean arrivalVerified;
    private Boolean completionVerified;

    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    private Integer distanceToSiteMeters;
    private Boolean insideGeofence;
    private LocalDateTime lastLocationAt;
    // how long since the phone last said anything. a timestamp alone makes every reader do
    // the subtraction, and the whole point of the monitor is to be read at a glance
    private Long minutesSinceLastLocation;
    private Boolean trackingEnabled;

    private LocalDateTime scheduledEndAt;
    private Long minutesRemaining;
    // past the agreed finish and still working. this is what an extension exists to answer,
    // so it is said plainly rather than left for the reader to infer from two timestamps
    private Boolean overrunning;

    private Integer totalWorkedMinutes;
    private Integer totalBreakMinutes;
    private Integer totalTravelMinutes;
    private Integer extensionCount;
    private Integer extensionsAllowed;
    private Integer extensionsRemaining;
    private Boolean extensionPending;

    private Integer milestonesTotal;
    private Integer milestonesCompleted;
    private Integer progressPercentage;

    private BigDecimal agreedRate;
    private BigDecimal baseEarnings;
    private BigDecimal extensionEarnings;
    private BigDecimal totalEarnings;

    private List<MilestoneDetail> milestones;
    private WorkSessionDetail currentSession;
}
