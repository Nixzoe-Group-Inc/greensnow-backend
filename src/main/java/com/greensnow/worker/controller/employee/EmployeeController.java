package com.greensnow.worker.controller.employee;

import com.greensnow.worker.model.enums.ApplicationStatus;
import com.greensnow.worker.model.request.AvailabilityRequest;
import com.greensnow.worker.model.request.EmployeeProfileRequest;
import com.greensnow.worker.model.request.EmployeeSkillRequest;
import com.greensnow.worker.model.request.JobApplicationRequest;
import com.greensnow.worker.model.response.EmployeeProfileResponse;
import com.greensnow.worker.model.response.EmployeeSkillListResponse;
import com.greensnow.worker.model.response.EmployeeSkillResponse;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.JobApplicationListResponse;
import com.greensnow.worker.model.response.JobApplicationResponse;
import com.greensnow.worker.model.response.JobListResponse;
import com.greensnow.worker.model.response.JobResponse;
import com.greensnow.worker.service.employee.EmployeeService;
import com.greensnow.worker.util.JsonUtility;
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
@RequestMapping("employee")
@Tag(name = "Employee", description = "The worker's own profile, skills, job search and applications")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "Create the worker's profile")
    @PostMapping("profile")
    public EmployeeProfileResponse createProfile(@AuthenticationPrincipal String userId,
                                                 @RequestBody EmployeeProfileRequest request) {
        log.info("Request to create a profile for user: {}", userId);
        EmployeeProfileResponse response = employeeService.createProfile(userId, request);
        log.info("Response to the profile creation for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read the worker's profile with their skills")
    @GetMapping("profile")
    public EmployeeProfileResponse getProfile(@AuthenticationPrincipal String userId) {
        log.info("Request to read the profile of user: {}", userId);
        EmployeeProfileResponse response = employeeService.getProfile(userId);
        log.info("Response to the profile read for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Update the worker's profile. Every field is optional and an unsent field is left as it was")
    @PutMapping("profile")
    public EmployeeProfileResponse updateProfile(@AuthenticationPrincipal String userId,
                                                 @RequestBody EmployeeProfileRequest request) {
        log.info("Request to update the profile of user: {}", userId);
        EmployeeProfileResponse response = employeeService.updateProfile(userId, request);
        log.info("Response to the profile update for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Set whether the worker is open to work right now")
    @PutMapping("availability")
    public GeneralResponse updateAvailability(@AuthenticationPrincipal String userId,
                                              @RequestBody AvailabilityRequest request) {
        log.info("Request to update availability for user: {}", userId);
        GeneralResponse response = employeeService.updateAvailability(userId, request);
        log.info("Response to the availability update for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Put a trade on the worker's profile")
    @PostMapping("skills")
    public EmployeeSkillResponse addSkill(@AuthenticationPrincipal String userId,
                                          @RequestBody EmployeeSkillRequest request) {
        log.info("Request to add a skill for user: {}", userId);
        EmployeeSkillResponse response = employeeService.addSkill(userId, request);
        log.info("Response to the skill addition for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Change what the worker claims for a trade they already hold")
    @PutMapping("skills/{employeeSkillId}")
    public EmployeeSkillResponse updateSkill(@AuthenticationPrincipal String userId,
                                             @PathVariable String employeeSkillId,
                                             @RequestBody EmployeeSkillRequest request) {
        log.info("Request to update skill {} for user: {}", employeeSkillId, userId);
        EmployeeSkillResponse response = employeeService.updateSkill(userId, employeeSkillId, request);
        log.info("Response to the skill update for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Take a trade off the worker's profile")
    @DeleteMapping("skills/{employeeSkillId}")
    public GeneralResponse removeSkill(@AuthenticationPrincipal String userId,
                                       @PathVariable String employeeSkillId) {
        log.info("Request to remove skill {} for user: {}", employeeSkillId, userId);
        GeneralResponse response = employeeService.removeSkill(userId, employeeSkillId);
        log.info("Response to the skill removal for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "List every trade on the worker's profile")
    @GetMapping("skills")
    public EmployeeSkillListResponse listSkills(@AuthenticationPrincipal String userId) {
        log.info("Request to list the skills of user: {}", userId);
        EmployeeSkillListResponse response = employeeService.listSkills(userId);
        log.info("Response to the skill listing for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Browse every approved job with a vacancy, scored against this worker but not filtered")
    @GetMapping("jobs")
    public JobListResponse browseJobs(@AuthenticationPrincipal String userId,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        log.info("Request to browse jobs for user: {}", userId);
        JobListResponse response = employeeService.browseJobs(userId, search, page, size);
        log.info("Response to the job browse for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    // mapped before the {jobId} route so the literal wins rather than being read as a job id
    @Operation(summary = "The same list with everything the worker is not eligible for dropped")
    @GetMapping("jobs/matches")
    public JobListResponse matchingJobs(@AuthenticationPrincipal String userId,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        log.info("Request to list matching jobs for user: {}", userId);
        JobListResponse response = employeeService.matchingJobs(userId, page, size);
        log.info("Response to the matching jobs for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Read one approved job with this worker's score against it")
    @GetMapping("jobs/{jobId}")
    public JobResponse getJob(@AuthenticationPrincipal String userId, @PathVariable String jobId) {
        log.info("Request to read job {} for user: {}", jobId, userId);
        JobResponse response = employeeService.getJob(userId, jobId);
        log.info("Response to the job read for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Apply for a job. The worker's score on the day is copied onto the application")
    @PostMapping("jobs/{jobId}/apply")
    public JobApplicationResponse applyForJob(@AuthenticationPrincipal String userId,
                                              @PathVariable String jobId,
                                              @RequestBody JobApplicationRequest request) {
        log.info("Request to apply for job {} by user: {}", jobId, userId);
        JobApplicationResponse response = employeeService.applyForJob(userId, jobId, request);
        log.info("Response to the job application for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "List the worker's applications, newest first, optionally narrowed to one status")
    @GetMapping("applications")
    public JobApplicationListResponse listApplications(@AuthenticationPrincipal String userId,
                                                       @RequestParam(required = false) ApplicationStatus status) {
        log.info("Request to list applications for user: {}", userId);
        JobApplicationListResponse response = employeeService.listApplications(userId, status);
        log.info("Response to the application listing for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }

    @Operation(summary = "Take back an application nobody has answered yet")
    @PostMapping("applications/{applicationId}/withdraw")
    public JobApplicationResponse withdrawApplication(@AuthenticationPrincipal String userId,
                                                      @PathVariable String applicationId) {
        log.info("Request to withdraw application {} for user: {}", applicationId, userId);
        JobApplicationResponse response = employeeService.withdrawApplication(userId, applicationId);
        log.info("Response to the application withdrawal for user {}: {}", userId, JsonUtility.toJson(response));
        return response;
    }
}
