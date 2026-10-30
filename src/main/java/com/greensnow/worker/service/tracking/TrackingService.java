package com.greensnow.worker.service.tracking;

import com.greensnow.worker.model.constant.ResponseCode;
import com.greensnow.worker.model.constant.ResponseMessage;
import com.greensnow.worker.model.entity.EmployeeProfile;
import com.greensnow.worker.model.entity.EmployerProfile;
import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.entity.JobExecution;
import com.greensnow.worker.model.entity.JobExtension;
import com.greensnow.worker.model.entity.JobMilestone;
import com.greensnow.worker.model.entity.LocationTracking;
import com.greensnow.worker.model.entity.TrackingNotification;
import com.greensnow.worker.model.entity.User;
import com.greensnow.worker.model.entity.WorkSession;
import com.greensnow.worker.model.enums.ExecutionStatus;
import com.greensnow.worker.model.enums.ExtensionStatus;
import com.greensnow.worker.model.enums.LocationType;
import com.greensnow.worker.model.enums.MilestoneStatus;
import com.greensnow.worker.model.enums.TrackingNotificationType;
import com.greensnow.worker.model.enums.WorkType;
import com.greensnow.worker.model.request.ExtensionDecisionRequest;
import com.greensnow.worker.model.request.ExtensionRequest;
import com.greensnow.worker.model.request.LocationPingRequest;
import com.greensnow.worker.model.request.MilestoneCompletionRequest;
import com.greensnow.worker.model.request.MilestoneRequest;
import com.greensnow.worker.model.request.WorkEventRequest;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.JobExecutionDetail;
import com.greensnow.worker.model.response.JobExecutionListResponse;
import com.greensnow.worker.model.response.JobExecutionResponse;
import com.greensnow.worker.model.response.JobExtensionDetail;
import com.greensnow.worker.model.response.JobExtensionListResponse;
import com.greensnow.worker.model.response.JobExtensionResponse;
import com.greensnow.worker.model.response.LiveMonitorResponse;
import com.greensnow.worker.model.response.LocationHistoryResponse;
import com.greensnow.worker.model.response.MilestoneListResponse;
import com.greensnow.worker.model.response.MilestoneResponse;
import com.greensnow.worker.model.response.TrackingNotificationListResponse;
import com.greensnow.worker.model.response.WorkSessionListResponse;
import com.greensnow.worker.repository.EmployeeProfileRepository;
import com.greensnow.worker.repository.EmployerProfileRepository;
import com.greensnow.worker.repository.JobExecutionRepository;
import com.greensnow.worker.repository.JobExtensionRepository;
import com.greensnow.worker.repository.JobMilestoneRepository;
import com.greensnow.worker.repository.JobRepository;
import com.greensnow.worker.repository.LocationTrackingRepository;
import com.greensnow.worker.repository.TrackingNotificationRepository;
import com.greensnow.worker.repository.UserRepository;
import com.greensnow.worker.repository.WorkSessionRepository;
import com.greensnow.worker.util.GeoUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackingService {

    // the radius used when a company pinned a site but never said how big it is. two hundred
    // metres covers a house, a yard and the street outside it, which is close enough that a
    // worker standing at the gate is not reported as somewhere else
    private static final int DEFAULT_GEOFENCE_RADIUS_METERS = 200;

    // nobody extends a day's work by more than a day. a figure beyond this is a typo on a
    // phone keypad, and the company would be answering a request for a cost nobody meant
    private static final int MAX_EXTENSION_HOURS = 24;

    private static final int MINUTES_PER_HOUR = 60;
    private static final int METERS_PER_KILOMETER = 1000;

    private final JobExecutionRepository jobExecutionRepository;
    private final JobRepository jobRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final UserRepository userRepository;
    private final LocationTrackingRepository locationTrackingRepository;
    private final WorkSessionRepository workSessionRepository;
    private final JobExtensionRepository jobExtensionRepository;
    private final JobMilestoneRepository jobMilestoneRepository;
    private final TrackingNotificationRepository trackingNotificationRepository;
    private final TrackingRequestBuilder trackingRequestBuilder;
    private final TrackingResponseProcessor trackingResponseProcessor;

    // ------------------------------------------------------------------------------------
    // the worker's own jobs
    // ------------------------------------------------------------------------------------

    public JobExecutionListResponse listExecutions(String userId, boolean liveOnly) {
        try {
            Optional<EmployeeProfile> profile = employeeProfileRepository.findByUserId(userId);
            if (profile.isEmpty()) {
                return trackingResponseProcessor.failedExecutionList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            String employeeProfileId = profile.get().getId();
            List<JobExecution> executions = liveOnly
                    ? jobExecutionRepository.findByEmployeeProfileIdAndStatusInOrderByAcceptedAtDesc(
                    employeeProfileId, liveStatuses())
                    : jobExecutionRepository.findByEmployeeProfileIdOrderByAcceptedAtDesc(employeeProfileId);

            // the list is the worker's day at a glance, so the milestones and the open session
            // are deliberately not read per row. a worker with eight jobs on their phone would
            // otherwise cost sixteen more queries to render a screen they are only scanning
            Map<String, Job> jobsById = jobsFor(executions);
            List<JobExecutionDetail> entries = executions.stream()
                    .map(execution -> trackingResponseProcessor.toExecutionDetail(execution,
                            jobsById.get(execution.getJobId()), null, null, null,
                            hasPendingExtension(execution.getId())))
                    .toList();

            return trackingResponseProcessor.processExecutionListResponse(entries);
        } catch (Exception exception) {
            log.error("Failed to load the jobs for user {}", userId, exception);
            return trackingResponseProcessor.failedExecutionList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EXECUTIONS_RETRIEVAL_FAILED);
        }
    }

    public JobExecutionResponse getExecution(String userId, String executionId) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            return trackingResponseProcessor.processExecutionResponse(ResponseMessage.EXECUTION_RETRIEVED,
                    fullDetail(found.get()));
        } catch (Exception exception) {
            log.error("Failed to load execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExecution(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EXECUTION_RETRIEVAL_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------
    // the steps of the day
    // ------------------------------------------------------------------------------------

    // the moment the worker sets off is the moment the company can see them coming. nothing
    // is billed for travel by default, but the stretch is still recorded, so a job that was
    // quoted for an hour of work and took three because of the drive reads that way afterwards
    public JobExecutionResponse startTravel(String userId, String executionId, WorkEventRequest request) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            JobExecution execution = found.get();
            if (!ExecutionStatus.NOT_STARTED.equals(execution.getStatus())) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.TRAVEL_NOT_STARTABLE, execution.getStatus()));
            }

            Job job = jobRepository.findById(execution.getJobId()).orElse(null);
            LocalDateTime now = LocalDateTime.now();
            execution.setStatus(ExecutionStatus.TRAVELING_TO_SITE);
            execution.setTravelStartedAt(now);
            applyPosition(execution, job, request.getLatitude(), request.getLongitude(), now);

            // the execution is saved before the session, because the status is what every other
            // route reads to decide what is allowed next. a travel row with no status behind it
            // would leave the worker unable to arrive
            jobExecutionRepository.save(execution);
            openSession(execution, WorkType.TRAVEL, request.getLatitude(), request.getLongitude(),
                    false, false, request.getNotes());

            notifyEmployer(job, execution, TrackingNotificationType.WORKER_EN_ROUTE,
                    "Worker on the way", workerNameFor(execution) + " has set off for " + titleOf(job), false);

            return trackingResponseProcessor.processExecutionResponse(ResponseMessage.TRAVEL_STARTED,
                    fullDetail(execution));
        } catch (Exception exception) {
            log.error("Failed to start travel on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExecution(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.TRAVEL_START_FAILED);
        }
    }

    // arriving away from the site is recorded rather than refused. a large site, a yard entrance
    // round the back and plain gps drift all read identically from here, and a worker standing
    // at the job unable to say so is a worker who cannot start. what the distance decides is
    // whether the arrival carries the verified flag, which is what the company actually reads
    public JobExecutionResponse recordArrival(String userId, String executionId, WorkEventRequest request) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            JobExecution execution = found.get();
            if (!ExecutionStatus.NOT_STARTED.equals(execution.getStatus())
                    && !ExecutionStatus.TRAVELING_TO_SITE.equals(execution.getStatus())) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.ARRIVAL_NOT_RECORDABLE, execution.getStatus()));
            }

            Job job = jobRepository.findById(execution.getJobId()).orElse(null);
            LocalDateTime now = LocalDateTime.now();
            Integer distanceMeters = distanceToSiteMeters(job, request.getLatitude(), request.getLongitude());
            boolean verified = Objects.nonNull(distanceMeters) && distanceMeters <= radiusOf(job);

            // the drive is closed off first so its minutes land on the travel total rather than
            // running on into the time the worker is stood on the site waiting to start
            closeOpenSession(execution, request.getLatitude(), request.getLongitude());

            execution.setStatus(ExecutionStatus.ON_SITE);
            execution.setArrivedAt(now);
            execution.setArrivalVerified(verified);
            applyPosition(execution, job, request.getLatitude(), request.getLongitude(), now);
            jobExecutionRepository.save(execution);

            notifyEmployer(job, execution, TrackingNotificationType.WORKER_ARRIVED,
                    "Worker on site", workerNameFor(execution) + " has arrived at " + titleOf(job)
                            + (verified ? "" : ", though their position is away from the site"), !verified);

            return trackingResponseProcessor.processExecutionResponse(
                    verified ? ResponseMessage.ARRIVAL_RECORDED : ResponseMessage.ARRIVAL_RECORDED_OFF_SITE,
                    fullDetail(execution));
        } catch (Exception exception) {
            log.error("Failed to record arrival on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExecution(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.ARRIVAL_FAILED);
        }
    }

    // starting work is where the finish time is set, and it is set from the moment the tools
    // come out rather than from the date on the job. a worker who was held up for two hours
    // is not overrunning the second they start, which is what a schedule read off the posting
    // would have said
    public JobExecutionResponse startWork(String userId, String executionId, WorkEventRequest request) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            JobExecution execution = found.get();
            if (!ExecutionStatus.ON_SITE.equals(execution.getStatus())) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.WORK_NOT_STARTABLE, execution.getStatus()));
            }

            Job job = jobRepository.findById(execution.getJobId()).orElse(null);
            LocalDateTime now = LocalDateTime.now();
            Integer distanceMeters = distanceToSiteMeters(job, request.getLatitude(), request.getLongitude());
            boolean verified = Objects.nonNull(distanceMeters) && distanceMeters <= radiusOf(job);

            execution.setStatus(ExecutionStatus.IN_PROGRESS);
            execution.setWorkStartedAt(now);
            execution.setScheduledEndAt(scheduledEndFrom(now, job));
            applyPosition(execution, job, request.getLatitude(), request.getLongitude(), now);
            jobExecutionRepository.save(execution);

            openSession(execution, WorkType.ACTIVE_WORK, request.getLatitude(), request.getLongitude(),
                    verified, false, request.getNotes());

            notifyEmployer(job, execution, TrackingNotificationType.WORK_STARTED,
                    "Work started", workerNameFor(execution) + " has started work on " + titleOf(job), false);

            return trackingResponseProcessor.processExecutionResponse(ResponseMessage.WORK_STARTED,
                    fullDetail(execution));
        } catch (Exception exception) {
            log.error("Failed to start work on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExecution(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.WORK_START_FAILED);
        }
    }

    // a break is recorded as its own stretch rather than by simply stopping the clock, because
    // a worker who took forty minutes for lunch and a worker who went home for two hours look
    // the same on a running total and nothing like each other on a timeline
    public JobExecutionResponse pauseWork(String userId, String executionId, WorkEventRequest request) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            JobExecution execution = found.get();
            if (!ExecutionStatus.IN_PROGRESS.equals(execution.getStatus())
                    && !ExecutionStatus.EXTENDED.equals(execution.getStatus())) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.WORK_NOT_PAUSABLE, execution.getStatus()));
            }

            Job job = jobRepository.findById(execution.getJobId()).orElse(null);
            LocalDateTime now = LocalDateTime.now();

            // the billable stretch is closed and paid before the break is opened. the other way
            // round leaves a moment where two sessions are open and the day is counted twice
            closeOpenSession(execution, request.getLatitude(), request.getLongitude());

            execution.setStatus(ExecutionStatus.PAUSED);
            applyPosition(execution, job, request.getLatitude(), request.getLongitude(), now);
            jobExecutionRepository.save(execution);

            openSession(execution, WorkType.BREAK, request.getLatitude(), request.getLongitude(),
                    false, false, request.getNotes());

            notifyEmployer(job, execution, TrackingNotificationType.WORK_PAUSED,
                    "Work paused", workerNameFor(execution) + " has paused work on " + titleOf(job), false);

            return trackingResponseProcessor.processExecutionResponse(ResponseMessage.WORK_PAUSED,
                    fullDetail(execution));
        } catch (Exception exception) {
            log.error("Failed to pause work on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExecution(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.WORK_PAUSE_FAILED);
        }
    }

    // coming back from a break on a job that has already been granted extra time returns to
    // EXTENDED rather than to IN_PROGRESS, so the stretch that follows is paid at the rate the
    // company agreed to for the overrun and not at the ordinary one
    public JobExecutionResponse resumeWork(String userId, String executionId, WorkEventRequest request) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            JobExecution execution = found.get();
            if (!ExecutionStatus.PAUSED.equals(execution.getStatus())) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.WORK_NOT_RESUMABLE, execution.getStatus()));
            }

            Job job = jobRepository.findById(execution.getJobId()).orElse(null);
            LocalDateTime now = LocalDateTime.now();
            Integer distanceMeters = distanceToSiteMeters(job, request.getLatitude(), request.getLongitude());
            boolean verified = Objects.nonNull(distanceMeters) && distanceMeters <= radiusOf(job);
            boolean onExtension = orZero(execution.getExtensionCount()) > 0;

            closeOpenSession(execution, request.getLatitude(), request.getLongitude());

            execution.setStatus(onExtension ? ExecutionStatus.EXTENDED : ExecutionStatus.IN_PROGRESS);
            applyPosition(execution, job, request.getLatitude(), request.getLongitude(), now);
            jobExecutionRepository.save(execution);

            openSession(execution, WorkType.ACTIVE_WORK, request.getLatitude(), request.getLongitude(),
                    verified, onExtension, request.getNotes());

            notifyEmployer(job, execution, TrackingNotificationType.WORK_RESUMED,
                    "Work resumed", workerNameFor(execution) + " has resumed work on " + titleOf(job), false);

            return trackingResponseProcessor.processExecutionResponse(ResponseMessage.WORK_RESUMED,
                    fullDetail(execution));
        } catch (Exception exception) {
            log.error("Failed to resume work on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExecution(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.WORK_RESUME_FAILED);
        }
    }

    // finishing closes whichever stretch was open, whatever it was. a worker who paused for a
    // delivery and then realised the job was done would otherwise leave a break running for
    // ever, and the day would never add up
    public JobExecutionResponse completeWork(String userId, String executionId, WorkEventRequest request) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            JobExecution execution = found.get();
            if (Objects.isNull(execution.getStatus()) || !execution.getStatus().isLive()
                    || ExecutionStatus.TRAVELING_TO_SITE.equals(execution.getStatus())) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.WORK_NOT_COMPLETABLE, execution.getStatus()));
            }

            Job job = jobRepository.findById(execution.getJobId()).orElse(null);
            LocalDateTime now = LocalDateTime.now();
            Integer distanceMeters = distanceToSiteMeters(job, request.getLatitude(), request.getLongitude());
            boolean verified = Objects.nonNull(distanceMeters) && distanceMeters <= radiusOf(job);

            closeOpenSession(execution, request.getLatitude(), request.getLongitude());

            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setCompletedAt(now);
            execution.setCompletionVerified(verified);
            // tracking goes off with the last step rather than being switched off later. the
            // job is what justified following somebody about, and it is over
            execution.setTrackingEnabled(false);
            applyPosition(execution, job, request.getLatitude(), request.getLongitude(), now);
            jobExecutionRepository.save(execution);

            notifyEmployer(job, execution, TrackingNotificationType.WORK_COMPLETED,
                    "Work completed", workerNameFor(execution) + " has finished " + titleOf(job)
                            + (verified ? "" : ", though their position is away from the site"), true);

            return trackingResponseProcessor.processExecutionResponse(
                    verified ? ResponseMessage.WORK_COMPLETED : ResponseMessage.WORK_COMPLETED_OFF_SITE,
                    fullDetail(execution));
        } catch (Exception exception) {
            log.error("Failed to complete execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExecution(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.WORK_COMPLETE_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------
    // the live location stream
    // ------------------------------------------------------------------------------------

    // every ping is kept rather than only the latest, because the trail is what answers the
    // questions that come later - whether somebody was on the site for the hours they billed,
    // and which way they went when a job on the other side of town overran
    public GeneralResponse recordLocation(String userId, String executionId, LocationPingRequest request) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            JobExecution execution = found.get();
            String rejection = locationRejection(execution, request);
            if (StringUtils.hasText(rejection)) {
                return trackingResponseProcessor.failedGeneral(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            Job job = jobRepository.findById(execution.getJobId()).orElse(null);
            Integer distanceMeters = distanceToSiteMeters(job, request.getLatitude(), request.getLongitude());
            boolean inside = insideGeofence(job, distanceMeters);
            boolean wasInside = execution.isInsideGeofence();

            // the point is written before the execution is touched. the trail is the record of
            // what happened and the live fields are only the last line of it, so a crash between
            // the two loses a cursor rather than losing where somebody was
            locationTrackingRepository.save(trackingRequestBuilder.buildLocation(execution.getId(), request,
                    locationTypeFor(execution, inside), distanceMeters, inside));

            applyPosition(execution, job, request.getLatitude(), request.getLongitude(), LocalDateTime.now());
            jobExecutionRepository.save(execution);

            // leaving the site mid-job is told once, on the crossing. a dashboard that repeated
            // it on every ping would bury the one that mattered under a hundred of the same
            if (wasInside && !inside && Objects.nonNull(execution.getArrivedAt())) {
                notifyEmployer(job, execution, TrackingNotificationType.GEOFENCE_EXIT,
                        "Worker has left the site",
                        workerNameFor(execution) + " has moved away from " + titleOf(job), true);
            }

            return trackingResponseProcessor.processGeneralResponse(ResponseMessage.LOCATION_RECORDED);
        } catch (Exception exception) {
            log.error("Failed to record a location on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.LOCATION_RECORD_FAILED);
        }
    }

    public LocationHistoryResponse getLocationHistory(String userId, String executionId) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedLocationHistory(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            return trackingResponseProcessor.processLocationHistoryResponse(executionId,
                    locationTrackingRepository.findByJobExecutionIdOrderByRecordedAtAsc(executionId));
        } catch (Exception exception) {
            log.error("Failed to load the location history on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedLocationHistory(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.LOCATION_HISTORY_RETRIEVAL_FAILED);
        }
    }

    public WorkSessionListResponse listSessions(String userId, String executionId) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedSessionList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            return trackingResponseProcessor.processSessionListResponse(executionId, found.get(),
                    workSessionRepository.findByJobExecutionIdOrderByStartedAtAsc(executionId));
        } catch (Exception exception) {
            log.error("Failed to load the sessions on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedSessionList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.SESSIONS_RETRIEVAL_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------
    // asking for more time
    // ------------------------------------------------------------------------------------

    // the whole point of the platform for a skilled trade: the work takes as long as it takes,
    // and a worker who finds a second problem behind the first can say so and be paid for it
    // rather than either walking off or working for nothing
    public JobExtensionResponse requestExtension(String userId, String executionId, ExtensionRequest request) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExtension(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            JobExecution execution = found.get();
            Job job = jobRepository.findById(execution.getJobId()).orElse(null);
            String rejection = extensionRejection(execution, job, request);
            if (StringUtils.hasText(rejection)) {
                return trackingResponseProcessor.failedExtension(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            int sequenceNumber = (int) jobExtensionRepository.countByJobExecutionId(executionId) + 1;
            JobExtension extension = jobExtensionRepository.save(trackingRequestBuilder.buildExtension(execution,
                    sequenceNumber, request.getRequestedHours(), request.getReason(),
                    request.getWorkCompletedPercentage()));

            // the company is told with the flag that puts the request in front of somebody. an
            // extension nobody answers expires in eight hours and the worker is left on a site
            // with no instruction, which is the failure this whole surface exists to avoid
            notifyEmployer(job, execution, TrackingNotificationType.EXTENSION_REQUESTED,
                    "More time requested", workerNameFor(execution) + " needs "
                            + request.getRequestedHours() + " more hours on " + titleOf(job), true);

            return trackingResponseProcessor.processExtensionResponse(ResponseMessage.EXTENSION_REQUESTED,
                    extension, execution.getJobId(), titleOf(job), workerNameFor(execution));
        } catch (Exception exception) {
            log.error("Failed to request an extension on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExtension(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EXTENSION_REQUEST_FAILED);
        }
    }

    public JobExtensionListResponse listExtensions(String userId, String executionId) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExtensionList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            JobExecution execution = found.get();
            Job job = jobRepository.findById(execution.getJobId()).orElse(null);
            String workerName = workerNameFor(execution);
            List<JobExtensionDetail> entries = jobExtensionRepository
                    .findByJobExecutionIdOrderBySequenceNumberAsc(executionId).stream()
                    .map(extension -> trackingResponseProcessor.toExtensionDetail(extension, execution.getJobId(),
                            titleOf(job), workerName))
                    .toList();

            return trackingResponseProcessor.processExtensionListResponse(entries);
        } catch (Exception exception) {
            log.error("Failed to load the extensions on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExtensionList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EXTENSIONS_RETRIEVAL_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------
    // the steps of the job
    // ------------------------------------------------------------------------------------

    public MilestoneResponse addMilestone(String userId, String executionId, MilestoneRequest request) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            String rejection = milestoneRejection(request);
            if (StringUtils.hasText(rejection)) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            int displayOrder = (int) jobMilestoneRepository.countByJobExecutionId(executionId) + 1;
            JobMilestone milestone = jobMilestoneRepository.save(
                    trackingRequestBuilder.buildMilestone(executionId, request, displayOrder));

            return trackingResponseProcessor.processMilestoneResponse(ResponseMessage.MILESTONE_ADDED, milestone);
        } catch (Exception exception) {
            log.error("Failed to add a step on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.MILESTONE_ADD_FAILED);
        }
    }

    public MilestoneListResponse listMilestones(String userId, String executionId) {
        try {
            Optional<JobExecution> found = workerExecution(userId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedMilestoneList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            return trackingResponseProcessor.processMilestoneListResponse(executionId,
                    jobMilestoneRepository.findByJobExecutionIdOrderByDisplayOrderAsc(executionId));
        } catch (Exception exception) {
            log.error("Failed to load the steps on execution {} for user {}", executionId, userId, exception);
            return trackingResponseProcessor.failedMilestoneList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.MILESTONES_RETRIEVAL_FAILED);
        }
    }

    public MilestoneResponse startMilestone(String userId, String executionId, String milestoneId) {
        try {
            Optional<JobMilestone> found = workerMilestone(userId, executionId, milestoneId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_MILESTONE_FOUND);
            }

            JobMilestone milestone = found.get();
            if (MilestoneStatus.COMPLETED.equals(milestone.getStatus())) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.MILESTONE_ALREADY_COMPLETED);
            }

            milestone.setStatus(MilestoneStatus.IN_PROGRESS);
            milestone.setStartedAt(LocalDateTime.now());
            // a step that was blocked and is now moving loses the reason it was stuck, because
            // leaving it there would have the company reading an excuse for work in progress
            milestone.setBlockedReason(null);

            return trackingResponseProcessor.processMilestoneResponse(ResponseMessage.MILESTONE_STARTED,
                    jobMilestoneRepository.save(milestone));
        } catch (Exception exception) {
            log.error("Failed to start step {} on execution {}", milestoneId, executionId, exception);
            return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.MILESTONE_START_FAILED);
        }
    }

    // a step is completed where the worker was standing when they finished it, and with a photo
    // if they took one. that is what turns a tick into something the company can sign off
    // without driving out to look
    public MilestoneResponse completeMilestone(String userId, String executionId, String milestoneId,
                                               MilestoneCompletionRequest request) {
        try {
            Optional<JobMilestone> found = workerMilestone(userId, executionId, milestoneId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_MILESTONE_FOUND);
            }

            JobMilestone milestone = found.get();
            if (MilestoneStatus.COMPLETED.equals(milestone.getStatus())) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.MILESTONE_ALREADY_COMPLETED);
            }

            milestone.setStatus(MilestoneStatus.COMPLETED);
            milestone.setCompletedAt(LocalDateTime.now());
            milestone.setCompletedLatitude(request.getLatitude());
            milestone.setCompletedLongitude(request.getLongitude());
            milestone.setProofPhotoUrl(request.getProofPhotoUrl());
            milestone.setEmployeeNotes(request.getNotes());
            milestone.setBlockedReason(null);
            JobMilestone saved = jobMilestoneRepository.save(milestone);

            jobExecutionRepository.findById(executionId).ifPresent(execution ->
                    notifyEmployer(jobRepository.findById(execution.getJobId()).orElse(null), execution,
                            TrackingNotificationType.MILESTONE_COMPLETED, "Step completed",
                            workerNameFor(execution) + " has completed " + saved.getTitle(), false));

            return trackingResponseProcessor.processMilestoneResponse(ResponseMessage.MILESTONE_COMPLETED, saved);
        } catch (Exception exception) {
            log.error("Failed to complete step {} on execution {}", milestoneId, executionId, exception);
            return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.MILESTONE_COMPLETE_FAILED);
        }
    }

    // a blocked step is a worker standing still waiting on somebody else, which is the thing a
    // company most needs told and most rarely hears. it carries the reason because "blocked"
    // on its own cannot be acted on by anybody
    public MilestoneResponse blockMilestone(String userId, String executionId, String milestoneId,
                                            MilestoneCompletionRequest request) {
        try {
            Optional<JobMilestone> found = workerMilestone(userId, executionId, milestoneId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_MILESTONE_FOUND);
            }
            if (!StringUtils.hasText(request.getBlockedReason())) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.NO_BLOCKED_REASON_FOUND);
            }

            JobMilestone milestone = found.get();
            if (MilestoneStatus.COMPLETED.equals(milestone.getStatus())) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.MILESTONE_ALREADY_COMPLETED);
            }

            milestone.setStatus(MilestoneStatus.BLOCKED);
            milestone.setBlockedReason(request.getBlockedReason().trim());
            JobMilestone saved = jobMilestoneRepository.save(milestone);

            jobExecutionRepository.findById(executionId).ifPresent(execution ->
                    notifyEmployer(jobRepository.findById(execution.getJobId()).orElse(null), execution,
                            TrackingNotificationType.MILESTONE_COMPLETED, "Step blocked",
                            saved.getTitle() + " is held up: " + saved.getBlockedReason(), true));

            return trackingResponseProcessor.processMilestoneResponse(ResponseMessage.MILESTONE_BLOCKED, saved);
        } catch (Exception exception) {
            log.error("Failed to block step {} on execution {}", milestoneId, executionId, exception);
            return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.MILESTONE_BLOCK_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------
    // what each side has been told
    // ------------------------------------------------------------------------------------

    // the notifications are keyed on the signed in account rather than on a role, so the one
    // method answers a worker reading an approval and a company reading an arrival
    public TrackingNotificationListResponse listNotifications(String userId, boolean unreadOnly) {
        try {
            List<TrackingNotification> notifications = unreadOnly
                    ? trackingNotificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
                    : trackingNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);

            return trackingResponseProcessor.processNotificationListResponse(notifications);
        } catch (Exception exception) {
            log.error("Failed to load the notifications for user {}", userId, exception);
            return trackingResponseProcessor.failedNotificationList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.NOTIFICATIONS_RETRIEVAL_FAILED);
        }
    }

    public GeneralResponse markNotificationRead(String userId, String notificationId) {
        try {
            Optional<TrackingNotification> found =
                    trackingNotificationRepository.findByIdAndRecipientId(notificationId, userId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_NOTIFICATION_FOUND);
            }

            TrackingNotification notification = found.get();
            // reading it a second time does not move the stamp. when somebody first saw an
            // overrun warning is a question that gets asked after the fact
            if (Objects.isNull(notification.getReadAt())) {
                notification.setReadAt(LocalDateTime.now());
                trackingNotificationRepository.save(notification);
            }

            return trackingResponseProcessor.processGeneralResponse(ResponseMessage.NOTIFICATION_READ);
        } catch (Exception exception) {
            log.error("Failed to mark notification {} read for user {}", notificationId, userId, exception);
            return trackingResponseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.NOTIFICATION_READ_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------
    // the company's side - the monitor, and the decisions only they can make
    // ------------------------------------------------------------------------------------

    // the monitor. one screen answering "where is everybody right now", which is the whole
    // reason a company hires through a platform rather than ringing round. the jobs are read
    // once, the live executions once, and everything hanging off them once across the whole
    // set - a dashboard that reopens every few seconds cannot afford a query per worker on it
    public LiveMonitorResponse monitorLive(String userId) {
        try {
            Optional<EmployerProfile> profile = employerProfileRepository.findByUserId(userId);
            if (profile.isEmpty()) {
                return trackingResponseProcessor.failedMonitor(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            List<Job> jobs = jobRepository.findByEmployerProfileIdOrderByCreatedAtDesc(profile.get().getId());
            if (jobs.isEmpty()) {
                // a company with no jobs gets an empty board rather than a failure. nobody on
                // site is an answer, and the counters on it are all zero
                return trackingResponseProcessor.processMonitorResponse(List.of());
            }

            Map<String, Job> jobsById = trackingResponseProcessor.byId(jobs, Job::getId);
            List<JobExecution> executions = jobExecutionRepository
                    .findByJobIdInAndStatusInOrderByAcceptedAtDesc(List.copyOf(jobsById.keySet()), liveStatuses());

            return trackingResponseProcessor.processMonitorResponse(detailsFor(executions, jobsById));
        } catch (Exception exception) {
            log.error("Failed to build the live monitor for user: {}", userId, exception);
            return trackingResponseProcessor.failedMonitor(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.MONITOR_RETRIEVAL_FAILED);
        }
    }

    // the same picture without the live filter - every worker the company has ever put on a
    // job, which is what a finished week is read back from
    public JobExecutionListResponse listEmployerExecutions(String userId, boolean liveOnly) {
        try {
            Optional<EmployerProfile> profile = employerProfileRepository.findByUserId(userId);
            if (profile.isEmpty()) {
                return trackingResponseProcessor.failedExecutionList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            List<Job> jobs = jobRepository.findByEmployerProfileIdOrderByCreatedAtDesc(profile.get().getId());
            if (jobs.isEmpty()) {
                return trackingResponseProcessor.processExecutionListResponse(List.of());
            }

            Map<String, Job> jobsById = trackingResponseProcessor.byId(jobs, Job::getId);
            List<String> jobIds = List.copyOf(jobsById.keySet());
            List<JobExecution> executions = liveOnly
                    ? jobExecutionRepository.findByJobIdInAndStatusInOrderByAcceptedAtDesc(jobIds, liveStatuses())
                    : jobExecutionRepository.findByJobIdIn(jobIds);

            return trackingResponseProcessor.processExecutionListResponse(detailsFor(executions, jobsById));
        } catch (Exception exception) {
            log.error("Failed to list the executions for employer user: {}", userId, exception);
            return trackingResponseProcessor.failedExecutionList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EXECUTIONS_RETRIEVAL_FAILED);
        }
    }

    public JobExecutionResponse getEmployerExecution(String userId, String executionId) {
        try {
            Optional<JobExecution> execution = employerExecution(userId, executionId);
            if (execution.isEmpty()) {
                return trackingResponseProcessor.failedExecution(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            return trackingResponseProcessor.processExecutionResponse(ResponseMessage.EXECUTION_RETRIEVED,
                    fullDetail(execution.get()));
        } catch (Exception exception) {
            log.error("Failed to retrieve execution {} for employer user: {}", executionId, userId, exception);
            return trackingResponseProcessor.failedExecution(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EXECUTION_RETRIEVAL_FAILED);
        }
    }

    // the route the worker took, as the company sees it. it is the same trail the worker can
    // read of themselves - there is one record of where somebody was, and both sides are shown
    // the same one rather than each being given a version of it
    public LocationHistoryResponse getEmployerLocationHistory(String userId, String executionId) {
        try {
            Optional<JobExecution> execution = employerExecution(userId, executionId);
            if (execution.isEmpty()) {
                return trackingResponseProcessor.failedLocationHistory(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            return trackingResponseProcessor.processLocationHistoryResponse(executionId,
                    locationTrackingRepository.findByJobExecutionIdOrderByRecordedAtAsc(executionId));
        } catch (Exception exception) {
            log.error("Failed to retrieve the location history on {} for employer user: {}",
                    executionId, userId, exception);
            return trackingResponseProcessor.failedLocationHistory(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.LOCATION_HISTORY_RETRIEVAL_FAILED);
        }
    }

    // where the day went, broken into its stretches. this is what an invoice is argued from
    // when a company asks why six hours on site came back as four hours of work
    public WorkSessionListResponse listEmployerSessions(String userId, String executionId) {
        try {
            Optional<JobExecution> execution = employerExecution(userId, executionId);
            if (execution.isEmpty()) {
                return trackingResponseProcessor.failedSessionList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            return trackingResponseProcessor.processSessionListResponse(executionId, execution.get(),
                    workSessionRepository.findByJobExecutionIdOrderByStartedAtAsc(executionId));
        } catch (Exception exception) {
            log.error("Failed to list the sessions on {} for employer user: {}", executionId, userId, exception);
            return trackingResponseProcessor.failedSessionList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.SESSIONS_RETRIEVAL_FAILED);
        }
    }

    // every request for more time still waiting on an answer, across all of the company's work.
    // this is the queue that matters most on the whole employer surface: at the other end of
    // each row is somebody standing on a site who cannot carry on until it is answered
    public JobExtensionListResponse listPendingExtensions(String userId) {
        try {
            Optional<EmployerProfile> profile = employerProfileRepository.findByUserId(userId);
            if (profile.isEmpty()) {
                return trackingResponseProcessor.failedExtensionList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            List<Job> jobs = jobRepository.findByEmployerProfileIdOrderByCreatedAtDesc(profile.get().getId());
            if (jobs.isEmpty()) {
                return trackingResponseProcessor.processExtensionListResponse(List.of());
            }

            Map<String, Job> jobsById = trackingResponseProcessor.byId(jobs, Job::getId);
            List<JobExecution> executions = jobExecutionRepository.findByJobIdIn(List.copyOf(jobsById.keySet()));
            if (executions.isEmpty()) {
                return trackingResponseProcessor.processExtensionListResponse(List.of());
            }

            Map<String, JobExecution> executionsById =
                    trackingResponseProcessor.byId(executions, JobExecution::getId);
            List<JobExtension> extensions = jobExtensionRepository.findByJobExecutionIdInAndStatusOrderByRequestedAtAsc(
                    List.copyOf(executionsById.keySet()), ExtensionStatus.REQUESTED);

            // oldest first, deliberately. the newest-first ordering used everywhere else would
            // leave the worker who has been waiting longest waiting longer still every time
            // somebody else asks
            List<JobExtensionDetail> entries = extensions.stream()
                    .map(extension -> {
                        JobExecution execution = executionsById.get(extension.getJobExecutionId());
                        Job job = Objects.nonNull(execution) ? jobsById.get(execution.getJobId()) : null;
                        return trackingResponseProcessor.toExtensionDetail(extension,
                                Objects.nonNull(job) ? job.getId() : null, titleOf(job),
                                Objects.nonNull(execution) ? workerNameFor(execution) : null);
                    })
                    .toList();

            return trackingResponseProcessor.processExtensionListResponse(entries);
        } catch (Exception exception) {
            log.error("Failed to list the pending extensions for employer user: {}", userId, exception);
            return trackingResponseProcessor.failedExtensionList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EXTENSIONS_RETRIEVAL_FAILED);
        }
    }

    // granting the time. a company may grant fewer hours than were asked for, so what is written
    // is what they granted and never what was requested, and the worker is told the new finish
    // time rather than left to assume they got all of it
    public JobExtensionResponse approveExtension(String userId, String extensionId,
                                                 ExtensionDecisionRequest request) {
        try {
            Optional<JobExtension> found = jobExtensionRepository.findById(extensionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExtension(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXTENSION_FOUND);
            }

            JobExtension extension = found.get();
            Optional<JobExecution> owned = employerExecution(userId, extension.getJobExecutionId());
            if (owned.isEmpty()) {
                return trackingResponseProcessor.failedExtension(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXTENSION_FOUND);
            }

            String rejection = decisionRejection(extension, request, true);
            if (StringUtils.hasText(rejection)) {
                return trackingResponseProcessor.failedExtension(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            JobExecution execution = owned.get();
            Job job = jobRepository.findById(execution.getJobId()).orElse(null);

            // the hours granted fall back to the hours asked for, so a company that simply says
            // yes grants what was in front of them rather than nothing at all
            Integer approvedHours = Objects.nonNull(request) && Objects.nonNull(request.getApprovedHours())
                    ? request.getApprovedHours()
                    : extension.getRequestedHours();

            // the decision is written before the execution is moved. an approval the worker's
            // phone has not picked up yet is recoverable; an execution reading EXTENDED with no
            // row saying who granted it is an overrun nobody can account for when it is billed
            JobExtension saved = jobExtensionRepository.save(trackingRequestBuilder.buildExtensionApproval(
                    extension, execution, userId, approvedHours,
                    Objects.nonNull(request) ? request.getNotes() : null));

            execution.setStatus(ExecutionStatus.EXTENDED);
            execution.setExtensionCount(orZero(execution.getExtensionCount()) + 1);
            execution.setScheduledEndAt(saved.getNewEndAt());
            jobExecutionRepository.save(execution);

            notifyWorker(execution, TrackingNotificationType.EXTENSION_APPROVED, "More time approved",
                    String.format("You have %s more hour(s) on %s.", approvedHours, titleOf(job)), false);

            return trackingResponseProcessor.processExtensionResponse(ResponseMessage.EXTENSION_APPROVED, saved,
                    execution.getJobId(), titleOf(job), workerNameFor(execution));
        } catch (Exception exception) {
            log.error("Failed to approve extension {} for employer user: {}", extensionId, userId, exception);
            return trackingResponseProcessor.failedExtension(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EXTENSION_APPROVAL_FAILED);
        }
    }

    // refusing the time. the execution is deliberately left exactly as it was - the worker is
    // still on the clock on the original terms, and a refusal is an instruction to stop at the
    // agreed finish rather than to down tools where they stand
    public JobExtensionResponse rejectExtension(String userId, String extensionId,
                                                ExtensionDecisionRequest request) {
        try {
            Optional<JobExtension> found = jobExtensionRepository.findById(extensionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedExtension(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXTENSION_FOUND);
            }

            JobExtension extension = found.get();
            Optional<JobExecution> owned = employerExecution(userId, extension.getJobExecutionId());
            if (owned.isEmpty()) {
                return trackingResponseProcessor.failedExtension(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXTENSION_FOUND);
            }

            String rejection = decisionRejection(extension, request, false);
            if (StringUtils.hasText(rejection)) {
                return trackingResponseProcessor.failedExtension(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            JobExecution execution = owned.get();
            Job job = jobRepository.findById(execution.getJobId()).orElse(null);

            JobExtension saved = jobExtensionRepository.save(trackingRequestBuilder.buildExtensionRejection(
                    extension, userId,
                    Objects.nonNull(request) ? request.getReason() : null,
                    Objects.nonNull(request) ? request.getNotes() : null));

            notifyWorker(execution, TrackingNotificationType.EXTENSION_REJECTED, "More time not approved",
                    String.format("Your request for more time on %s was not approved.", titleOf(job)), false);

            return trackingResponseProcessor.processExtensionResponse(ResponseMessage.EXTENSION_REJECTED, saved,
                    execution.getJobId(), titleOf(job), workerNameFor(execution));
        } catch (Exception exception) {
            log.error("Failed to reject extension {} for employer user: {}", extensionId, userId, exception);
            return trackingResponseProcessor.failedExtension(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EXTENSION_REJECTION_FAILED);
        }
    }

    public MilestoneListResponse listEmployerMilestones(String userId, String executionId) {
        try {
            Optional<JobExecution> execution = employerExecution(userId, executionId);
            if (execution.isEmpty()) {
                return trackingResponseProcessor.failedMilestoneList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            return trackingResponseProcessor.processMilestoneListResponse(executionId,
                    jobMilestoneRepository.findByJobExecutionIdOrderByDisplayOrderAsc(executionId));
        } catch (Exception exception) {
            log.error("Failed to list the milestones on {} for employer user: {}", executionId, userId, exception);
            return trackingResponseProcessor.failedMilestoneList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.MILESTONES_RETRIEVAL_FAILED);
        }
    }

    // signing off a step. only a step the worker has reported finished can be approved - a
    // company ticking off work that was never said to be done is the one direction this record
    // must not be writable in, because that tick is what the worker is paid against
    public MilestoneResponse approveMilestone(String userId, String executionId, String milestoneId) {
        try {
            Optional<JobExecution> execution = employerExecution(userId, executionId);
            if (execution.isEmpty()) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EXECUTION_FOUND);
            }

            Optional<JobMilestone> found = jobMilestoneRepository.findByIdAndJobExecutionId(milestoneId, executionId);
            if (found.isEmpty()) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_MILESTONE_FOUND);
            }

            JobMilestone milestone = found.get();
            if (!MilestoneStatus.COMPLETED.equals(milestone.getStatus())) {
                return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.MILESTONE_NOT_APPROVABLE, milestone.getStatus()));
            }

            milestone.setEmployerApproved(true);
            milestone.setEmployerApprovedAt(LocalDateTime.now());

            return trackingResponseProcessor.processMilestoneResponse(ResponseMessage.MILESTONE_APPROVED,
                    jobMilestoneRepository.save(milestone));
        } catch (Exception exception) {
            log.error("Failed to approve milestone {} on {} for employer user: {}",
                    milestoneId, executionId, userId, exception);
            return trackingResponseProcessor.failedMilestone(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.MILESTONE_APPROVAL_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------
    // shared internals
    // ------------------------------------------------------------------------------------

    // nothing is written without a position. a ping carrying no coordinates is a row saying a
    // phone was awake and nothing more, and it would sit in the trail looking like evidence
    private String locationRejection(JobExecution execution, LocationPingRequest request) {
        if (Objects.isNull(request) || Objects.isNull(request.getLatitude())
                || Objects.isNull(request.getLongitude())
                || !GeoUtility.isValidLatitude(request.getLatitude())
                || !GeoUtility.isValidLongitude(request.getLongitude())) {
            return ResponseMessage.NO_LOCATION_FOUND;
        }
        // the day is over, and with it the right to know where this person is. tracking that
        // outlives the job is surveillance, so the stream closes the moment the work does
        if (Objects.nonNull(execution.getStatus()) && execution.getStatus().isTerminal()) {
            return ResponseMessage.TRACKING_CLOSED;
        }
        // and it does not open before the worker chooses to set off, either. being hired is not
        // consent to be followed from home on the morning of
        if (ExecutionStatus.NOT_STARTED.equals(execution.getStatus())) {
            return ResponseMessage.TRACKING_NOT_OPEN;
        }
        return null;
    }

    // what the company is being asked to agree to has to be a figure and a reason. a request for
    // "more time" carrying neither can only be answered by ringing the worker, which is the
    // phone call this whole flow exists to replace
    private String extensionRejection(JobExecution execution, Job job, ExtensionRequest request) {
        if (Objects.isNull(request) || Objects.isNull(request.getRequestedHours())) {
            return ResponseMessage.NO_EXTENSION_HOURS_FOUND;
        }
        if (request.getRequestedHours() <= 0 || request.getRequestedHours() > MAX_EXTENSION_HOURS) {
            return String.format(ResponseMessage.INVALID_EXTENSION_HOURS, MAX_EXTENSION_HOURS);
        }
        if (!StringUtils.hasText(request.getReason())) {
            return ResponseMessage.NO_EXTENSION_REASON_FOUND;
        }
        if (Objects.nonNull(request.getWorkCompletedPercentage())
                && (request.getWorkCompletedPercentage() < 0 || request.getWorkCompletedPercentage() > 100)) {
            return ResponseMessage.INVALID_WORK_COMPLETED_PERCENTAGE;
        }
        // there is nothing to extend until the work is actually under way. a job not yet started
        // is rescheduled, not extended, and the two are different conversations
        if (!ExecutionStatus.IN_PROGRESS.equals(execution.getStatus())
                && !ExecutionStatus.PAUSED.equals(execution.getStatus())
                && !ExecutionStatus.EXTENDED.equals(execution.getStatus())) {
            return String.format(ResponseMessage.EXTENSION_NOT_REQUESTABLE, execution.getStatus());
        }
        // one open request at a time. a second sent while the first is unanswered puts two
        // different finish times, at two different costs, in front of the company for one
        // afternoon
        if (hasPendingExtension(execution.getId())) {
            return ResponseMessage.EXTENSION_ALREADY_PENDING;
        }
        // the ceiling is the company's own, set when they posted the job. a worker who has used
        // it up is told so now rather than having the request sit unanswered until it expires
        int allowed = Objects.nonNull(job) ? orZero(job.getExtensionsAllowed()) : 0;
        if (orZero(execution.getExtensionCount()) >= allowed) {
            return String.format(ResponseMessage.EXTENSIONS_EXHAUSTED, allowed);
        }
        return null;
    }

    // a step with no title is a row nobody can read back. the weight is optional, but one
    // outside 1-100 makes the progress figure it feeds meaningless
    private String milestoneRejection(MilestoneRequest request) {
        if (Objects.isNull(request) || !StringUtils.hasText(request.getTitle())) {
            return ResponseMessage.NO_MILESTONE_TITLE_FOUND;
        }
        if (Objects.nonNull(request.getWeightPercentage())
                && (request.getWeightPercentage() <= 0 || request.getWeightPercentage() > 100)) {
            return ResponseMessage.INVALID_MILESTONE_WEIGHT;
        }
        return null;
    }

    // a decision is made once and only while the request is still live. a company answering an
    // eight-hour-old request the worker already gave up on would otherwise be committing them
    // to hours that were never worked, against a day that is already over
    private String decisionRejection(JobExtension extension, ExtensionDecisionRequest request, boolean approving) {
        if (!ExtensionStatus.REQUESTED.equals(extension.getStatus())) {
            return ResponseMessage.EXTENSION_ALREADY_ANSWERED;
        }
        if (Objects.nonNull(extension.getExpiresAt()) && extension.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ResponseMessage.EXTENSION_EXPIRED;
        }
        // granting more hours than were asked for is not generosity, it is a figure the worker
        // never proposed and the company was never shown a cost for
        if (approving && Objects.nonNull(request) && Objects.nonNull(request.getApprovedHours())
                && (request.getApprovedHours() <= 0
                || request.getApprovedHours() > orZero(extension.getRequestedHours()))) {
            return String.format(ResponseMessage.INVALID_APPROVED_HOURS, orZero(extension.getRequestedHours()));
        }
        return null;
    }

    // what a point on the trail means. the same coordinates are travel on the way there and a
    // step off site once the worker has arrived, and that difference is the whole of what a
    // route map can be read for afterwards
    private LocationType locationTypeFor(JobExecution execution, boolean inside) {
        if (ExecutionStatus.TRAVELING_TO_SITE.equals(execution.getStatus())) {
            return LocationType.IN_TRANSIT;
        }
        return inside ? LocationType.AT_SITE : LocationType.OFF_SITE_PAUSE;
    }

    // a step is always reached through the execution it belongs to, so a worker cannot touch a
    // milestone on somebody else's job by guessing at its id
    private Optional<JobMilestone> workerMilestone(String userId, String executionId, String milestoneId) {
        return workerExecution(userId, executionId)
                .flatMap(execution -> jobMilestoneRepository.findByIdAndJobExecutionId(milestoneId, executionId));
    }

    // the batch read behind every employer list. the workers, the steps, the open sessions and
    // the pending extensions are each one query across the whole set rather than one per row,
    // because the monitor is the screen that reopens constantly and a per-worker read there is
    // a board that gets slower every time the company hires somebody
    private List<JobExecutionDetail> detailsFor(List<JobExecution> executions, Map<String, Job> jobsById) {
        if (executions.isEmpty()) {
            return List.of();
        }

        List<String> executionIds = executions.stream().map(JobExecution::getId).toList();
        List<String> profileIds = executions.stream()
                .map(JobExecution::getEmployeeProfileId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, String> userIdByProfileId = employeeProfileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(EmployeeProfile::getId, EmployeeProfile::getUserId,
                        (first, second) -> first));
        Map<String, User> workersByUserId = trackingResponseProcessor.byId(
                userRepository.findAllById(userIdByProfileId.values().stream().distinct().toList()), User::getId);

        Map<String, List<JobMilestone>> milestonesByExecution =
                jobMilestoneRepository.findByJobExecutionIdIn(executionIds).stream()
                        .collect(Collectors.groupingBy(JobMilestone::getJobExecutionId));

        // the stretch a worker is in the middle of right now, which is what the board reads as
        // "working" or "on a break". the newest open one wins if an older one was left hanging
        Map<String, WorkSession> openSessionByExecution =
                workSessionRepository.findByJobExecutionIdInOrderByStartedAtAsc(executionIds).stream()
                        .filter(session -> Objects.isNull(session.getEndedAt()))
                        .collect(Collectors.toMap(WorkSession::getJobExecutionId, session -> session,
                                (first, second) -> second));

        Map<String, List<JobExtension>> pendingByExecution =
                jobExtensionRepository.findByJobExecutionIdInAndStatusOrderByRequestedAtAsc(
                                executionIds, ExtensionStatus.REQUESTED).stream()
                        .collect(Collectors.groupingBy(JobExtension::getJobExecutionId));

        return executions.stream()
                .map(execution -> trackingResponseProcessor.toExecutionDetail(
                        execution,
                        jobsById.get(execution.getJobId()),
                        workersByUserId.get(userIdByProfileId.get(execution.getEmployeeProfileId())),
                        milestonesByExecution.getOrDefault(execution.getId(), List.of()),
                        openSessionByExecution.get(execution.getId()),
                        pendingByExecution.containsKey(execution.getId())))
                .toList();
    }

    // a worker is only ever handed their own assignment. an id belonging to somebody else is
    // answered as missing rather than refused, because "that is not yours" tells a stranger
    // the record exists
    private Optional<JobExecution> workerExecution(String userId, String executionId) {
        return employeeProfileRepository.findByUserId(userId)
                .flatMap(profile -> jobExecutionRepository.findByIdAndEmployeeProfileId(executionId, profile.getId()));
    }

    // and the same from the company's side, resolved through the job rather than through the
    // execution. a company owns the work, not the worker, so ownership is whether this job is
    // one of theirs
    private Optional<JobExecution> employerExecution(String userId, String executionId) {
        Optional<EmployerProfile> profile = employerProfileRepository.findByUserId(userId);
        if (profile.isEmpty()) {
            return Optional.empty();
        }

        return jobExecutionRepository.findById(executionId)
                .filter(execution -> jobRepository.findById(execution.getJobId())
                        .map(job -> profile.get().getId().equals(job.getEmployerProfileId()))
                        .orElse(false));
    }

    private List<ExecutionStatus> liveStatuses() {
        return Arrays.stream(ExecutionStatus.values()).filter(ExecutionStatus::isLive).toList();
    }

    private Map<String, Job> jobsFor(List<JobExecution> executions) {
        List<String> jobIds = executions.stream().map(JobExecution::getJobId).distinct().toList();
        return trackingResponseProcessor.byId(jobRepository.findAllById(jobIds), Job::getId);
    }

    // the single view of one execution, with everything on it. it is read in full here because
    // this is the screen a worker or a manager is actually looking at, which is the one place
    // the extra queries are worth paying for
    private JobExecutionDetail fullDetail(JobExecution execution) {
        Job job = jobRepository.findById(execution.getJobId()).orElse(null);
        User worker = workerFor(execution);
        List<JobMilestone> milestones =
                jobMilestoneRepository.findByJobExecutionIdOrderByDisplayOrderAsc(execution.getId());
        WorkSession currentSession = workSessionRepository
                .findFirstByJobExecutionIdAndEndedAtIsNullOrderByStartedAtDesc(execution.getId())
                .orElse(null);

        return trackingResponseProcessor.toExecutionDetail(execution, job, worker, milestones, currentSession,
                hasPendingExtension(execution.getId()));
    }

    private User workerFor(JobExecution execution) {
        return employeeProfileRepository.findById(execution.getEmployeeProfileId())
                .flatMap(profile -> userRepository.findById(profile.getUserId()))
                .orElse(null);
    }

    private String workerNameFor(JobExecution execution) {
        User worker = workerFor(execution);
        if (Objects.isNull(worker)) {
            return "A worker";
        }
        String name = String.join(" ",
                StringUtils.hasText(worker.getFirstName()) ? worker.getFirstName() : "",
                StringUtils.hasText(worker.getLastName()) ? worker.getLastName() : "").trim();
        return StringUtils.hasText(name) ? name : "A worker";
    }

    private String titleOf(Job job) {
        return Objects.nonNull(job) && StringUtils.hasText(job.getTitle()) ? job.getTitle() : "the job";
    }

    private boolean hasPendingExtension(String executionId) {
        return jobExtensionRepository.countByJobExecutionIdAndStatus(executionId, ExtensionStatus.REQUESTED) > 0;
    }

    // metres from the pinned site, or null when there is nothing to measure against. the
    // distance is kept in metres rather than kilometres because a geofence is a doorstep
    // question and a figure of 0.05 km answers it worse than one of fifty
    private Integer distanceToSiteMeters(Job job, BigDecimal latitude, BigDecimal longitude) {
        if (Objects.isNull(job)) {
            return null;
        }
        BigDecimal distanceKm = GeoUtility.distanceKm(latitude, longitude,
                job.getSiteLatitude(), job.getSiteLongitude());
        if (Objects.isNull(distanceKm)) {
            return null;
        }
        return distanceKm.multiply(BigDecimal.valueOf(METERS_PER_KILOMETER))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int radiusOf(Job job) {
        if (Objects.isNull(job) || Objects.isNull(job.getSiteRadiusMeters()) || job.getSiteRadiusMeters() <= 0) {
            return DEFAULT_GEOFENCE_RADIUS_METERS;
        }
        return job.getSiteRadiusMeters();
    }

    // a worker cannot be outside a fence that was never drawn, so a job with no pinned site
    // reads as inside. what it does not get is the verified flag, which needs a real distance
    private boolean insideGeofence(Job job, Integer distanceMeters) {
        if (Objects.isNull(distanceMeters)) {
            return Objects.isNull(job) || Objects.isNull(job.getSiteLatitude()) || Objects.isNull(job.getSiteLongitude());
        }
        return distanceMeters <= radiusOf(job);
    }

    // every step that carries a position updates the live fields on the execution, so the
    // monitor has somewhere to read from even on a job whose phone sends no pings at all
    private void applyPosition(JobExecution execution, Job job, BigDecimal latitude, BigDecimal longitude,
                               LocalDateTime at) {
        if (Objects.isNull(latitude) || Objects.isNull(longitude)) {
            return;
        }
        Integer distanceMeters = distanceToSiteMeters(job, latitude, longitude);
        execution.setCurrentLatitude(latitude);
        execution.setCurrentLongitude(longitude);
        execution.setDistanceToSiteMeters(distanceMeters);
        execution.setInsideGeofence(insideGeofence(job, distanceMeters));
        execution.setLastLocationAt(at);
    }

    private LocalDateTime scheduledEndFrom(LocalDateTime startedAt, Job job) {
        if (Objects.isNull(job) || Objects.isNull(job.getEstimatedDurationHours())
                || job.getEstimatedDurationHours() <= 0) {
            // a job with no estimate on it has no finish time to overrun, so none is invented.
            // a made-up one would put workers on an overrun list for a deadline nobody set
            return null;
        }
        return startedAt.plusHours(job.getEstimatedDurationHours());
    }

    private WorkSession openSession(JobExecution execution, WorkType workType, BigDecimal latitude,
                                    BigDecimal longitude, boolean geofenceVerified, boolean extension,
                                    String notes) {
        return workSessionRepository.save(trackingRequestBuilder.buildSession(execution.getId(), workType,
                latitude, longitude, geofenceVerified, extension, notes));
    }

    // closing a stretch is where the day's totals and the day's money are written. the minutes
    // land on the total that matches what the stretch was, and only a billable one is paid -
    // so a worker is never paid for a break and never has the break counted as work
    private void closeOpenSession(JobExecution execution, BigDecimal latitude, BigDecimal longitude) {
        Optional<WorkSession> open = workSessionRepository
                .findFirstByJobExecutionIdAndEndedAtIsNullOrderByStartedAtDesc(execution.getId());
        if (open.isEmpty()) {
            return;
        }

        WorkSession session = open.get();
        LocalDateTime now = LocalDateTime.now();
        int minutes = Objects.nonNull(session.getStartedAt())
                ? (int) Duration.between(session.getStartedAt(), now).toMinutes()
                : 0;

        session.setEndedAt(now);
        session.setDurationMinutes(minutes);
        session.setEndLatitude(latitude);
        session.setEndLongitude(longitude);

        WorkType workType = session.getWorkType();
        if (WorkType.TRAVEL.equals(workType)) {
            execution.setTotalTravelMinutes(orZero(execution.getTotalTravelMinutes()) + minutes);
        } else if (WorkType.BREAK.equals(workType) || WorkType.WAITING.equals(workType)) {
            execution.setTotalBreakMinutes(orZero(execution.getTotalBreakMinutes()) + minutes);
        } else {
            execution.setTotalWorkedMinutes(orZero(execution.getTotalWorkedMinutes()) + minutes);
        }

        if (Objects.nonNull(workType) && workType.isBillable()) {
            BigDecimal earnings = earningsFor(execution.getAgreedRate(), minutes, session.getRateMultiplier());
            session.setEarnings(earnings);
            // the overrun is kept in its own pot rather than folded into the base. a company
            // asked to pay for extra time is entitled to see what the extra time cost on its own
            if (session.isExtension()) {
                execution.setExtensionEarnings(sum(execution.getExtensionEarnings(), earnings));
            } else {
                execution.setBaseEarnings(sum(execution.getBaseEarnings(), earnings));
            }
        }

        workSessionRepository.save(session);
    }

    private BigDecimal earningsFor(BigDecimal agreedRate, int minutes, BigDecimal rateMultiplier) {
        if (Objects.isNull(agreedRate) || minutes <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal multiplier = Objects.nonNull(rateMultiplier) ? rateMultiplier : BigDecimal.ONE;
        // the hours are carried to four places before the money is rounded to two, so a run of
        // short stretches does not quietly lose a worker a few minutes each time it is written
        return agreedRate.multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(MINUTES_PER_HOUR), 4, RoundingMode.HALF_UP)
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // a notification is written in its own try/catch so that a failure to tell somebody never
    // reads back as the step itself having failed. the arrival happened; a worker told their
    // arrival did not record would walk off a site they are standing on
    private void notifyEmployer(Job job, JobExecution execution, TrackingNotificationType notificationType,
                                String title, String body, boolean requiresAction) {
        try {
            if (Objects.isNull(job)) {
                return;
            }
            employerProfileRepository.findById(job.getEmployerProfileId()).ifPresent(profile ->
                    trackingNotificationRepository.save(trackingRequestBuilder.buildNotification(
                            profile.getUserId(), execution.getId(), notificationType, title, body, requiresAction)));
        } catch (Exception exception) {
            log.error("Failed to notify the company on execution {}", execution.getId(), exception);
        }
    }

    private void notifyWorker(JobExecution execution, TrackingNotificationType notificationType, String title,
                              String body, boolean requiresAction) {
        try {
            employeeProfileRepository.findById(execution.getEmployeeProfileId()).ifPresent(profile ->
                    trackingNotificationRepository.save(trackingRequestBuilder.buildNotification(
                            profile.getUserId(), execution.getId(), notificationType, title, body, requiresAction)));
        } catch (Exception exception) {
            log.error("Failed to notify the worker on execution {}", execution.getId(), exception);
        }
    }

    private BigDecimal sum(BigDecimal first, BigDecimal second) {
        BigDecimal left = Objects.nonNull(first) ? first : BigDecimal.ZERO;
        BigDecimal right = Objects.nonNull(second) ? second : BigDecimal.ZERO;
        return left.add(right);
    }

    private int orZero(Integer value) {
        return Objects.nonNull(value) ? value : 0;
    }
}
