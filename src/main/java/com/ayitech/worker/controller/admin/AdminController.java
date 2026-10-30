package com.ayitech.worker.controller.admin;

import com.ayitech.worker.model.enums.JobStatus;
import com.ayitech.worker.model.enums.UserType;
import com.ayitech.worker.model.request.AccountStatusRequest;
import com.ayitech.worker.model.request.JobReviewRequest;
import com.ayitech.worker.model.request.SkillRequest;
import com.ayitech.worker.model.response.GeneralResponse;
import com.ayitech.worker.model.response.JobApprovalListResponse;
import com.ayitech.worker.model.response.JobListResponse;
import com.ayitech.worker.model.response.JobResponse;
import com.ayitech.worker.model.response.PlatformOverviewResponse;
import com.ayitech.worker.model.response.SkillListResponse;
import com.ayitech.worker.model.response.SkillResponse;
import com.ayitech.worker.model.response.UserListResponse;
import com.ayitech.worker.model.response.UserResponse;
import com.ayitech.worker.service.admin.AdminService;
import com.ayitech.worker.util.JsonUtility;
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

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("admin")
@Tag(name = "Admin", description = "The approval queue, the trade catalogue, the accounts and the verifications")
public class AdminController {

    private final AdminService adminService;

    // ------------------------------------------------------------------------------------------
    // the approval queue
    // ------------------------------------------------------------------------------------------

