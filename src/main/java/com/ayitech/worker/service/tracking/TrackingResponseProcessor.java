package com.ayitech.worker.service.tracking;

import com.ayitech.worker.model.constant.ResponseCode;
import com.ayitech.worker.model.constant.ResponseMessage;
import com.ayitech.worker.model.entity.Job;
import com.ayitech.worker.model.entity.JobExecution;
import com.ayitech.worker.model.entity.JobExtension;
import com.ayitech.worker.model.entity.JobMilestone;
import com.ayitech.worker.model.entity.LocationTracking;
import com.ayitech.worker.model.entity.TrackingNotification;
import com.ayitech.worker.model.entity.User;
import com.ayitech.worker.model.entity.WorkSession;
import com.ayitech.worker.model.enums.ExecutionStatus;
import com.ayitech.worker.model.enums.MilestoneStatus;
import com.ayitech.worker.model.response.GeneralResponse;
import com.ayitech.worker.model.response.JobExecutionDetail;
import com.ayitech.worker.model.response.JobExecutionListResponse;
import com.ayitech.worker.model.response.JobExecutionResponse;
import com.ayitech.worker.model.response.JobExtensionDetail;
import com.ayitech.worker.model.response.JobExtensionListResponse;
import com.ayitech.worker.model.response.JobExtensionResponse;
import com.ayitech.worker.model.response.LiveMonitorResponse;
import com.ayitech.worker.model.response.LocationHistoryResponse;
import com.ayitech.worker.model.response.LocationPointDetail;
import com.ayitech.worker.model.response.MilestoneDetail;
import com.ayitech.worker.model.response.MilestoneListResponse;
import com.ayitech.worker.model.response.MilestoneResponse;
import com.ayitech.worker.model.response.TrackingNotificationDetail;
import com.ayitech.worker.model.response.TrackingNotificationListResponse;
import com.ayitech.worker.model.response.WorkSessionDetail;
import com.ayitech.worker.model.response.WorkSessionListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TrackingResponseProcessor {

    // past this long with nothing from the handset the monitor calls it a lost signal rather
    // than a position. the last point is still shown, because "here is where they were twenty
    // minutes ago" is useful and "here is where they are" would be a lie
    private static final long SIGNAL_LOST_MINUTES = 15;

    public JobExecutionResponse processExecutionResponse(String responseMessage, JobExecutionDetail execution) {
        return JobExecutionResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .execution(execution)
                .build();
    }

    public JobExecutionResponse failedExecution(String responseCode, String responseMessage) {
        return JobExecutionResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public JobExecutionListResponse processExecutionListResponse(List<JobExecutionDetail> executions) {
        return JobExecutionListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.EXECUTIONS_RETRIEVED)
                .totalExecutions(executions.size())
                .executions(executions)
                .build();
    }

    public JobExecutionListResponse failedExecutionList(String responseCode, String responseMessage) {
        return JobExecutionListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    // the monitor is counted rather than listed first. a company with thirty people out needs
    // to know how many are overrunning before it needs to know which, and a screen that makes
    // the reader count rows themselves is one nobody checks
    public LiveMonitorResponse processMonitorResponse(List<JobExecutionDetail> workers) {
        return LiveMonitorResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.MONITOR_RETRIEVED)
                .asOf(LocalDateTime.now())
                .totalLive(workers.size())
                .travellingToSite(countOf(workers, ExecutionStatus.TRAVELING_TO_SITE))
                .onSite(countOf(workers, ExecutionStatus.ON_SITE))
                .working(countOf(workers, ExecutionStatus.IN_PROGRESS) + countOf(workers, ExecutionStatus.EXTENDED))
                .paused(countOf(workers, ExecutionStatus.PAUSED))
                .overrunning((int) workers.stream().filter(worker -> Boolean.TRUE.equals(worker.getOverrunning())).count())
                // a worker who has started and is not where the work is. it is counted apart
                // from the overrun because the two ask different questions of the company -
                // one is running late, the other is not at the job at all
                .outsideGeofence((int) workers.stream()
                        .filter(worker -> Objects.nonNull(worker.getArrivedAt()))
                        .filter(worker -> Boolean.FALSE.equals(worker.getInsideGeofence()))
                        .count())
                .awaitingExtensionDecision((int) workers.stream()
                        .filter(worker -> Boolean.TRUE.equals(worker.getExtensionPending()))
                        .count())
                .signalLost((int) workers.stream()
                        .filter(worker -> Objects.nonNull(worker.getMinutesSinceLastLocation()))
                        .filter(worker -> worker.getMinutesSinceLastLocation() >= SIGNAL_LOST_MINUTES)
                        .count())
                .workers(workers)
                .build();
    }

    public LiveMonitorResponse failedMonitor(String responseCode, String responseMessage) {
        return LiveMonitorResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public LocationHistoryResponse processLocationHistoryResponse(String executionId, List<LocationTracking> points) {
        List<LocationPointDetail> entries = points.stream()
                .map(this::toLocationPointDetail)
                .toList();

        return LocationHistoryResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.LOCATION_HISTORY_RETRIEVED)
                .executionId(executionId)
                .totalPoints(entries.size())
                // the whole route is the trail and the last point is where they are. both are
                // returned so a map can draw the line and drop the pin without walking the list
                .latest(entries.isEmpty() ? null : entries.getLast())
                .points(entries)
                .build();
    }

    public LocationHistoryResponse failedLocationHistory(String responseCode, String responseMessage) {
        return LocationHistoryResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public WorkSessionListResponse processSessionListResponse(String executionId, JobExecution execution,
                                                              List<WorkSession> sessions) {
        List<WorkSessionDetail> entries = sessions.stream()
                .map(this::toSessionDetail)
                .toList();

        return WorkSessionListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.SESSIONS_RETRIEVED)
                .executionId(executionId)
                .totalSessions(entries.size())
                // the running totals come off the execution rather than being added up here.
                // they are what the pay is worked out from, so the figure a worker reads on
                // their day and the figure that is paid are the same number
                .totalWorkedMinutes(orZero(execution.getTotalWorkedMinutes()))
                .totalBreakMinutes(orZero(execution.getTotalBreakMinutes()))
                .totalTravelMinutes(orZero(execution.getTotalTravelMinutes()))
                .sessions(entries)
                .build();
    }

    public WorkSessionListResponse failedSessionList(String responseCode, String responseMessage) {
        return WorkSessionListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public JobExtensionResponse processExtensionResponse(String responseMessage, JobExtension extension,
                                                         String jobId, String jobTitle, String workerName) {
        return JobExtensionResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .extension(toExtensionDetail(extension, jobId, jobTitle, workerName))
                .build();
    }

    public JobExtensionResponse failedExtension(String responseCode, String responseMessage) {
        return JobExtensionResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public JobExtensionListResponse processExtensionListResponse(List<JobExtensionDetail> extensions) {
        return JobExtensionListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.EXTENSIONS_RETRIEVED)
                .totalExtensions(extensions.size())
                .extensions(extensions)
                .build();
    }

    public JobExtensionListResponse failedExtensionList(String responseCode, String responseMessage) {
        return JobExtensionListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public MilestoneResponse processMilestoneResponse(String responseMessage, JobMilestone milestone) {
        return MilestoneResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .milestone(toMilestoneDetail(milestone))
                .build();
    }

    public MilestoneResponse failedMilestone(String responseCode, String responseMessage) {
        return MilestoneResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public MilestoneListResponse processMilestoneListResponse(String executionId, List<JobMilestone> milestones) {
        List<MilestoneDetail> entries = milestones.stream()
                .map(this::toMilestoneDetail)
                .toList();

        return MilestoneListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.MILESTONES_RETRIEVED)
                .executionId(executionId)
                .totalMilestones(entries.size())
                .completedMilestones((int) milestones.stream()
                        .filter(milestone -> MilestoneStatus.COMPLETED.equals(milestone.getStatus()))
                        .count())
                .progressPercentage(progressPercentage(milestones))
                .milestones(entries)
                .build();
    }

    public MilestoneListResponse failedMilestoneList(String responseCode, String responseMessage) {
        return MilestoneListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public TrackingNotificationListResponse processNotificationListResponse(List<TrackingNotification> notifications) {
        List<TrackingNotificationDetail> entries = notifications.stream()
                .map(this::toNotificationDetail)
                .toList();

        return TrackingNotificationListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.NOTIFICATIONS_RETRIEVED)
                .totalNotifications(entries.size())
                .unread((int) notifications.stream()
                        .filter(notification -> Objects.isNull(notification.getReadAt()))
                        .count())
                .notifications(entries)
                .build();
    }

    public TrackingNotificationListResponse failedNotificationList(String responseCode, String responseMessage) {
        return TrackingNotificationListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public GeneralResponse processGeneralResponse(String responseMessage) {
        return GeneralResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .build();
    }

    public GeneralResponse failedGeneral(String responseCode, String responseMessage) {
        return GeneralResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    // the one place an execution is turned into something a person reads, so that a worker's
    // own view of their day and the company's view of the same worker are the same record
    // rendered once. the pieces the caller did not look up are passed as null rather than
    // being fetched here - a monitor of forty rows does not read forty milestone lists
    public JobExecutionDetail toExecutionDetail(JobExecution execution, Job job, User worker,
                                                List<JobMilestone> milestones, WorkSession currentSession,
                                                boolean extensionPending) {
        LocalDateTime now = LocalDateTime.now();
        Integer extensionsAllowed = Objects.nonNull(job) ? job.getExtensionsAllowed() : null;
        Integer used = orZero(execution.getExtensionCount());

        return JobExecutionDetail.builder()
                .executionId(execution.getId())
                .jobId(execution.getJobId())
                .jobTitle(Objects.nonNull(job) ? job.getTitle() : null)
                .employeeProfileId(execution.getEmployeeProfileId())
                .workerName(composeFullName(worker))
                .workerPhoneNumber(Objects.nonNull(worker) ? worker.getPhoneNumber() : null)
                .status(execution.getStatus())
                .siteAddress(Objects.nonNull(job) ? job.getSiteAddress() : null)
                .siteLatitude(Objects.nonNull(job) ? job.getSiteLatitude() : null)
                .siteLongitude(Objects.nonNull(job) ? job.getSiteLongitude() : null)
                .siteRadiusMeters(Objects.nonNull(job) ? job.getSiteRadiusMeters() : null)
                .acceptedAt(execution.getAcceptedAt())
                .travelStartedAt(execution.getTravelStartedAt())
                .arrivedAt(execution.getArrivedAt())
                .workStartedAt(execution.getWorkStartedAt())
                .completedAt(execution.getCompletedAt())
                .cancelledAt(execution.getCancelledAt())
                .cancellationReason(execution.getCancellationReason())
                .arrivalVerified(execution.isArrivalVerified())
                .completionVerified(execution.isCompletionVerified())
                .currentLatitude(execution.getCurrentLatitude())
                .currentLongitude(execution.getCurrentLongitude())
                .distanceToSiteMeters(execution.getDistanceToSiteMeters())
                .insideGeofence(execution.isInsideGeofence())
                .lastLocationAt(execution.getLastLocationAt())
                .minutesSinceLastLocation(minutesBetween(execution.getLastLocationAt(), now))
                .trackingEnabled(execution.isTrackingEnabled())
                .scheduledEndAt(execution.getScheduledEndAt())
                .minutesRemaining(minutesBetween(now, execution.getScheduledEndAt()))
                // only a job that is still running can be overrunning. one that finished late
                // is late, which is a different thing and belongs in the metrics rather than
                // on a live screen asking somebody to act
                .overrunning(isOverrunning(execution, now))
                .totalWorkedMinutes(orZero(execution.getTotalWorkedMinutes()))
                .totalBreakMinutes(orZero(execution.getTotalBreakMinutes()))
                .totalTravelMinutes(orZero(execution.getTotalTravelMinutes()))
                .extensionCount(used)
                .extensionsAllowed(extensionsAllowed)
                .extensionsRemaining(Objects.nonNull(extensionsAllowed)
                        ? Math.max(extensionsAllowed - used, 0)
                        : null)
                .extensionPending(extensionPending)
                .milestonesTotal(Objects.nonNull(milestones) ? milestones.size() : null)
                .milestonesCompleted(Objects.nonNull(milestones)
                        ? (int) milestones.stream()
                        .filter(milestone -> MilestoneStatus.COMPLETED.equals(milestone.getStatus()))
                        .count()
                        : null)
                .progressPercentage(Objects.nonNull(milestones) ? progressPercentage(milestones) : null)
                .agreedRate(execution.getAgreedRate())
                .baseEarnings(execution.getBaseEarnings())
                .extensionEarnings(execution.getExtensionEarnings())
                .totalEarnings(sum(execution.getBaseEarnings(), execution.getExtensionEarnings()))
                .milestones(Objects.nonNull(milestones) ? milestones.stream().map(this::toMilestoneDetail).toList() : null)
                .currentSession(Objects.nonNull(currentSession) ? toSessionDetail(currentSession) : null)
                .build();
    }

    public JobExtensionDetail toExtensionDetail(JobExtension extension, String jobId, String jobTitle,
                                                String workerName) {
        return JobExtensionDetail.builder()
                .extensionId(extension.getId())
                .executionId(extension.getJobExecutionId())
                .jobId(jobId)
                .jobTitle(jobTitle)
                .workerName(workerName)
                .sequenceNumber(extension.getSequenceNumber())
                .status(extension.getStatus())
                .requestedBy(extension.getRequestedBy())
                .reason(extension.getReason())
                .requestedHours(extension.getRequestedHours())
                .approvedHours(extension.getApprovedHours())
                .originalEndAt(extension.getOriginalEndAt())
                .proposedEndAt(extension.getProposedEndAt())
                .newEndAt(extension.getNewEndAt())
                .rateMultiplier(extension.getRateMultiplier())
                .estimatedAdditionalCost(extension.getEstimatedAdditionalCost())
                .actualAdditionalCost(extension.getActualAdditionalCost())
                .workCompletedPercentage(extension.getWorkCompletedPercentage())
                .requestedAt(extension.getRequestedAt())
                .expiresAt(extension.getExpiresAt())
                .respondedAt(extension.getRespondedAt())
                .rejectionReason(extension.getRejectionReason())
                .employerNotes(extension.getEmployerNotes())
                .build();
    }

    public MilestoneDetail toMilestoneDetail(JobMilestone milestone) {
        return MilestoneDetail.builder()
                .milestoneId(milestone.getId())
                .executionId(milestone.getJobExecutionId())
                .title(milestone.getTitle())
                .description(milestone.getDescription())
                .displayOrder(milestone.getDisplayOrder())
                .status(milestone.getStatus())
                .weightPercentage(milestone.getWeightPercentage())
                .startedAt(milestone.getStartedAt())
                .completedAt(milestone.getCompletedAt())
                .completedLatitude(milestone.getCompletedLatitude())
                .completedLongitude(milestone.getCompletedLongitude())
                .proofPhotoUrl(milestone.getProofPhotoUrl())
                .employeeNotes(milestone.getEmployeeNotes())
                .employerApproved(milestone.isEmployerApproved())
                .employerApprovedAt(milestone.getEmployerApprovedAt())
                .blockedReason(milestone.getBlockedReason())
                .build();
    }

    public WorkSessionDetail toSessionDetail(WorkSession session) {
        return WorkSessionDetail.builder()
                .sessionId(session.getId())
                .workDate(session.getWorkDate())
                .workType(session.getWorkType())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                // a session that is still open has no duration of its own yet, so the minutes
                // it has run so far are worked out on read. a phone showing nothing until the
                // worker stops is a timer that only ever reads zero while it matters
                .durationMinutes(Objects.nonNull(session.getDurationMinutes())
                        ? session.getDurationMinutes()
                        : elapsedMinutes(session.getStartedAt()))
                .geofenceVerified(session.isGeofenceVerified())
                .extension(session.isExtension())
                .rateMultiplier(session.getRateMultiplier())
                .earnings(session.getEarnings())
                .notes(session.getNotes())
                .build();
    }

    public LocationPointDetail toLocationPointDetail(LocationTracking point) {
        return LocationPointDetail.builder()
                .pointId(point.getId())
                .latitude(point.getLatitude())
                .longitude(point.getLongitude())
                .accuracyMeters(point.getAccuracyMeters())
                .speedKmh(point.getSpeedKmh())
                .headingDegrees(point.getHeadingDegrees())
                .locationType(point.getLocationType())
                .distanceToSiteMeters(point.getDistanceToSiteMeters())
                .insideGeofence(point.isInsideGeofence())
                .batteryLevel(point.getBatteryLevel())
                .networkType(point.getNetworkType())
                .recordedAt(point.getRecordedAt())
                .build();
    }

    public TrackingNotificationDetail toNotificationDetail(TrackingNotification notification) {
        return TrackingNotificationDetail.builder()
                .notificationId(notification.getId())
                .executionId(notification.getJobExecutionId())
                .notificationType(notification.getNotificationType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .actionUrl(notification.getActionUrl())
                .requiresAction(notification.isRequiresAction())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    // progress is weighted rather than counted, because the steps of a job are not the same
    // size. four of five boxes ticked reads as eighty per cent when the fifth is the two days
    // of work and the other four were an hour each
    private Integer progressPercentage(List<JobMilestone> milestones) {
        if (Objects.isNull(milestones) || milestones.isEmpty()) {
            return null;
        }

        int totalWeight = milestones.stream().mapToInt(milestone -> orZero(milestone.getWeightPercentage())).sum();
        if (totalWeight <= 0) {
            // a job whose steps carry no weights at all falls back to counting them, which is
            // the best that can honestly be said about it
            long completed = milestones.stream()
                    .filter(milestone -> MilestoneStatus.COMPLETED.equals(milestone.getStatus()))
                    .count();
            return (int) Math.round((completed * 100.0) / milestones.size());
        }

        int completedWeight = milestones.stream()
                .filter(milestone -> MilestoneStatus.COMPLETED.equals(milestone.getStatus()))
                .mapToInt(milestone -> orZero(milestone.getWeightPercentage()))
                .sum();
        return (int) Math.round((completedWeight * 100.0) / totalWeight);
    }

    private Boolean isOverrunning(JobExecution execution, LocalDateTime now) {
        if (Objects.isNull(execution.getScheduledEndAt()) || Objects.isNull(execution.getStatus())) {
            return false;
        }
        return execution.getStatus().isLive() && now.isAfter(execution.getScheduledEndAt());
    }

    private Long minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (Objects.isNull(from) || Objects.isNull(to)) {
            return null;
        }
        return Duration.between(from, to).toMinutes();
    }

    private Integer elapsedMinutes(LocalDateTime from) {
        if (Objects.isNull(from)) {
            return null;
        }
        return (int) Duration.between(from, LocalDateTime.now()).toMinutes();
    }

    private int countOf(List<JobExecutionDetail> workers, ExecutionStatus status) {
        return (int) workers.stream().filter(worker -> status.equals(worker.getStatus())).count();
    }

    private BigDecimal sum(BigDecimal first, BigDecimal second) {
        BigDecimal left = Objects.nonNull(first) ? first : BigDecimal.ZERO;
        BigDecimal right = Objects.nonNull(second) ? second : BigDecimal.ZERO;
        return left.add(right);
    }

    private int orZero(Integer value) {
        return Objects.nonNull(value) ? value : 0;
    }

    private String composeFullName(User user) {
        if (Objects.isNull(user)) {
            return null;
        }
        return Stream.of(user.getFirstName(), user.getLastName())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }

    // a helper for the callers that have already read a batch of users or jobs and want them
    // keyed up for the detail calls, so a monitor of forty rows is three queries and not eighty
    public <T> Map<String, T> byId(List<T> items, java.util.function.Function<T, String> idOf) {
        return items.stream().collect(Collectors.toMap(idOf, item -> item, (first, second) -> first));
    }
}
