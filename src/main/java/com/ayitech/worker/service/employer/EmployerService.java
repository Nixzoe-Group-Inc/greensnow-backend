package com.ayitech.worker.service.employer;

import com.ayitech.worker.model.constant.ResponseCode;
import com.ayitech.worker.model.constant.ResponseMessage;
import com.ayitech.worker.model.entity.EmployeeProfile;
import com.ayitech.worker.model.entity.EmployeeSkill;
import com.ayitech.worker.model.entity.EmployerProfile;
import com.ayitech.worker.model.entity.Job;
import com.ayitech.worker.model.entity.JobApplication;
import com.ayitech.worker.model.entity.JobApproval;
import com.ayitech.worker.model.entity.JobRequirement;
import com.ayitech.worker.model.entity.Skill;
import com.ayitech.worker.model.entity.User;
import com.ayitech.worker.model.enums.ApplicationStatus;
import com.ayitech.worker.model.enums.JobStatus;
import com.ayitech.worker.model.request.ApplicationDecisionRequest;
import com.ayitech.worker.model.request.EmployerProfileRequest;
import com.ayitech.worker.model.request.JobRequest;
import com.ayitech.worker.model.request.JobRequirementRequest;
import com.ayitech.worker.model.response.ApplicantDetail;
import com.ayitech.worker.model.response.ApplicantListResponse;
import com.ayitech.worker.model.response.ApplicantResponse;
import com.ayitech.worker.model.response.EmployerProfileResponse;
import com.ayitech.worker.model.response.GeneralResponse;
import com.ayitech.worker.model.response.JobApprovalListResponse;
import com.ayitech.worker.model.response.JobDetail;
import com.ayitech.worker.model.response.JobListResponse;
import com.ayitech.worker.model.response.JobResponse;
import com.ayitech.worker.repository.EmployeeProfileRepository;
import com.ayitech.worker.repository.EmployeeSkillRepository;
import com.ayitech.worker.repository.EmployerProfileRepository;
import com.ayitech.worker.repository.JobApplicationRepository;
import com.ayitech.worker.repository.JobApprovalRepository;
import com.ayitech.worker.repository.JobExecutionRepository;
import com.ayitech.worker.repository.JobRepository;
import com.ayitech.worker.repository.JobRequirementRepository;
import com.ayitech.worker.repository.SkillRepository;
import com.ayitech.worker.repository.UserRepository;
import com.ayitech.worker.service.tracking.TrackingRequestBuilder;
import com.ayitech.worker.util.GeoUtility;
import com.ayitech.worker.util.JsonUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployerService {

    // a single posting asking for more than fifty hands is a programme of work rather than a
    // job, and far more likely to be a typo. it would sit at the top of every worker's list
    private static final int MAX_WORKERS_NEEDED = 50;

    private static final int MAX_DAILY_HOURS = 24;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployerProfileRepository employerProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final JobRequirementRepository jobRequirementRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobApprovalRepository jobApprovalRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final UserRepository userRepository;
    private final EmployerRequestBuilder requestBuilder;
    private final EmployerResponseProcessor responseProcessor;
    private final TrackingRequestBuilder trackingRequestBuilder;

    public EmployerProfileResponse createProfile(String userId, EmployerProfileRequest request) {
        try {
            log.info("Going to process company profile creation for user: {}", userId);

            if (employerProfileRepository.existsByUserId(userId)) {
                log.info("A company profile already exists for user: {}", userId);
                return responseProcessor.failedProfile(ResponseCode.CODE_DUPLICATE_RECORD,
                        ResponseMessage.PROFILE_ALREADY_EXISTS);
            }

            String rejection = rejectUnusableProfile(request);
            if (StringUtils.hasText(rejection)) {
                log.info("Company profile creation refused for user: {}. Reason: {}", userId, rejection);
                return responseProcessor.failedProfile(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            EmployerProfile profile = requestBuilder.buildProfile(userId, request);

            log.info("Going to persist the company profile for user: {}", userId);
            EmployerProfile saved = employerProfileRepository.save(profile);

            log.info("Response after creating the company profile for user {}: {}", userId,
                    JsonUtility.toJson(saved.getId()));
            return responseProcessor.processProfileResponse(ResponseMessage.PROFILE_CREATED, saved, userOf(userId));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to create a company profile for user: {}. Exception: ",
                    userId, e);
            return responseProcessor.failedProfile(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.PROFILE_CREATION_FAILED);
        }
    }

    public EmployerProfileResponse getProfile(String userId) {
        try {
            log.info("Going to process company profile retrieval for user: {}", userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found for user: {}", userId);
                return responseProcessor.failedProfile(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            log.info("Response after retrieving the company profile for user {}: {}", userId,
                    JsonUtility.toJson(found.get().getId()));
            return responseProcessor.processProfileResponse(ResponseMessage.PROFILE_RETRIEVED, found.get(),
                    userOf(userId));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to retrieve the company profile for user: {}. Exception: ",
                    userId, e);
            return responseProcessor.failedProfile(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.PROFILE_RETRIEVAL_FAILED);
        }
    }

    public EmployerProfileResponse updateProfile(String userId, EmployerProfileRequest request) {
        try {
            log.info("Going to process company profile update for user: {}", userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to update for user: {}", userId);
                return responseProcessor.failedProfile(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            String rejection = rejectUnusableProfile(request);
            if (StringUtils.hasText(rejection)) {
                log.info("Company profile update refused for user: {}. Reason: {}", userId, rejection);
                return responseProcessor.failedProfile(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            EmployerProfile profile = found.get();

            log.info("Going to persist the updated company profile for user: {}", userId);
            EmployerProfile saved = employerProfileRepository.save(requestBuilder.buildProfileUpdate(profile, request));

            log.info("Response after updating the company profile for user {}: {}", userId,
                    JsonUtility.toJson(saved.getId()));
            return responseProcessor.processProfileResponse(ResponseMessage.PROFILE_UPDATED, saved, userOf(userId));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to update the company profile for user: {}. Exception: ",
                    userId, e);
            return responseProcessor.failedProfile(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.PROFILE_UPDATE_FAILED);
        }
    }

    public JobResponse createJob(String userId, JobRequest request) {
        try {
            log.info("Going to process job creation for user: {}", userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to post a job against for user: {}", userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            String rejection = rejectUnusableJob(request);
            if (StringUtils.hasText(rejection)) {
                log.info("Job creation refused for user: {}. Reason: {}", userId, rejection);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            List<JobRequirementRequest> wanted = requested(request);
            String requirementRejection = rejectUnusableRequirements(wanted);
            if (StringUtils.hasText(requirementRejection)) {
                log.info("Job creation refused for user: {}. Reason: {}", userId, requirementRejection);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST, requirementRejection);
            }

            EmployerProfile profile = found.get();
            Job job = requestBuilder.buildJob(profile.getId(), request);

            log.info("Going to persist the job for user: {}", userId);
            Job saved = jobRepository.save(job);

            // the requirements are written second and the posting tally third. there is no
            // transaction here, so a job that lands without its requirements is a draft the
            // employer can see is short and fix, where requirements written against a job that
            // was never saved are rows nothing can reach
            List<JobRequirement> requirements = wanted.isEmpty()
                    ? List.of()
                    : jobRequirementRepository.saveAll(requestBuilder.buildRequirements(saved.getId(), wanted));

            profile.setJobsPosted(Objects.nonNull(profile.getJobsPosted()) ? profile.getJobsPosted() + 1 : 1);
            employerProfileRepository.save(profile);

            log.info("Response after creating the job for user {}: {}", userId, JsonUtility.toJson(saved.getId()));
            return responseProcessor.processJobResponse(ResponseMessage.JOB_CREATED, saved, requirements,
                    cataloguedBy(requirements));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to create a job for user: {}. Exception: ", userId, e);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOB_CREATION_FAILED);
        }
    }

    public JobListResponse listJobs(String userId, JobStatus status, Integer page, Integer size) {
        try {
            log.info("Going to process the job listing for user: {}", userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to list jobs for user: {}", userId);
                return responseProcessor.failedJobList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            String employerProfileId = found.get().getId();
            List<Job> jobs = Objects.nonNull(status)
                    ? jobRepository.findByEmployerProfileIdAndStatusOrderByCreatedAtDesc(employerProfileId, status)
                    : jobRepository.findByEmployerProfileIdOrderByCreatedAtDesc(employerProfileId);

            int resolvedPage = Objects.nonNull(page) && page > 0 ? page : 1;
            int resolvedSize = resolvedSize(size);
            int from = Math.min((resolvedPage - 1) * resolvedSize, jobs.size());
            int to = Math.min(from + resolvedSize, jobs.size());
            List<Job> pageOfJobs = jobs.subList(from, to);

            // the whole page's requirements in one read rather than one read per job. a list of
            // twenty jobs would otherwise be twenty one trips to the database
            Map<String, List<JobRequirement>> requirementsByJob = requirementsFor(pageOfJobs);
            Map<String, Skill> skillsById = cataloguedBy(requirementsByJob.values().stream()
                    .flatMap(List::stream)
                    .toList());

            List<JobDetail> details = pageOfJobs.stream()
                    .map(job -> responseProcessor.toJobDetail(job,
                            requirementsByJob.getOrDefault(job.getId(), List.of()), skillsById))
                    .toList();

            log.info("Response after listing jobs for user {}: {}", userId, JsonUtility.toJson(details.size()));
            return responseProcessor.processJobListResponse(details, jobs.size(), resolvedPage, resolvedSize);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to list jobs for user: {}. Exception: ", userId, e);
            return responseProcessor.failedJobList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOBS_RETRIEVAL_FAILED);
        }
    }

    public JobResponse getJob(String userId, String jobId) {
        try {
            log.info("Going to process the read of job {} for user: {}", jobId, userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to read a job for user: {}", userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            Optional<Job> ownedJob = ownedJob(found.get().getId(), jobId);
            if (ownedJob.isEmpty()) {
                log.info("No job {} found for user: {}", jobId, userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND, ResponseMessage.NO_JOB_FOUND);
            }

            Job job = ownedJob.get();
            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(job.getId());

            log.info("Response after reading job {} for user {}: {}", jobId, userId, JsonUtility.toJson(job.getId()));
            return responseProcessor.processJobResponse(ResponseMessage.JOB_RETRIEVED, job, requirements,
                    cataloguedBy(requirements));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to read job {} for user: {}. Exception: ", jobId, userId, e);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.JOB_RETRIEVAL_FAILED);
        }
    }

    public JobResponse updateJob(String userId, String jobId, JobRequest request) {
        try {
            log.info("Going to process the update of job {} for user: {}", jobId, userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to update a job for user: {}", userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            Optional<Job> ownedJob = ownedJob(found.get().getId(), jobId);
            if (ownedJob.isEmpty()) {
                log.info("No job {} found to update for user: {}", jobId, userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND, ResponseMessage.NO_JOB_FOUND);
            }

            Job job = ownedJob.get();

            // once a job has been approved it is in front of workers, and some of them have
            // applied on the terms they read. rewriting the rate or the site under an
            // application is the one thing the approval step exists to stop
            if (!editable(job.getStatus())) {
                log.info("Job {} is {} and cannot be changed by user: {}", jobId, job.getStatus(), userId);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.JOB_NOT_EDITABLE, job.getStatus()));
            }

            String rejection = rejectUnusableJob(request);
            if (StringUtils.hasText(rejection)) {
                log.info("Job update refused for user: {}. Reason: {}", userId, rejection);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            List<JobRequirementRequest> wanted = requested(request);
            String requirementRejection = rejectUnusableRequirements(wanted);
            if (StringUtils.hasText(requirementRejection)) {
                log.info("Job update refused for user: {}. Reason: {}", userId, requirementRejection);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST, requirementRejection);
            }

            log.info("Going to persist the updated job {} for user: {}", jobId, userId);
            Job saved = jobRepository.save(requestBuilder.buildJobUpdate(job, request));

            // a requirement list that arrives replaces what the job carried. a per-requirement
            // patch would need ids the employer has no reason to hold, and leaving the old rows
            // beside the new ones would score the job against skills it no longer asks for
            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(saved.getId());
            if (!wanted.isEmpty()) {
                jobRequirementRepository.deleteAll(requirements);
                requirements = jobRequirementRepository.saveAll(requestBuilder.buildRequirements(saved.getId(), wanted));
            }

            log.info("Response after updating job {} for user {}: {}", jobId, userId, JsonUtility.toJson(saved.getId()));
            return responseProcessor.processJobResponse(ResponseMessage.JOB_UPDATED, saved, requirements,
                    cataloguedBy(requirements));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to update job {} for user: {}. Exception: ",
                    jobId, userId, e);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.JOB_UPDATE_FAILED);
        }
    }

    public GeneralResponse deleteJob(String userId, String jobId) {
        try {
            log.info("Going to process the deletion of job {} for user: {}", jobId, userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to delete a job for user: {}", userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            Optional<Job> ownedJob = ownedJob(found.get().getId(), jobId);
            if (ownedJob.isEmpty()) {
                log.info("No job {} found to delete for user: {}", jobId, userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND, ResponseMessage.NO_JOB_FOUND);
            }

            Job job = ownedJob.get();

            // only a draft truly goes. anything further along has been read by an approver or
            // applied for by a worker, and deleting it would take their record with it -
            // closing is what ends a job that has been seen
            if (!JobStatus.DRAFT.equals(job.getStatus())) {
                log.info("Job {} is {} and cannot be deleted by user: {}", jobId, job.getStatus(), userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.JOB_NOT_DELETABLE);
            }

            log.info("Going to delete job {} for user: {}", jobId, userId);
            jobRequirementRepository.deleteAll(jobRequirementRepository.findByJobId(job.getId()));
            jobRepository.delete(job);

            log.info("Response after deleting job {} for user {}: {}", jobId, userId, JsonUtility.toJson(jobId));
            return responseProcessor.processGeneralResponse(ResponseMessage.JOB_DELETED);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to delete job {} for user: {}. Exception: ",
                    jobId, userId, e);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.JOB_DELETE_FAILED);
        }
    }

    public JobResponse submitForApproval(String userId, String jobId) {
        try {
            log.info("Going to process the submission of job {} for user: {}", jobId, userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to submit a job for user: {}", userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            EmployerProfile profile = found.get();
            Optional<Job> ownedJob = ownedJob(profile.getId(), jobId);
            if (ownedJob.isEmpty()) {
                log.info("No job {} found to submit for user: {}", jobId, userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND, ResponseMessage.NO_JOB_FOUND);
            }

            Job job = ownedJob.get();
            if (!submittable(job.getStatus())) {
                log.info("Job {} is {} and cannot be submitted by user: {}", jobId, job.getStatus(), userId);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.JOB_NOT_SUBMITTABLE, job.getStatus()));
            }

            // a draft can be saved half finished, which is the point of a draft. the completeness
            // check belongs here instead, at the moment it goes in front of workers
            String rejection = rejectIncompleteJob(job);
            if (StringUtils.hasText(rejection)) {
                log.info("Submission of job {} refused for user: {}. Reason: {}", jobId, userId, rejection);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(job.getId());
            if (requirements.isEmpty()) {
                log.info("Job {} carries no requirements and cannot be submitted by user: {}", jobId, userId);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.NO_REQUIREMENTS_FOUND);
            }

            boolean resubmission = JobStatus.CHANGES_REQUESTED.equals(job.getStatus())
                    || JobStatus.REJECTED.equals(job.getStatus());
            // a company the platform has already trusted skips the queue. that switch is the
            // platform's to set, never the employer's, which is why it is read off the profile
            // and not off the request
            boolean autoApproved = profile.isAutoApproveJobs();

            job.setStatus(autoApproved ? JobStatus.APPROVED : JobStatus.PENDING_APPROVAL);
            job.setSubmittedForApprovalAt(LocalDateTime.now());
            if (autoApproved) {
                job.setApprovedAt(LocalDateTime.now());
                job.setPublishedAt(LocalDateTime.now());
            }

            log.info("Going to persist the submitted job {} for user: {}", jobId, userId);
            Job saved = jobRepository.save(job);

            // the status is written first and the history row second. a job that reached the
            // queue with no history entry is a job an approver can still answer, where a history
            // entry against a job still sitting in draft is a submission that never happened
            JobApproval approval = autoApproved
                    ? requestBuilder.buildAutoApproval(saved.getId())
                    : resubmission
                    ? requestBuilder.buildResubmission(saved.getId())
                    : requestBuilder.buildSubmission(saved.getId());
            jobApprovalRepository.save(approval);

            log.info("Response after submitting job {} for user {}: {}", jobId, userId, JsonUtility.toJson(saved.getId()));
            return responseProcessor.processJobResponse(
                    autoApproved ? ResponseMessage.JOB_AUTO_APPROVED : ResponseMessage.JOB_SUBMITTED,
                    saved, requirements, cataloguedBy(requirements));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to submit job {} for user: {}. Exception: ",
                    jobId, userId, e);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOB_SUBMISSION_FAILED);
        }
    }

    public JobResponse closeJob(String userId, String jobId) {
        try {
            log.info("Going to process the closing of job {} for user: {}", jobId, userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to close a job for user: {}", userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            Optional<Job> ownedJob = ownedJob(found.get().getId(), jobId);
            if (ownedJob.isEmpty()) {
                log.info("No job {} found to close for user: {}", jobId, userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND, ResponseMessage.NO_JOB_FOUND);
            }

            Job job = ownedJob.get();
            if (JobStatus.CLOSED.equals(job.getStatus()) || JobStatus.CANCELLED.equals(job.getStatus())) {
                log.info("Job {} is already {} for user: {}", jobId, job.getStatus(), userId);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.JOB_ALREADY_CLOSED);
            }

            job.setStatus(JobStatus.CLOSED);
            job.setClosedAt(LocalDateTime.now());

            log.info("Going to persist the closed job {} for user: {}", jobId, userId);
            Job saved = jobRepository.save(job);

            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(saved.getId());

            log.info("Response after closing job {} for user {}: {}", jobId, userId, JsonUtility.toJson(saved.getId()));
            return responseProcessor.processJobResponse(ResponseMessage.JOB_CLOSED, saved, requirements,
                    cataloguedBy(requirements));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to close job {} for user: {}. Exception: ",
                    jobId, userId, e);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.JOB_CLOSE_FAILED);
        }
    }

    public JobApprovalListResponse approvalHistory(String userId, String jobId) {
        try {
            log.info("Going to process the approval history of job {} for user: {}", jobId, userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to read an approval history for user: {}", userId);
                return responseProcessor.failedApprovalList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            Optional<Job> ownedJob = ownedJob(found.get().getId(), jobId);
            if (ownedJob.isEmpty()) {
                log.info("No job {} found to read an approval history for user: {}", jobId, userId);
                return responseProcessor.failedApprovalList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_JOB_FOUND);
            }

            List<JobApproval> approvals = jobApprovalRepository.findByJobIdOrderByCreatedAtDesc(jobId);

            log.info("Response after reading the approval history of job {} for user {}: {}", jobId, userId,
                    JsonUtility.toJson(approvals.size()));
            return responseProcessor.processApprovalListResponse(jobId, approvals);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to read the approval history of job {} for user: {}. "
                    + "Exception: ", jobId, userId, e);
            return responseProcessor.failedApprovalList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPROVALS_RETRIEVAL_FAILED);
        }
    }

    public ApplicantListResponse listApplicants(String userId, String jobId, ApplicationStatus status) {
        try {
            log.info("Going to process the applicant listing of job {} for user: {}", jobId, userId);

            Optional<EmployerProfile> found = employerProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No company profile found to list applicants for user: {}", userId);
                return responseProcessor.failedApplicantList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            Optional<Job> ownedJob = ownedJob(found.get().getId(), jobId);
            if (ownedJob.isEmpty()) {
                log.info("No job {} found to list applicants for user: {}", jobId, userId);
                return responseProcessor.failedApplicantList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_JOB_FOUND);
            }

            List<JobApplication> applications = Objects.nonNull(status)
                    ? jobApplicationRepository.findByJobIdAndStatusOrderByMatchScoreDesc(jobId, status)
                    : jobApplicationRepository.findByJobIdOrderByMatchScoreDesc(jobId);

            log.info("Response after listing the applicants of job {} for user {}: {}", jobId, userId,
                    JsonUtility.toJson(applications.size()));
            return responseProcessor.processApplicantListResponse(ownedJob.get(), toApplicantDetails(applications));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to list the applicants of job {} for user: {}. "
                    + "Exception: ", jobId, userId, e);
            return responseProcessor.failedApplicantList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPLICANTS_RETRIEVAL_FAILED);
        }
    }

    public ApplicantResponse getApplicant(String userId, String jobId, String applicationId) {
        try {
            log.info("Going to process the read of applicant {} on job {} for user: {}", applicationId, jobId, userId);

            Optional<JobApplication> found = ownedApplication(userId, jobId, applicationId);
            if (found.isEmpty()) {
                log.info("No applicant {} found on job {} for user: {}", applicationId, jobId, userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_APPLICATION_FOUND);
            }

            List<ApplicantDetail> details = toApplicantDetails(List.of(found.get()));

            log.info("Response after reading applicant {} on job {} for user {}: {}", applicationId, jobId, userId,
                    JsonUtility.toJson(applicationId));
            return responseProcessor.processApplicantResponse(ResponseMessage.APPLICANT_RETRIEVED,
                    details.isEmpty() ? null : details.getFirst());

        } catch (Exception e) {
            log.info("An exception occurred when attempting to read applicant {} on job {} for user: {}. Exception: ",
                    applicationId, jobId, userId, e);
            return responseProcessor.failedApplicant(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPLICANT_RETRIEVAL_FAILED);
        }
    }

    public ApplicantResponse shortlistApplicant(String userId, String jobId, String applicationId) {
        try {
            log.info("Going to process the shortlisting of applicant {} on job {} for user: {}",
                    applicationId, jobId, userId);

            Optional<JobApplication> found = ownedApplication(userId, jobId, applicationId);
            if (found.isEmpty()) {
                log.info("No applicant {} found to shortlist on job {} for user: {}", applicationId, jobId, userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_APPLICATION_FOUND);
            }

            JobApplication application = found.get();

            // shortlisting is a step forward from a fresh application and nothing else. a worker
            // already accepted, turned down or gone has been told where they stand, and moving
            // them now would change an outcome they have already been given
            if (!ApplicationStatus.APPLIED.equals(application.getStatus())) {
                log.info("Applicant {} is {} and cannot be shortlisted by user: {}", applicationId,
                        application.getStatus(), userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.APPLICANT_NOT_SHORTLISTABLE, application.getStatus()));
            }

            application.setStatus(ApplicationStatus.SHORTLISTED);

            log.info("Going to persist the shortlisted applicant {} on job {} for user: {}",
                    applicationId, jobId, userId);
            JobApplication saved = jobApplicationRepository.save(application);

            List<ApplicantDetail> details = toApplicantDetails(List.of(saved));

            log.info("Response after shortlisting applicant {} on job {} for user {}: {}", applicationId, jobId, userId,
                    JsonUtility.toJson(applicationId));
            return responseProcessor.processApplicantResponse(ResponseMessage.APPLICANT_SHORTLISTED,
                    details.isEmpty() ? null : details.getFirst());

        } catch (Exception e) {
            log.info("An exception occurred when attempting to shortlist applicant {} on job {} for user: {}. "
                    + "Exception: ", applicationId, jobId, userId, e);
            return responseProcessor.failedApplicant(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPLICANT_SHORTLIST_FAILED);
        }
    }

    public ApplicantResponse acceptApplicant(String userId, String jobId, String applicationId) {
        try {
            log.info("Going to process the acceptance of applicant {} on job {} for user: {}",
                    applicationId, jobId, userId);

            Optional<EmployerProfile> profile = employerProfileRepository.findByUserId(userId);
            if (profile.isEmpty()) {
                log.info("No company profile found to accept an applicant for user: {}", userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_EMPLOYER_PROFILE_FOUND);
            }

            Optional<Job> ownedJob = ownedJob(profile.get().getId(), jobId);
            if (ownedJob.isEmpty()) {
                log.info("No job {} found to accept an applicant on for user: {}", jobId, userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_JOB_FOUND);
            }

            Optional<JobApplication> found = jobApplicationRepository.findByIdAndJobId(applicationId, jobId);
            if (found.isEmpty()) {
                log.info("No applicant {} found to accept on job {} for user: {}", applicationId, jobId, userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_APPLICATION_FOUND);
            }

            Job job = ownedJob.get();
            JobApplication application = found.get();

            String rejection = rejectUnanswerableAcceptance(job, application);
            if (StringUtils.hasText(rejection)) {
                log.info("Acceptance of applicant {} on job {} refused for user: {}. Reason: {}",
                        applicationId, jobId, userId, rejection);
                return responseProcessor.failedApplicant(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            application.setStatus(ApplicationStatus.ACCEPTED);
            application.setRespondedAt(LocalDateTime.now());

            // the worker's answer is written first and the job's staffing second. there is no
            // transaction here, so a worker recorded as hired on a job whose count has not
            // caught up is a tally the employer can see is off, where a count raised against an
            // acceptance that was never written is a place held for nobody
            log.info("Going to persist the accepted applicant {} on job {} for user: {}", applicationId, jobId, userId);
            JobApplication saved = jobApplicationRepository.save(application);

            int assigned = (Objects.nonNull(job.getWorkersAssigned()) ? job.getWorkersAssigned() : 0) + 1;
            job.setWorkersAssigned(assigned);
            // a job carrying everybody it asked for comes off the worker side's list. it is the
            // staffing rather than the status that decides whether it is still offered, but
            // saying so on the status is what lets the employer read it at a glance
            if (assigned >= (Objects.nonNull(job.getWorkersNeeded()) ? job.getWorkersNeeded() : 0)) {
                job.setStatus(JobStatus.ASSIGNED);
            }
            jobRepository.save(job);

            openExecution(job, saved);

            List<ApplicantDetail> details = toApplicantDetails(List.of(saved));

            log.info("Response after accepting applicant {} on job {} for user {}: {}", applicationId, jobId, userId,
                    JsonUtility.toJson(applicationId));
            return responseProcessor.processApplicantResponse(ResponseMessage.APPLICANT_ACCEPTED,
                    details.isEmpty() ? null : details.getFirst());

        } catch (Exception e) {
            log.info("An exception occurred when attempting to accept applicant {} on job {} for user: {}. "
                    + "Exception: ", applicationId, jobId, userId, e);
            return responseProcessor.failedApplicant(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPLICANT_ACCEPT_FAILED);
        }
    }

    public ApplicantResponse rejectApplicant(String userId, String jobId, String applicationId,
                                             ApplicationDecisionRequest request) {
        try {
            log.info("Going to process the refusal of applicant {} on job {} for user: {}",
                    applicationId, jobId, userId);

            // a worker who travelled nowhere still arranged their week around this. a refusal
            // with no reason tells them nothing they can act on next time
            if (Objects.isNull(request) || !StringUtils.hasText(request.getReason())) {
                log.info("No reason was sent to refuse applicant {} on job {} by user: {}",
                        applicationId, jobId, userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.NO_REJECTION_REASON_FOUND);
            }

            Optional<JobApplication> found = ownedApplication(userId, jobId, applicationId);
            if (found.isEmpty()) {
                log.info("No applicant {} found to refuse on job {} for user: {}", applicationId, jobId, userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_APPLICATION_FOUND);
            }

            JobApplication application = found.get();

            if (ApplicationStatus.WITHDRAWN.equals(application.getStatus())) {
                log.info("Applicant {} on job {} has withdrawn and cannot be refused by user: {}",
                        applicationId, jobId, userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.APPLICATION_WAS_WITHDRAWN);
            }
            if (answered(application.getStatus())) {
                log.info("Applicant {} on job {} is already {} for user: {}", applicationId, jobId,
                        application.getStatus(), userId);
                return responseProcessor.failedApplicant(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.APPLICATION_ALREADY_ANSWERED);
            }

            application.setStatus(ApplicationStatus.REJECTED);
            application.setRespondedAt(LocalDateTime.now());
            application.setRejectionReason(request.getReason().trim());

            log.info("Going to persist the refused applicant {} on job {} for user: {}", applicationId, jobId, userId);
            JobApplication saved = jobApplicationRepository.save(application);

            List<ApplicantDetail> details = toApplicantDetails(List.of(saved));

            log.info("Response after refusing applicant {} on job {} for user {}: {}", applicationId, jobId, userId,
                    JsonUtility.toJson(applicationId));
            return responseProcessor.processApplicantResponse(ResponseMessage.APPLICANT_REJECTED,
                    details.isEmpty() ? null : details.getFirst());

        } catch (Exception e) {
            log.info("An exception occurred when attempting to refuse applicant {} on job {} for user: {}. "
                    + "Exception: ", applicationId, jobId, userId, e);
            return responseProcessor.failedApplicant(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPLICANT_REJECT_FAILED);
        }
    }

    // null is the answer when nothing is wrong, so a caller reads one string rather than a
    // chain of booleans. an update is the whole record, so the same checks run on both paths -
    // a company name is asked for on an edit too, because an edit without one would clear the
    // name every job on the platform is posted under
    private String rejectUnusableProfile(EmployerProfileRequest request) {
        if (!StringUtils.hasText(request.getCompanyName())) {
            return ResponseMessage.NO_COMPANY_NAME_FOUND;
        }
        // half a coordinate locates nothing. a company carrying one of the two would be read as
        // having no address at all, which is not what the employer asked for
        if (Objects.nonNull(request.getLatitude()) != Objects.nonNull(request.getLongitude())) {
            return ResponseMessage.INCOMPLETE_COMPANY_LOCATION;
        }
        if (Objects.nonNull(request.getLatitude())
                && (!GeoUtility.isValidLatitude(request.getLatitude())
                || !GeoUtility.isValidLongitude(request.getLongitude()))) {
            return ResponseMessage.INVALID_COORDINATES;
        }
        return null;
    }

    // the few fields a job cannot be read without are asked for on an edit as well as on a
    // create, because an edit is the whole record and one that left the title out would clear
    // the title the job was created with. everything else is checked only if it was sent, so a
    // draft can still be saved half written and finished later
    private String rejectUnusableJob(JobRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            return ResponseMessage.NO_JOB_TITLE_FOUND;
        }
        if (!StringUtils.hasText(request.getDescription())) {
            return ResponseMessage.NO_JOB_DESCRIPTION_FOUND;
        }
        if (Objects.isNull(request.getJobType())) {
            return ResponseMessage.NO_JOB_TYPE_FOUND;
        }
        if (Objects.nonNull(request.getHourlyRate()) && request.getHourlyRate().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseMessage.INVALID_JOB_RATE;
        }
        if (Objects.nonNull(request.getDailyRate()) && request.getDailyRate().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseMessage.INVALID_JOB_RATE;
        }
        if (Objects.nonNull(request.getTotalBudget()) && request.getTotalBudget().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseMessage.INVALID_JOB_RATE;
        }
        if (Objects.nonNull(request.getWorkersNeeded())
                && (request.getWorkersNeeded() < 1 || request.getWorkersNeeded() > MAX_WORKERS_NEEDED)) {
            return String.format(ResponseMessage.INVALID_WORKERS_NEEDED, MAX_WORKERS_NEEDED);
        }
        if (Objects.nonNull(request.getEstimatedDurationHours()) && request.getEstimatedDurationHours() <= 0) {
            return ResponseMessage.INVALID_DURATION;
        }
        if (Objects.nonNull(request.getMaxDailyHours())
                && (request.getMaxDailyHours() < 1 || request.getMaxDailyHours() > MAX_DAILY_HOURS)) {
            return ResponseMessage.INVALID_DAILY_HOURS;
        }
        if (Objects.nonNull(request.getExtensionsAllowed()) && request.getExtensionsAllowed() < 0) {
            return ResponseMessage.INVALID_EXTENSIONS_ALLOWED;
        }
        if (Objects.nonNull(request.getScheduledStartDate()) && Objects.nonNull(request.getScheduledEndDate())
                && request.getScheduledEndDate().isBefore(request.getScheduledStartDate())) {
            return ResponseMessage.INVALID_SCHEDULE;
        }
        if (Objects.nonNull(request.getSiteLatitude()) != Objects.nonNull(request.getSiteLongitude())) {
            return ResponseMessage.INCOMPLETE_SITE_LOCATION;
        }
        if (Objects.nonNull(request.getSiteLatitude())
                && (!GeoUtility.isValidLatitude(request.getSiteLatitude())
                || !GeoUtility.isValidLongitude(request.getSiteLongitude()))) {
            return ResponseMessage.INVALID_SITE_COORDINATES;
        }
        return null;
    }

    // what a worker has to be able to read before they will consider travelling to a site:
    // where it is, what it involves and what it pays
    private String rejectIncompleteJob(Job job) {
        if (!StringUtils.hasText(job.getTitle())) {
            return ResponseMessage.NO_JOB_TITLE_FOUND;
        }
        if (!StringUtils.hasText(job.getDescription())) {
            return ResponseMessage.NO_JOB_DESCRIPTION_FOUND;
        }
        if (Objects.isNull(job.getJobType())) {
            return ResponseMessage.NO_JOB_TYPE_FOUND;
        }
        if (!StringUtils.hasText(job.getSiteAddress())) {
            return ResponseMessage.NO_SITE_ADDRESS_FOUND;
        }
        if (!positive(job.getHourlyRate()) && !positive(job.getDailyRate()) && !positive(job.getTotalBudget())) {
            return ResponseMessage.NO_JOB_RATE_FOUND;
        }
        return null;
    }

    private String rejectUnusableRequirements(List<JobRequirementRequest> requirements) {
        if (requirements.isEmpty()) {
            return null;
        }

        Set<String> seen = new HashSet<>();
        for (JobRequirementRequest requirement : requirements) {
            if (!StringUtils.hasText(requirement.getSkillId())) {
                return ResponseMessage.NO_SKILL_FOUND;
            }
            // the same trade listed twice is weighted twice, which quietly shifts the score
            // away from everything else the job asked for
            if (!seen.add(requirement.getSkillId())) {
                return String.format(ResponseMessage.DUPLICATE_REQUIREMENT, requirement.getSkillId());
            }
            if (Objects.isNull(requirement.getMinimumProficiency())) {
                return ResponseMessage.NO_PROFICIENCY_LEVEL_FOUND;
            }
        }

        // the whole list checked against the catalogue in one read rather than one read each
        Set<String> known = skillRepository.findAllById(seen).stream()
                .map(Skill::getId)
                .collect(Collectors.toSet());
        for (String skillId : seen) {
            if (!known.contains(skillId)) {
                return String.format(ResponseMessage.REQUIREMENT_SKILL_NOT_RECOGNISED, skillId);
            }
        }
        return null;
    }

    // a job already carrying everybody it asked for cannot take another, and one that has not
    // been approved has not been in front of workers at all
    private String rejectUnanswerableAcceptance(Job job, JobApplication application) {
        if (ApplicationStatus.WITHDRAWN.equals(application.getStatus())) {
            return ResponseMessage.APPLICATION_WAS_WITHDRAWN;
        }
        if (answered(application.getStatus())) {
            return ResponseMessage.APPLICATION_ALREADY_ANSWERED;
        }
        if (!hiring(job.getStatus())) {
            return String.format(ResponseMessage.JOB_NOT_HIRING, job.getStatus());
        }
        int needed = Objects.nonNull(job.getWorkersNeeded()) ? job.getWorkersNeeded() : 0;
        int assigned = Objects.nonNull(job.getWorkersAssigned()) ? job.getWorkersAssigned() : 0;
        if (assigned >= needed) {
            return ResponseMessage.JOB_FULLY_STAFFED;
        }
        return null;
    }

    // the tracked job is born here, the moment somebody is hired, rather than when they set off.
    // the row has to exist before a phone can hang a single location off it, and the gap between
    // being accepted and starting is exactly the window the worker needs to see the job on their
    // handset at all. it is written last and in its own try/catch on purpose: the acceptance is
    // the thing the employer asked for and it is already saved, so a tracking row that failed to
    // open must not read back to them as a worker who was never hired - that would have them
    // accept the same person twice
    private void openExecution(Job job, JobApplication application) {
        try {
            if (jobExecutionRepository.existsByJobIdAndEmployeeProfileId(job.getId(),
                    application.getEmployeeProfileId())) {
                return;
            }
            jobExecutionRepository.save(trackingRequestBuilder.buildExecution(job, application));
        } catch (Exception e) {
            log.error("Failed to open the tracked job for applicant {} on job {}. Exception: ",
                    application.getId(), job.getId(), e);
        }
    }

    // the whole applicant list's profiles, users and skills in three reads rather than three
    // per applicant. a job with thirty applicants would otherwise be ninety trips
    private List<ApplicantDetail> toApplicantDetails(List<JobApplication> applications) {
        if (applications.isEmpty()) {
            return List.of();
        }

        List<String> profileIds = applications.stream()
                .map(JobApplication::getEmployeeProfileId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, EmployeeProfile> profilesById = employeeProfileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(EmployeeProfile::getId, Function.identity(), (first, second) -> first));

        Map<String, User> usersById = usersFor(profilesById.values().stream()
                .map(EmployeeProfile::getUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList());

        Map<String, List<EmployeeSkill>> skillsByProfile = new java.util.HashMap<>();
        List<EmployeeSkill> allHeld = new ArrayList<>();
        for (String profileId : profileIds) {
            List<EmployeeSkill> held = employeeSkillRepository.findByEmployeeProfileId(profileId);
            skillsByProfile.put(profileId, held);
            allHeld.addAll(held);
        }
        Map<String, Skill> skillsById = skillsById(allHeld.stream()
                .map(EmployeeSkill::getSkillId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList());

        return applications.stream()
                .map(application -> {
                    EmployeeProfile profile = profilesById.get(application.getEmployeeProfileId());
                    User user = Objects.nonNull(profile) ? usersById.get(profile.getUserId()) : null;
                    List<EmployeeSkill> held = skillsByProfile
                            .getOrDefault(application.getEmployeeProfileId(), List.of());
                    return responseProcessor.toApplicantDetail(application, profile, user, held, skillsById);
                })
                .toList();
    }

    private Optional<Job> ownedJob(String employerProfileId, String jobId) {
        if (!StringUtils.hasText(jobId)) {
            return Optional.empty();
        }
        return jobRepository.findByIdAndEmployerProfileId(jobId, employerProfileId);
    }

    // an application is reachable only through a job the signed in employer owns, so another
    // company's application id answers exactly as one that was never issued does
    private Optional<JobApplication> ownedApplication(String userId, String jobId, String applicationId) {
        if (!StringUtils.hasText(jobId) || !StringUtils.hasText(applicationId)) {
            return Optional.empty();
        }
        return employerProfileRepository.findByUserId(userId)
                .flatMap(profile -> ownedJob(profile.getId(), jobId))
                .flatMap(job -> jobApplicationRepository.findByIdAndJobId(applicationId, job.getId()));
    }

    private List<JobRequirementRequest> requested(JobRequest request) {
        return Objects.nonNull(request.getRequirements()) ? request.getRequirements() : List.of();
    }

    private Map<String, List<JobRequirement>> requirementsFor(List<Job> jobs) {
        List<String> jobIds = jobs.stream().map(Job::getId).filter(StringUtils::hasText).toList();
        if (jobIds.isEmpty()) {
            return Map.of();
        }
        return jobRequirementRepository.findByJobIdIn(jobIds).stream()
                .filter(requirement -> StringUtils.hasText(requirement.getJobId()))
                .collect(Collectors.groupingBy(JobRequirement::getJobId));
    }

    private Map<String, Skill> cataloguedBy(List<JobRequirement> requirements) {
        return skillsById(requirements.stream()
                .map(JobRequirement::getSkillId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList());
    }

    private Map<String, Skill> skillsById(List<String> skillIds) {
        if (skillIds.isEmpty()) {
            return Map.of();
        }
        return skillRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity(), (first, second) -> first));
    }

    private Map<String, User> usersFor(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (first, second) -> first));
    }

    private User userOf(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    // a job is only the employer's to rewrite before anybody has seen it. CHANGES_REQUESTED is
    // in here because an approver asking for changes is asking the employer to make them
    private boolean editable(JobStatus status) {
        return JobStatus.DRAFT.equals(status) || JobStatus.CHANGES_REQUESTED.equals(status);
    }

    // REJECTED is submittable too: a refusal that could not be answered would make the employer
    // post the whole job again and lose its history
    private boolean submittable(JobStatus status) {
        return JobStatus.DRAFT.equals(status)
                || JobStatus.CHANGES_REQUESTED.equals(status)
                || JobStatus.REJECTED.equals(status);
    }

    // a job takes workers while it is open and while it is still filling places. IN_PROGRESS is
    // in here on purpose - work that has started can still be short a pair of hands
    private boolean hiring(JobStatus status) {
        return JobStatus.APPROVED.equals(status)
                || JobStatus.ASSIGNED.equals(status)
                || JobStatus.IN_PROGRESS.equals(status);
    }

    private boolean answered(ApplicationStatus status) {
        return ApplicationStatus.ACCEPTED.equals(status) || ApplicationStatus.REJECTED.equals(status);
    }

    private boolean positive(BigDecimal value) {
        return Objects.nonNull(value) && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private int resolvedSize(Integer size) {
        if (Objects.isNull(size) || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