    @Operation(summary = "List the jobs waiting for approval, oldest first")
    @GetMapping("jobs/queue")
    public JobListResponse listApprovalQueue(@AuthenticationPrincipal String userId) {
        log.info("Request to load the approval queue for admin: {}", userId);
        JobListResponse response = adminService.listApprovalQueue(userId);
        log.info("Response to the approval queue request for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "List jobs across the platform, newest first")
    @GetMapping("jobs")
    public JobListResponse listJobs(@AuthenticationPrincipal String userId,
                                    @RequestParam(required = false) JobStatus status,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size) {
        log.info("Request to load jobs with status {} for admin: {}", status, userId);
        JobListResponse response = adminService.listJobs(userId, status, page, size);
        log.info("Response to the job listing request for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read a single job in full")
    @GetMapping("jobs/{jobId}")
    public JobResponse getJob(@AuthenticationPrincipal String userId, @PathVariable String jobId) {
        log.info("Request to load job {} for admin: {}", jobId, userId);
        JobResponse response = adminService.getJob(userId, jobId);
        log.info("Response to the job request for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Approve a job and open it to workers")
    @PostMapping("jobs/{jobId}/approve")
    public JobResponse approveJob(@AuthenticationPrincipal String userId, @PathVariable String jobId,
                                  @RequestBody JobReviewRequest request) {
        log.info("Request to approve job {} by admin: {}", jobId, userId);
        JobResponse response = adminService.approveJob(userId, jobId, request);
        log.info("Response to the approval of job {} by admin {}: {}", jobId, userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Turn a job down")
    @PostMapping("jobs/{jobId}/reject")
    public JobResponse rejectJob(@AuthenticationPrincipal String userId, @PathVariable String jobId,
                                 @RequestBody JobReviewRequest request) {
        log.info("Request to turn down job {} by admin: {}", jobId, userId);
        JobResponse response = adminService.rejectJob(userId, jobId, request);
        log.info("Response to the refusal of job {} by admin {}: {}", jobId, userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Send a job back to the company with changes")
    @PostMapping("jobs/{jobId}/request-changes")
    public JobResponse requestChanges(@AuthenticationPrincipal String userId, @PathVariable String jobId,
                                      @RequestBody JobReviewRequest request) {
        log.info("Request to send job {} back for changes by admin: {}", jobId, userId);
        JobResponse response = adminService.requestChanges(userId, jobId, request);
        log.info("Response to sending job {} back by admin {}: {}", jobId, userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read the approval history of a job")
    @GetMapping("jobs/{jobId}/approvals")
    public JobApprovalListResponse listJobApprovals(@AuthenticationPrincipal String userId,
                                                    @PathVariable String jobId) {
        log.info("Request to load the approval history of job {} for admin: {}", jobId, userId);
        JobApprovalListResponse response = adminService.listJobApprovals(userId, jobId);
        log.info("Response to the approval history request for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read the reviews this approver has made")
    @GetMapping("reviews")
    public JobApprovalListResponse listMyReviews(@AuthenticationPrincipal String userId) {
        log.info("Request to load the reviews of admin: {}", userId);
        JobApprovalListResponse response = adminService.listMyReviews(userId);
        log.info("Response to the reviews request for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------------
    // the trade catalogue
    // ------------------------------------------------------------------------------------------

    @Operation(summary = "Add a skill to the catalogue")
    @PostMapping("skills")
    public SkillResponse createSkill(@AuthenticationPrincipal String userId, @RequestBody SkillRequest request) {
        log.info("Request to add a skill by admin: {}", userId);
        SkillResponse response = adminService.createSkill(userId, request);
        log.info("Response to the skill creation by admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Update a skill in the catalogue")
    @PutMapping("skills/{skillId}")
    public SkillResponse updateSkill(@AuthenticationPrincipal String userId, @PathVariable String skillId,
                                     @RequestBody SkillRequest request) {
        log.info("Request to update skill {} by admin: {}", skillId, userId);
        SkillResponse response = adminService.updateSkill(userId, skillId, request);
        log.info("Response to the update of skill {} by admin {}: {}", skillId, userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "List the catalogue, retired skills included")
    @GetMapping("skills")
    public SkillListResponse listSkills(@AuthenticationPrincipal String userId,
                                        @RequestParam(required = false) String category) {
        log.info("Request to load the catalogue for admin: {}", userId);
        SkillListResponse response = adminService.listSkills(userId, category);
        log.info("Response to the catalogue request for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read a single skill")
    @GetMapping("skills/{skillId}")
    public SkillResponse getSkill(@AuthenticationPrincipal String userId, @PathVariable String skillId) {
        log.info("Request to load skill {} for admin: {}", skillId, userId);
        SkillResponse response = adminService.getSkill(userId, skillId);
        log.info("Response to the skill request for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Retire a skill so nothing new can be built around it")
    @PostMapping("skills/{skillId}/retire")
    public SkillResponse retireSkill(@AuthenticationPrincipal String userId, @PathVariable String skillId) {
        log.info("Request to retire skill {} by admin: {}", skillId, userId);
        SkillResponse response = adminService.retireSkill(userId, skillId);
        log.info("Response to retiring skill {} by admin {}: {}", skillId, userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Bring a retired skill back into the catalogue")
    @PostMapping("skills/{skillId}/restore")
    public SkillResponse restoreSkill(@AuthenticationPrincipal String userId, @PathVariable String skillId) {
        log.info("Request to restore skill {} by admin: {}", skillId, userId);
        SkillResponse response = adminService.restoreSkill(userId, skillId);
        log.info("Response to restoring skill {} by admin {}: {}", skillId, userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------------
    // the accounts
    // ------------------------------------------------------------------------------------------

    @Operation(summary = "List the accounts on the platform")
    @GetMapping("users")
    public UserListResponse listUsers(@AuthenticationPrincipal String userId,
                                      @RequestParam(required = false) UserType userType,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        log.info("Request to load accounts of type {} for admin: {}", userType, userId);
        UserListResponse response = adminService.listUsers(userId, userType, page, size);
        log.info("Response to the account listing for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read a single account")
    @GetMapping("users/{accountId}")
    public UserResponse getUser(@AuthenticationPrincipal String userId, @PathVariable String accountId) {
        log.info("Request to load account {} for admin: {}", accountId, userId);
        UserResponse response = adminService.getUser(userId, accountId);
        log.info("Response to the account request for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Suspend an account so it can no longer sign in")
    @PostMapping("users/{accountId}/suspend")
    public GeneralResponse suspendUser(@AuthenticationPrincipal String userId, @PathVariable String accountId,
                                       @RequestBody AccountStatusRequest request) {
        log.info("Request to suspend account {} by admin: {}", accountId, userId);
        GeneralResponse response = adminService.suspendUser(userId, accountId, request);
        log.info("Response to suspending account {} by admin {}: {}", accountId, userId,
                JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Restore a suspended account")
    @PostMapping("users/{accountId}/restore")
    public GeneralResponse restoreUser(@AuthenticationPrincipal String userId, @PathVariable String accountId) {
        log.info("Request to restore account {} by admin: {}", accountId, userId);
        GeneralResponse response = adminService.restoreUser(userId, accountId);
        log.info("Response to restoring account {} by admin {}: {}", accountId, userId, JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------------
    // verification
    // ------------------------------------------------------------------------------------------

    @Operation(summary = "Verify a company, or withdraw its verification")
    @PostMapping("employers/{accountId}/verification")
    public GeneralResponse setEmployerVerification(@AuthenticationPrincipal String userId,
                                                   @PathVariable String accountId,
                                                   @RequestParam boolean verified) {
        log.info("Request to set the verification of company {} to {} by admin: {}", accountId, verified, userId);
        GeneralResponse response = adminService.setEmployerVerification(userId, accountId, verified);
        log.info("Response to the verification of company {} by admin {}: {}", accountId, userId,
                JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Let a verified company's jobs skip the approval queue, or stop them doing so")
    @PostMapping("employers/{accountId}/auto-approval")
    public GeneralResponse setEmployerAutoApproval(@AuthenticationPrincipal String userId,
                                                   @PathVariable String accountId,
                                                   @RequestParam boolean autoApprove) {
        log.info("Request to set auto approval for company {} to {} by admin: {}", accountId, autoApprove, userId);
        GeneralResponse response = adminService.setEmployerAutoApproval(userId, accountId, autoApprove);
        log.info("Response to the auto approval change for company {} by admin {}: {}", accountId, userId,
                JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Verify a worker's identity, or withdraw it")
    @PostMapping("workers/{accountId}/identity")
    public GeneralResponse setWorkerIdVerification(@AuthenticationPrincipal String userId,
                                                   @PathVariable String accountId,
                                                   @RequestParam boolean verified) {
        log.info("Request to set the identity verification of worker {} to {} by admin: {}",
                accountId, verified, userId);
        GeneralResponse response = adminService.setWorkerIdVerification(userId, accountId, verified);
        log.info("Response to the identity verification of worker {} by admin {}: {}", accountId, userId,
                JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Record a worker's background check, or withdraw it")
    @PostMapping("workers/{accountId}/background-check")
    public GeneralResponse setWorkerBackgroundCheck(@AuthenticationPrincipal String userId,
                                                    @PathVariable String accountId,
                                                    @RequestParam boolean checked) {
        log.info("Request to set the background check of worker {} to {} by admin: {}", accountId, checked, userId);
        GeneralResponse response = adminService.setWorkerBackgroundCheck(userId, accountId, checked);
        log.info("Response to the background check of worker {} by admin {}: {}", accountId, userId,
                JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Verify a worker's certification in one trade, or withdraw it")
    @PostMapping("worker-skills/{employeeSkillId}/verification")
    public GeneralResponse setWorkerSkillVerification(@AuthenticationPrincipal String userId,
                                                      @PathVariable String employeeSkillId,
                                                      @RequestParam boolean verified) {
        log.info("Request to set the verification of certification {} to {} by admin: {}",
                employeeSkillId, verified, userId);
        GeneralResponse response = adminService.setWorkerSkillVerification(userId, employeeSkillId, verified);
        log.info("Response to the verification of certification {} by admin {}: {}", employeeSkillId, userId,
                JsonUtility.toJson(response));
        return response;
    }

    // ------------------------------------------------------------------------------------------
    // the overview
    // ------------------------------------------------------------------------------------------

    @Operation(summary = "Read the platform overview")
    @GetMapping("overview")
    public PlatformOverviewResponse getOverview(@AuthenticationPrincipal String userId) {
        log.info("Request to load the platform overview for admin: {}", userId);
        PlatformOverviewResponse response = adminService.getOverview(userId);
        log.info("Response to the overview request for admin {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }
}
