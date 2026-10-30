package com.greensnow.worker.controller.tracking;

import com.greensnow.worker.model.request.ExtensionRequest;
import com.greensnow.worker.model.request.LocationPingRequest;
import com.greensnow.worker.model.request.MilestoneCompletionRequest;
import com.greensnow.worker.model.request.MilestoneRequest;
import com.greensnow.worker.model.request.WorkEventRequest;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.JobExecutionListResponse;
import com.greensnow.worker.model.response.JobExecutionResponse;
import com.greensnow.worker.model.response.JobExtensionListResponse;
import com.greensnow.worker.model.response.JobExtensionResponse;
import com.greensnow.worker.model.response.LocationHistoryResponse;
import com.greensnow.worker.model.response.MilestoneListResponse;
import com.greensnow.worker.model.response.MilestoneResponse;
import com.greensnow.worker.model.response.TrackingNotificationListResponse;
import com.greensnow.worker.model.response.WorkSessionListResponse;
import com.greensnow.worker.service.tracking.TrackingService;
import com.greensnow.worker.util.JsonUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// mounted under /employee so the role gate in SecurityConfig already covers it - every route
// here answers off the signed in worker's own profile, and an employer's token reaching it
// would be reading somebody else's day
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("employee/tracking")
@Tag(name = "Worker tracking", description = "The worker's day: travel, arrival, the clock, more time and the steps")
public class WorkerTrackingController {

    private final TrackingService trackingService;

    // ------------------------------------------------------------------------------------
    // the worker's own jobs
    // ------------------------------------------------------------------------------------

