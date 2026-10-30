package com.greensnow.worker.service.tracking;

import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.entity.JobApplication;
import com.greensnow.worker.model.entity.JobExecution;
import com.greensnow.worker.model.entity.JobExtension;
import com.greensnow.worker.model.entity.JobMilestone;
import com.greensnow.worker.model.entity.LocationTracking;
import com.greensnow.worker.model.entity.TrackingNotification;
import com.greensnow.worker.model.entity.WorkSession;
import com.greensnow.worker.model.enums.ExecutionStatus;
import com.greensnow.worker.model.enums.ExtensionStatus;
import com.greensnow.worker.model.enums.LocationType;
import com.greensnow.worker.model.enums.MilestoneStatus;
import com.greensnow.worker.model.enums.RequestedByParty;
import com.greensnow.worker.model.enums.TrackingNotificationType;
import com.greensnow.worker.model.enums.WorkType;
import com.greensnow.worker.model.request.LocationPingRequest;
import com.greensnow.worker.model.request.MilestoneRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class TrackingRequestBuilder {

    // an extension is paid at time and a half unless something later decides otherwise. the
    // multiplier lives on the row rather than in the rate itself so that what the worker was
    // paid for the overrun is readable years later without knowing what the rule was that day
    private static final BigDecimal EXTENSION_RATE_MULTIPLIER = new BigDecimal("1.50");
    private static final BigDecimal NORMAL_RATE_MULTIPLIER = BigDecimal.ONE;

    // a company has a working day to answer a request for more time. past that the worker is
    // stood on a site with no instruction, so the request lapses rather than hanging open and
    // being granted the following week against hours that were worked or were not
    private static final int EXTENSION_RESPONSE_HOURS = 8;

    // the execution is born the moment an employer accepts somebody, not the moment they set
    // off. the row has to exist before the first location can be hung off it, and the gap
    // between being hired and starting is exactly the window the worker needs to see the job
    // on their phone at all
    public JobExecution buildExecution(Job job, JobApplication application) {
        return JobExecution.builder()
                .jobId(job.getId())
                .employeeProfileId(application.getEmployeeProfileId())
                .status(ExecutionStatus.NOT_STARTED)
                .acceptedAt(LocalDateTime.now())
                .arrivalVerified(false)
                .completionVerified(false)
                .insideGeofence(false)
                // tracking is on from the start, but nothing is recorded until the worker sets
                // off - the flag says the job is one that is tracked, not that a phone is
                // reporting right now
                .trackingEnabled(true)
                .totalWorkedMinutes(0)
                .totalBreakMinutes(0)
                .totalTravelMinutes(0)
                .extensionCount(0)
                // what the worker asked for if they named a rate, and the job's own rate if they
                // did not. it is pinned here rather than read off the job later because a job's
                // rate can be edited while it is a draft and this one is already agreed
                .agreedRate(Objects.nonNull(application.getProposedRate())
                        ? application.getProposedRate()
                        : job.getHourlyRate())
                .baseEarnings(BigDecimal.ZERO)
                .extensionEarnings(BigDecimal.ZERO)
                .build();
    }

    public LocationTracking buildLocation(String executionId, LocationPingRequest request, LocationType locationType,
                                          Integer distanceToSiteMeters, boolean insideGeofence) {
        return LocationTracking.builder()
                .jobExecutionId(executionId)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracyMeters(request.getAccuracyMeters())
                .altitudeMeters(request.getAltitudeMeters())
                .speedKmh(request.getSpeedKmh())
                .headingDegrees(request.getHeadingDegrees())
                .locationType(locationType)
                .distanceToSiteMeters(distanceToSiteMeters)
                .insideGeofence(insideGeofence)
                .batteryLevel(request.getBatteryLevel())
                .networkType(trimmed(request.getNetworkType()))
                .deviceId(trimmed(request.getDeviceId()))
                // the phone says when it took the reading, not when the server heard about it.
                // a van driving through a dead spot delivers an hour of points at once, and
                // stamping them all on arrival would draw a route that teleports
                .recordedAt(LocalDateTime.now())
                .build();
    }

    // every stretch of the day is a session, billable or not. travel, work, breaks and waiting
    // are all recorded the same way so the day adds up end to end - a worker who is paid for
    // six hours and was on site for nine can see where the other three went
    public WorkSession buildSession(String executionId, WorkType workType, BigDecimal latitude,
                                    BigDecimal longitude, boolean geofenceVerified, boolean extension,
                                    String notes) {
        LocalDateTime now = LocalDateTime.now();
        return WorkSession.builder()
                .jobExecutionId(executionId)
                .workDate(LocalDate.now())
                .workType(workType)
                .startedAt(now)
                .startLatitude(latitude)
                .startLongitude(longitude)
                .geofenceVerified(geofenceVerified)
                .extension(extension)
                .rateMultiplier(extension ? EXTENSION_RATE_MULTIPLIER : NORMAL_RATE_MULTIPLIER)
                .earnings(BigDecimal.ZERO)
                .notes(trimmed(notes))
                .build();
    }

    // an extension is a proposal with a price on it. the company is agreeing to a cost rather
    // than to a span of hours, so the figure is worked out here and travels with the request
    public JobExtension buildExtension(JobExecution execution, int sequenceNumber, Integer requestedHours,
                                       String reason, Integer workCompletedPercentage) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime originalEndAt = execution.getScheduledEndAt();
        LocalDateTime proposedEndAt = Objects.nonNull(originalEndAt)
                ? originalEndAt.plusHours(requestedHours)
                : now.plusHours(requestedHours);

        return JobExtension.builder()
                .jobExecutionId(execution.getId())
                .sequenceNumber(sequenceNumber)
                .status(ExtensionStatus.REQUESTED)
                // the worker is the one standing in front of the unfinished work, so they are
                // the one who asks. the field exists because the platform itself will later
                // raise one off an overrun nobody acted on
                .requestedBy(RequestedByParty.EMPLOYEE)
                .reason(trimmed(reason))
                .requestedHours(requestedHours)
                .originalEndAt(originalEndAt)
                .proposedEndAt(proposedEndAt)
                .rateMultiplier(EXTENSION_RATE_MULTIPLIER)
                .estimatedAdditionalCost(additionalCost(execution.getAgreedRate(), requestedHours))
                .workCompletedPercentage(workCompletedPercentage)
                .requestedAt(now)
                .expiresAt(now.plusHours(EXTENSION_RESPONSE_HOURS))
                .build();
    }

    // the company may grant fewer hours than were asked for, so the finish time is worked out
    // from what they actually granted and never from what was requested
    public JobExtension buildExtensionApproval(JobExtension extension, JobExecution execution, String responderId,
                                               Integer approvedHours, String notes) {
        LocalDateTime base = Objects.nonNull(extension.getOriginalEndAt())
                ? extension.getOriginalEndAt()
                : LocalDateTime.now();

        extension.setStatus(ExtensionStatus.APPROVED);
        extension.setApprovedHours(approvedHours);
        extension.setNewEndAt(base.plusHours(approvedHours));
        extension.setActualAdditionalCost(additionalCost(execution.getAgreedRate(), approvedHours));
        extension.setRespondedAt(LocalDateTime.now());
        extension.setRespondedById(responderId);
        extension.setEmployerNotes(trimmed(notes));
        return extension;
    }

    public JobExtension buildExtensionRejection(JobExtension extension, String responderId, String reason,
                                                String notes) {
        extension.setStatus(ExtensionStatus.REJECTED);
        extension.setApprovedHours(0);
        extension.setActualAdditionalCost(BigDecimal.ZERO);
        extension.setRespondedAt(LocalDateTime.now());
        extension.setRespondedById(responderId);
        extension.setRejectionReason(trimmed(reason));
        extension.setEmployerNotes(trimmed(notes));
        return extension;
    }

    public JobMilestone buildMilestone(String executionId, MilestoneRequest request, int displayOrder) {
        return JobMilestone.builder()
                .jobExecutionId(executionId)
                .title(trimmed(request.getTitle()))
                .description(trimmed(request.getDescription()))
                .displayOrder(Objects.nonNull(request.getDisplayOrder()) ? request.getDisplayOrder() : displayOrder)
                .status(MilestoneStatus.PENDING)
                .weightPercentage(request.getWeightPercentage())
                .employerApproved(false)
                .build();
    }

    // the notification is written alongside the thing that happened rather than pushed from
    // here. a push that fails is a push that can be retried off this row, where an event that
    // was only ever a push is gone the moment the handset is off
    public TrackingNotification buildNotification(String recipientId, String executionId,
                                                  TrackingNotificationType notificationType, String title,
                                                  String body, boolean requiresAction) {
        return TrackingNotification.builder()
                .recipientId(recipientId)
                .jobExecutionId(executionId)
                .notificationType(notificationType)
                .title(title)
                .body(body)
                .requiresAction(requiresAction)
                .pushed(false)
                .build();
    }

    private BigDecimal additionalCost(BigDecimal agreedRate, Integer hours) {
        if (Objects.isNull(agreedRate) || Objects.isNull(hours)) {
            // no rate on the job means no figure can honestly be put in front of the company.
            // zero would read as "the extra time is free", which is the one thing it is not
            return null;
        }
        return agreedRate.multiply(BigDecimal.valueOf(hours)).multiply(EXTENSION_RATE_MULTIPLIER);
    }

    private String trimmed(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
