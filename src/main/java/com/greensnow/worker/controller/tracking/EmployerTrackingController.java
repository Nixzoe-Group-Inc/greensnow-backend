package com.greensnow.worker.controller.tracking;

import com.greensnow.worker.model.request.ExtensionDecisionRequest;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.JobExecutionListResponse;
import com.greensnow.worker.model.response.JobExecutionResponse;
import com.greensnow.worker.model.response.JobExtensionListResponse;
import com.greensnow.worker.model.response.JobExtensionResponse;
import com.greensnow.worker.model.response.LiveMonitorResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// mounted under /employer, so the role gate in SecurityConfig already covers it. every route
// resolves the company from the signed in user and then checks the job is one of theirs, so a
// worker's token here could only ever reach their own employer's board by holding two accounts
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("employer/tracking")
@Tag(name = "Employer tracking", description = "The live monitor, and the decisions only the company can make")
public class EmployerTrackingController {

    private final TrackingService trackingService;

    // ------------------------------------------------------------------------------------
    // the monitor
    // ------------------------------------------------------------------------------------

    // the one screen the company actually watches: everybody who is out working right now, where
    // they are, how far from the site they are meant to be on, and who is waiting on an answer.
    // it is a plain poll rather than a socket - a board refreshed every few seconds is the same
    // picture at a fraction of what holding a connection open per company would cost
    @Operation(summary = "The live board: everybody out working right now")
    @GetMapping("monitor")
    public LiveMonitorResponse monitorLive(@AuthenticationPrincipal String userId) {
        log.info("Request for the live monitor by user: {}", userId);
        LiveMonitorResponse response = trackingService.monitorLive(userId);
        log.info("Response to the live monitor for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------
    // the company's workers
    // ------------------------------------------------------------------------------------

    @Operation(summary = "List the workers this company has put on its jobs")
    @GetMapping("executions")
    public JobExecutionListResponse listExecutions(@AuthenticationPrincipal String userId,
                                                   @RequestParam(required = false, defaultValue = "false")
                                                   boolean liveOnly) {
        log.info("Request to list the executions for employer user: {}", userId);
        JobExecutionListResponse response = trackingService.listEmployerExecutions(userId, liveOnly);
        log.info("Response to the execution list for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read one worker's job in full")
    @GetMapping("executions/{executionId}")
    public JobExecutionResponse getExecution(@AuthenticationPrincipal String userId,
                                             @PathVariable String executionId) {
        log.info("Request to read execution {} for employer user: {}", executionId, userId);
        JobExecutionResponse response = trackingService.getEmployerExecution(userId, executionId);
        log.info("Response to the execution read for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read the route a worker took")
    @GetMapping("executions/{executionId}/location")
    public LocationHistoryResponse getLocationHistory(@AuthenticationPrincipal String userId,
                                                      @PathVariable String executionId) {
        log.info("Request to read the location history on execution {} for employer user: {}", executionId, userId);
        LocationHistoryResponse response = trackingService.getEmployerLocationHistory(userId, executionId);
        log.info("Response to the location history for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read where a worker's day went")
    @GetMapping("executions/{executionId}/sessions")
    public WorkSessionListResponse listSessions(@AuthenticationPrincipal String userId,
                                                @PathVariable String executionId) {
        log.info("Request to list the sessions on execution {} for employer user: {}", executionId, userId);
        WorkSessionListResponse response = trackingService.listEmployerSessions(userId, executionId);
        log.info("Response to the session list for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------
    // answering a request for more time
    // ------------------------------------------------------------------------------------

    @Operation(summary = "Every request for more time still waiting on an answer")
    @GetMapping("extensions/pending")
    public JobExtensionListResponse listPendingExtensions(@AuthenticationPrincipal String userId) {
        log.info("Request to list the pending extensions for employer user: {}", userId);
        JobExtensionListResponse response = trackingService.listPendingExtensions(userId);
        log.info("Response to the pending extension list for employer user {}: {}",
                userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Grant the time")
    @PutMapping("extensions/{extensionId}/approve")
    public JobExtensionResponse approveExtension(@AuthenticationPrincipal String userId,
                                                 @PathVariable String extensionId,
                                                 @RequestBody ExtensionDecisionRequest request) {
        log.info("Request to approve extension {} by employer user: {}", extensionId, userId);
        JobExtensionResponse response = trackingService.approveExtension(userId, extensionId, request);
        log.info("Response to the extension approval for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Refuse the time")
    @PutMapping("extensions/{extensionId}/reject")
    public JobExtensionResponse rejectExtension(@AuthenticationPrincipal String userId,
                                                @PathVariable String extensionId,
                                                @RequestBody ExtensionDecisionRequest request) {
        log.info("Request to reject extension {} by employer user: {}", extensionId, userId);
        JobExtensionResponse response = trackingService.rejectExtension(userId, extensionId, request);
        log.info("Response to the extension rejection for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------
    // signing off the steps
    // ------------------------------------------------------------------------------------

    @Operation(summary = "List the steps of a worker's job")
    @GetMapping("executions/{executionId}/milestones")
    public MilestoneListResponse listMilestones(@AuthenticationPrincipal String userId,
                                                @PathVariable String executionId) {
        log.info("Request to list the steps on execution {} for employer user: {}", executionId, userId);
        MilestoneListResponse response = trackingService.listEmployerMilestones(userId, executionId);
        log.info("Response to the step list for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Sign off a finished step")
    @PutMapping("executions/{executionId}/milestones/{milestoneId}/approve")
    public MilestoneResponse approveMilestone(@AuthenticationPrincipal String userId,
                                              @PathVariable String executionId,
                                              @PathVariable String milestoneId) {
        log.info("Request to approve step {} on execution {} by employer user: {}",
                milestoneId, executionId, userId);
        MilestoneResponse response = trackingService.approveMilestone(userId, executionId, milestoneId);
        log.info("Response to the step approval for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------
    // what this company has been told
    // ------------------------------------------------------------------------------------

    // the notifications are keyed on the signed in account rather than on a role, so this is the
    // same service method the worker's side calls - what differs is who is asking
    @Operation(summary = "List what this company has been told")
    @GetMapping("notifications")
    public TrackingNotificationListResponse listNotifications(@AuthenticationPrincipal String userId,
                                                              @RequestParam(required = false, defaultValue = "false")
                                                              boolean unreadOnly) {
        log.info("Request to list the notifications of employer user: {}", userId);
        TrackingNotificationListResponse response = trackingService.listNotifications(userId, unreadOnly);
        log.info("Response to the notification list for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Mark one as read")
    @PutMapping("notifications/{notificationId}/read")
    public GeneralResponse markNotificationRead(@AuthenticationPrincipal String userId,
                                                @PathVariable String notificationId) {
        log.info("Request to mark notification {} read for employer user: {}", notificationId, userId);
        GeneralResponse response = trackingService.markNotificationRead(userId, notificationId);
        log.info("Response to the notification read for employer user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }
}