    @Operation(summary = "List the jobs this worker has been put on")
    @GetMapping("executions")
    public JobExecutionListResponse listExecutions(@AuthenticationPrincipal String userId,
                                                   @RequestParam(required = false, defaultValue = "false")
                                                   boolean liveOnly) {
        log.info("Request to list the executions of user: {}", userId);
        JobExecutionListResponse response = trackingService.listExecutions(userId, liveOnly);
        log.info("Response to the execution list for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read one job this worker is on")
    @GetMapping("executions/{executionId}")
    public JobExecutionResponse getExecution(@AuthenticationPrincipal String userId,
                                             @PathVariable String executionId) {
        log.info("Request to read execution {} for user: {}", executionId, userId);
        JobExecutionResponse response = trackingService.getExecution(userId, executionId);
        log.info("Response to the execution read for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------
    // the steps of the day
    // ------------------------------------------------------------------------------------

    @Operation(summary = "Set off for the site")
    @PostMapping("executions/{executionId}/travel")
    public JobExecutionResponse startTravel(@AuthenticationPrincipal String userId,
                                            @PathVariable String executionId,
                                            @RequestBody WorkEventRequest request) {
        log.info("Request to start travel on execution {} for user: {}", executionId, userId);
        JobExecutionResponse response = trackingService.startTravel(userId, executionId, request);
        log.info("Response to the travel start for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Record arriving at the site")
    @PostMapping("executions/{executionId}/arrive")
    public JobExecutionResponse recordArrival(@AuthenticationPrincipal String userId,
                                              @PathVariable String executionId,
                                              @RequestBody WorkEventRequest request) {
        log.info("Request to record arrival on execution {} for user: {}", executionId, userId);
        JobExecutionResponse response = trackingService.recordArrival(userId, executionId, request);
        log.info("Response to the arrival for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Start the work")
    @PostMapping("executions/{executionId}/start")
    public JobExecutionResponse startWork(@AuthenticationPrincipal String userId,
                                          @PathVariable String executionId,
                                          @RequestBody WorkEventRequest request) {
        log.info("Request to start work on execution {} for user: {}", executionId, userId);
        JobExecutionResponse response = trackingService.startWork(userId, executionId, request);
        log.info("Response to the work start for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Pause the work")
    @PostMapping("executions/{executionId}/pause")
    public JobExecutionResponse pauseWork(@AuthenticationPrincipal String userId,
                                          @PathVariable String executionId,
                                          @RequestBody WorkEventRequest request) {
        log.info("Request to pause work on execution {} for user: {}", executionId, userId);
        JobExecutionResponse response = trackingService.pauseWork(userId, executionId, request);
        log.info("Response to the work pause for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Pick the work back up")
    @PostMapping("executions/{executionId}/resume")
    public JobExecutionResponse resumeWork(@AuthenticationPrincipal String userId,
                                           @PathVariable String executionId,
                                           @RequestBody WorkEventRequest request) {
        log.info("Request to resume work on execution {} for user: {}", executionId, userId);
        JobExecutionResponse response = trackingService.resumeWork(userId, executionId, request);
        log.info("Response to the work resume for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Finish the work")
    @PostMapping("executions/{executionId}/complete")
    public JobExecutionResponse completeWork(@AuthenticationPrincipal String userId,
                                             @PathVariable String executionId,
                                             @RequestBody WorkEventRequest request) {
        log.info("Request to complete work on execution {} for user: {}", executionId, userId);
        JobExecutionResponse response = trackingService.completeWork(userId, executionId, request);
        log.info("Response to the work completion for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------
    // the live location stream
    // ------------------------------------------------------------------------------------

    // the one route on the whole platform called on a timer rather than by somebody pressing
    // something, so it is deliberately the cheapest: it answers an outcome and nothing else,
    // because a handset posting every few seconds has no use for the execution back each time
    @Operation(summary = "Report where the worker is now")
    @PostMapping("executions/{executionId}/location")
    public GeneralResponse recordLocation(@AuthenticationPrincipal String userId,
                                          @PathVariable String executionId,
                                          @RequestBody LocationPingRequest request) {
        GeneralResponse response = trackingService.recordLocation(userId, executionId, request);
        log.info("Response to the location ping on execution {} for user {}: {}",
                executionId, userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read the route this worker took")
    @GetMapping("executions/{executionId}/location")
    public LocationHistoryResponse getLocationHistory(@AuthenticationPrincipal String userId,
                                                      @PathVariable String executionId) {
        log.info("Request to read the location history on execution {} for user: {}", executionId, userId);
        LocationHistoryResponse response = trackingService.getLocationHistory(userId, executionId);
        log.info("Response to the location history for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read where the day went")
    @GetMapping("executions/{executionId}/sessions")
    public WorkSessionListResponse listSessions(@AuthenticationPrincipal String userId,
                                                @PathVariable String executionId) {
        log.info("Request to list the sessions on execution {} for user: {}", executionId, userId);
        WorkSessionListResponse response = trackingService.listSessions(userId, executionId);
        log.info("Response to the session list for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------
    // asking for more time
    // ------------------------------------------------------------------------------------

    @Operation(summary = "Ask the company for more time on this job")
    @PostMapping("executions/{executionId}/extensions")
    public JobExtensionResponse requestExtension(@AuthenticationPrincipal String userId,
                                                 @PathVariable String executionId,
                                                 @RequestBody ExtensionRequest request) {
        log.info("Request for more time on execution {} by user: {}", executionId, userId);
        JobExtensionResponse response = trackingService.requestExtension(userId, executionId, request);
        log.info("Response to the extension request for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "List the requests for more time made on this job")
    @GetMapping("executions/{executionId}/extensions")
    public JobExtensionListResponse listExtensions(@AuthenticationPrincipal String userId,
                                                   @PathVariable String executionId) {
        log.info("Request to list the extensions on execution {} for user: {}", executionId, userId);
        JobExtensionListResponse response = trackingService.listExtensions(userId, executionId);
        log.info("Response to the extension list for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------
    // the steps of the job
    // ------------------------------------------------------------------------------------

    @Operation(summary = "Add a step to this job")
    @PostMapping("executions/{executionId}/milestones")
    public MilestoneResponse addMilestone(@AuthenticationPrincipal String userId,
                                          @PathVariable String executionId,
                                          @RequestBody MilestoneRequest request) {
        log.info("Request to add a step on execution {} for user: {}", executionId, userId);
        MilestoneResponse response = trackingService.addMilestone(userId, executionId, request);
        log.info("Response to the step creation for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "List the steps of this job")
    @GetMapping("executions/{executionId}/milestones")
    public MilestoneListResponse listMilestones(@AuthenticationPrincipal String userId,
                                                @PathVariable String executionId) {
        log.info("Request to list the steps on execution {} for user: {}", executionId, userId);
        MilestoneListResponse response = trackingService.listMilestones(userId, executionId);
        log.info("Response to the step list for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Start a step")
    @PutMapping("executions/{executionId}/milestones/{milestoneId}/start")
    public MilestoneResponse startMilestone(@AuthenticationPrincipal String userId,
                                            @PathVariable String executionId,
                                            @PathVariable String milestoneId) {
        log.info("Request to start step {} on execution {} for user: {}", milestoneId, executionId, userId);
        MilestoneResponse response = trackingService.startMilestone(userId, executionId, milestoneId);
        log.info("Response to the step start for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Finish a step")
    @PutMapping("executions/{executionId}/milestones/{milestoneId}/complete")
    public MilestoneResponse completeMilestone(@AuthenticationPrincipal String userId,
                                               @PathVariable String executionId,
                                               @PathVariable String milestoneId,
                                               @RequestBody MilestoneCompletionRequest request) {
        log.info("Request to complete step {} on execution {} for user: {}", milestoneId, executionId, userId);
        MilestoneResponse response = trackingService.completeMilestone(userId, executionId, milestoneId, request);
        log.info("Response to the step completion for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Report a step as held up")
    @PutMapping("executions/{executionId}/milestones/{milestoneId}/block")
    public MilestoneResponse blockMilestone(@AuthenticationPrincipal String userId,
                                            @PathVariable String executionId,
                                            @PathVariable String milestoneId,
                                            @RequestBody MilestoneCompletionRequest request) {
        log.info("Request to block step {} on execution {} for user: {}", milestoneId, executionId, userId);
        MilestoneResponse response = trackingService.blockMilestone(userId, executionId, milestoneId, request);
        log.info("Response to the step block for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------
    // what this worker has been told
    // ------------------------------------------------------------------------------------

    @Operation(summary = "List what this worker has been told")
    @GetMapping("notifications")
    public TrackingNotificationListResponse listNotifications(@AuthenticationPrincipal String userId,
                                                              @RequestParam(required = false, defaultValue = "false")
                                                              boolean unreadOnly) {
        log.info("Request to list the notifications of user: {}", userId);
        TrackingNotificationListResponse response = trackingService.listNotifications(userId, unreadOnly);
        log.info("Response to the notification list for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Mark one as read")
    @PutMapping("notifications/{notificationId}/read")
    public GeneralResponse markNotificationRead(@AuthenticationPrincipal String userId,
                                                @PathVariable String notificationId) {
        log.info("Request to mark notification {} read for user: {}", notificationId, userId);
        GeneralResponse response = trackingService.markNotificationRead(userId, notificationId);
        log.info("Response to the notification read for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }
}
