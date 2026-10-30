package com.ayitech.worker.controller.employer;

import com.ayitech.worker.model.enums.ApplicationStatus;
import com.ayitech.worker.model.enums.JobStatus;
import com.ayitech.worker.model.request.ApplicationDecisionRequest;
import com.ayitech.worker.model.request.EmployerProfileRequest;
import com.ayitech.worker.model.request.JobRequest;
import com.ayitech.worker.model.response.ApplicantListResponse;
import com.ayitech.worker.model.response.ApplicantResponse;
import com.ayitech.worker.model.response.EmployerProfileResponse;
import com.ayitech.worker.model.response.GeneralResponse;
import com.ayitech.worker.model.response.JobApprovalListResponse;
import com.ayitech.worker.model.response.JobListResponse;
import com.ayitech.worker.model.response.JobResponse;
import com.ayitech.worker.service.employer.EmployerService;
import com.ayitech.worker.util.JsonUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("employer")
@Tag(name = "Employer", description = "The company's own profile, the jobs it posts and the workers who apply")
public class EmployerController {

    private final EmployerService employerService;

    @Operation(summary = "Create the company's profile")
    @PostMapping("profile")
    public EmployerProfileResponse createProfile(@AuthenticationPrincipal String userId,
                                                 @RequestBody EmployerProfileRequest request) {
        log.info("Request to create an employer profile for user: {}", userId);
        EmployerProfileResponse response = employerService.createProfile(userId, request);
        log.info("Response to the employer profile creation for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read the company's profile")
    @GetMapping("profile")
    public EmployerProfileResponse getProfile(@AuthenticationPrincipal String userId) {
        log.info("Request to read the employer profile of user: {}", userId);
        EmployerProfileResponse response = employerService.getProfile(userId);
        log.info("Response to the employer profile read for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Update the company's profile")
    @PutMapping("profile")
    public EmployerProfileResponse updateProfile(@AuthenticationPrincipal String userId,
                                                 @RequestBody EmployerProfileRequest request) {
        log.info("Request to update the employer profile of user: {}", userId);
        EmployerProfileResponse response = employerService.updateProfile(userId, request);
        log.info("Response to the employer profile update for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Post a job. It is born a draft and reaches workers only once it has been approved")
    @PostMapping("jobs")
    public JobResponse createJob(@AuthenticationPrincipal String userId, @RequestBody JobRequest request) {
        log.info("Request to create a job for user: {}", userId);
        JobResponse response = employerService.createJob(userId, request);
        log.info("Response to the job creation for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "List the company's own jobs, newest first, optionally narrowed to one status")
    @GetMapping("jobs")
    public JobListResponse listJobs(@AuthenticationPrincipal String userId,
                                    @RequestParam(required = false) JobStatus status,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size) {
        log.info("Request to list the jobs of user: {}", userId);
        JobListResponse response = employerService.listJobs(userId, status, page, size);
        log.info("Response to the job listing for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read one of the company's own jobs")
    @GetMapping("jobs/{jobId}")
    public JobResponse getJob(@AuthenticationPrincipal String userId, @PathVariable String jobId) {
        log.info("Request to read job {} for user: {}", jobId, userId);
        JobResponse response = employerService.getJob(userId, jobId);
        log.info("Response to the job read for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Change a job. Only a draft or a job sent back for changes can be edited")
    @PutMapping("jobs/{jobId}")
    public JobResponse updateJob(@AuthenticationPrincipal String userId,
                                 @PathVariable String jobId,
                                 @RequestBody JobRequest request) {
        log.info("Request to update job {} for user: {}", jobId, userId);
        JobResponse response = employerService.updateJob(userId, jobId, request);
        log.info("Response to the job update for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Delete a job nobody has seen yet. Anything past a draft is closed rather than deleted")
    @DeleteMapping("jobs/{jobId}")
    public GeneralResponse deleteJob(@AuthenticationPrincipal String userId, @PathVariable String jobId) {
        log.info("Request to delete job {} for user: {}", jobId, userId);
        GeneralResponse response = employerService.deleteJob(userId, jobId);
        log.info("Response to the job deletion for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Send a job for approval. A company the platform has already trusted skips the queue")
    @PostMapping("jobs/{jobId}/submit")
    public JobResponse submitForApproval(@AuthenticationPrincipal String userId, @PathVariable String jobId) {
        log.info("Request to submit job {} for approval by user: {}", jobId, userId);
        JobResponse response = employerService.submitForApproval(userId, jobId);
        log.info("Response to the job submission for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Close a job so no further worker can apply for it")
    @PostMapping("jobs/{jobId}/close")
    public JobResponse closeJob(@AuthenticationPrincipal String userId, @PathVariable String jobId) {
        log.info("Request to close job {} for user: {}", jobId, userId);
        JobResponse response = employerService.closeJob(userId, jobId);
        log.info("Response to the job close for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Everything that has been said about a job in the approval queue, newest first")
    @GetMapping("jobs/{jobId}/approvals")
    public JobApprovalListResponse approvalHistory(@AuthenticationPrincipal String userId,
                                                   @PathVariable String jobId) {
        log.info("Request to read the approval history of job {} for user: {}", jobId, userId);
        JobApprovalListResponse response = employerService.approvalHistory(userId, jobId);
        log.info("Response to the approval history for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "The workers who applied for a job, best match first, optionally narrowed to one status")
    @GetMapping("jobs/{jobId}/applicants")
    public ApplicantListResponse listApplicants(@AuthenticationPrincipal String userId,
                                                @PathVariable String jobId,
                                                @RequestParam(required = false) ApplicationStatus status) {
        log.info("Request to list the applicants of job {} for user: {}", jobId, userId);
        ApplicantListResponse response = employerService.listApplicants(userId, jobId, status);
        log.info("Response to the applicant listing for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read one applicant. Their contact details are released only once they have been accepted")
    @GetMapping("jobs/{jobId}/applicants/{applicationId}")
    public ApplicantResponse getApplicant(@AuthenticationPrincipal String userId,
                                          @PathVariable String jobId,
                                          @PathVariable String applicationId) {
        log.info("Request to read applicant {} on job {} for user: {}", applicationId, jobId, userId);
        ApplicantResponse response = employerService.getApplicant(userId, jobId, applicationId);
        log.info("Response to the applicant read for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Shortlist an applicant without answering them either way yet")
    @PostMapping("jobs/{jobId}/applicants/{applicationId}/shortlist")
    public ApplicantResponse shortlistApplicant(@AuthenticationPrincipal String userId,
                                                @PathVariable String jobId,
                                                @PathVariable String applicationId) {
        log.info("Request to shortlist applicant {} on job {} for user: {}", applicationId, jobId, userId);
        ApplicantResponse response = employerService.shortlistApplicant(userId, jobId, applicationId);
        log.info("Response to the shortlisting for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Hire an applicant. This fills one of the job's vacancies and releases their contact details")
    @PostMapping("jobs/{jobId}/applicants/{applicationId}/accept")
    public ApplicantResponse acceptApplicant(@AuthenticationPrincipal String userId,
                                             @PathVariable String jobId,
                                             @PathVariable String applicationId) {
        log.info("Request to accept applicant {} on job {} for user: {}", applicationId, jobId, userId);
        ApplicantResponse response = employerService.acceptApplicant(userId, jobId, applicationId);
        log.info("Response to the acceptance for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Turn an applicant down, with a reason the worker is shown")
    @PostMapping("jobs/{jobId}/applicants/{applicationId}/reject")
    public ApplicantResponse rejectApplicant(@AuthenticationPrincipal String userId,
                                             @PathVariable String jobId,
                                             @PathVariable String applicationId,
                                             @RequestBody ApplicationDecisionRequest request) {
        log.info("Request to reject applicant {} on job {} for user: {}", applicationId, jobId, userId);
        ApplicantResponse response = employerService.rejectApplicant(userId, jobId, applicationId, request);
        log.info("Response to the rejection for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }
}
