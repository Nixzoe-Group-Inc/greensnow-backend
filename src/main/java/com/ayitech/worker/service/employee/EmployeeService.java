package com.ayitech.worker.service.employee;

import com.ayitech.worker.model.constant.ResponseCode;
import com.ayitech.worker.model.constant.ResponseMessage;
import com.ayitech.worker.model.entity.EmployeeProfile;
import com.ayitech.worker.model.entity.EmployeeSkill;
import com.ayitech.worker.model.entity.Job;
import com.ayitech.worker.model.entity.JobApplication;
import com.ayitech.worker.model.entity.JobRequirement;
import com.ayitech.worker.model.entity.Skill;
import com.ayitech.worker.model.entity.User;
import com.ayitech.worker.model.enums.ApplicationStatus;
import com.ayitech.worker.model.enums.AvailabilityStatus;
import com.ayitech.worker.model.enums.JobStatus;
import com.ayitech.worker.model.request.AvailabilityRequest;
import com.ayitech.worker.model.request.EmployeeProfileRequest;
import com.ayitech.worker.model.request.EmployeeSkillRequest;
import com.ayitech.worker.model.request.JobApplicationRequest;
import com.ayitech.worker.model.response.EmployeeProfileResponse;
import com.ayitech.worker.model.response.EmployeeSkillListResponse;
import com.ayitech.worker.model.response.EmployeeSkillResponse;
import com.ayitech.worker.model.response.GeneralResponse;
import com.ayitech.worker.model.response.JobApplicationDetail;
import com.ayitech.worker.model.response.JobApplicationListResponse;
import com.ayitech.worker.model.response.JobApplicationResponse;
import com.ayitech.worker.model.response.JobDetail;
import com.ayitech.worker.model.response.JobListResponse;
import com.ayitech.worker.model.response.JobResponse;
import com.ayitech.worker.repository.EmployeeProfileRepository;
import com.ayitech.worker.repository.EmployeeSkillRepository;
import com.ayitech.worker.repository.JobApplicationRepository;
import com.ayitech.worker.repository.JobRepository;
import com.ayitech.worker.repository.JobRequirementRepository;
import com.ayitech.worker.repository.SkillRepository;
import com.ayitech.worker.repository.UserRepository;
import com.ayitech.worker.util.GeoUtility;
import com.ayitech.worker.util.JsonUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
public class EmployeeService {

    // nobody has worked seventy years at a trade. the ceiling is here to catch a typo that
    // would otherwise score the worker above every honest profile on the platform
    private static final int MAX_YEARS_OF_EXPERIENCE = 70;

    private static final int MIN_SERVICE_RADIUS_KM = 1;
    private static final int MAX_SERVICE_RADIUS_KM = 500;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final JobRequirementRepository jobRequirementRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final EmployeeRequestBuilder requestBuilder;
    private final EmployeeResponseProcessor responseProcessor;
    private final JobMatchingService jobMatchingService;

    public EmployeeProfileResponse createProfile(String userId, EmployeeProfileRequest request) {
        try {
            log.info("Going to process profile creation for user: {}", userId);

            if (employeeProfileRepository.existsByUserId(userId)) {
                log.info("A profile already exists for user: {}", userId);
                return responseProcessor.failedProfile(ResponseCode.CODE_DUPLICATE_RECORD,
                        ResponseMessage.PROFILE_ALREADY_EXISTS);
            }

            String rejection = rejectUnusableProfile(request);
            if (StringUtils.hasText(rejection)) {
                log.info("Profile creation refused for user: {}. Reason: {}", userId, rejection);
                return responseProcessor.failedProfile(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            EmployeeProfile profile = requestBuilder.buildProfile(userId, request);

            log.info("Going to persist the profile for user: {}", userId);
            EmployeeProfile saved = employeeProfileRepository.save(profile);

            log.info("Response after creating the profile for user {}: {}", userId, JsonUtility.toJson(saved.getId()));
            return responseProcessor.processProfileResponse(ResponseMessage.PROFILE_CREATED, saved, userOf(userId),
                    List.of(), Map.of());

        } catch (Exception e) {
            log.info("An exception occurred when attempting to create a profile for user: {}. Exception: ", userId, e);
            return responseProcessor.failedProfile(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.PROFILE_CREATION_FAILED);
        }
    }

    public EmployeeProfileResponse getProfile(String userId) {
        try {
            log.info("Going to process profile retrieval for user: {}", userId);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found for user: {}", userId);
                return responseProcessor.failedProfile(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            EmployeeProfile profile = found.get();
            List<EmployeeSkill> heldSkills = employeeSkillRepository.findByEmployeeProfileId(profile.getId());

            log.info("Response after retrieving the profile for user {}: {}", userId,
                    JsonUtility.toJson(profile.getId()));
            return responseProcessor.processProfileResponse(ResponseMessage.PROFILE_RETRIEVED, profile, userOf(userId),
                    heldSkills, cataloguedBy(heldSkills));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to retrieve the profile for user: {}. Exception: ",
                    userId, e);
            return responseProcessor.failedProfile(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.PROFILE_RETRIEVAL_FAILED);
        }
    }

    public EmployeeProfileResponse updateProfile(String userId, EmployeeProfileRequest request) {
        try {
            log.info("Going to process profile update for user: {}", userId);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to update for user: {}", userId);
                return responseProcessor.failedProfile(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            String rejection = rejectUnusableProfile(request);
            if (StringUtils.hasText(rejection)) {
                log.info("Profile update refused for user: {}. Reason: {}", userId, rejection);
                return responseProcessor.failedProfile(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            EmployeeProfile profile = found.get();

            // a worker under review cannot clear the review by posting their own status, and
            // nobody can put themselves under one either - suspension is the platform's to set
            if (AvailabilityStatus.SUSPENDED.equals(request.getAvailabilityStatus())
                    || (Objects.nonNull(request.getAvailabilityStatus())
                    && AvailabilityStatus.SUSPENDED.equals(profile.getAvailabilityStatus()))) {
                log.info("Availability is not self assignable for user: {}", userId);
                return responseProcessor.failedProfile(ResponseCode.CODE_UNAUTHORISED,
                        ResponseMessage.AVAILABILITY_NOT_SELF_ASSIGNABLE);
            }

            log.info("Going to persist the updated profile for user: {}", userId);
            EmployeeProfile saved = employeeProfileRepository.save(requestBuilder.buildProfileUpdate(profile, request));

            List<EmployeeSkill> heldSkills = employeeSkillRepository.findByEmployeeProfileId(saved.getId());

            log.info("Response after updating the profile for user {}: {}", userId, JsonUtility.toJson(saved.getId()));
            return responseProcessor.processProfileResponse(ResponseMessage.PROFILE_UPDATED, saved, userOf(userId),
                    heldSkills, cataloguedBy(heldSkills));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to update the profile for user: {}. Exception: ",
                    userId, e);
            return responseProcessor.failedProfile(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.PROFILE_UPDATE_FAILED);
        }
    }

    public GeneralResponse updateAvailability(String userId, AvailabilityRequest request) {
        try {
            log.info("Going to process availability update for user: {}", userId);

            if (Objects.isNull(request.getAvailabilityStatus())) {
                log.info("No availability status was sent by user: {}", userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.NO_AVAILABILITY_STATUS_FOUND);
            }

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to set availability on for user: {}", userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            EmployeeProfile profile = found.get();

            if (AvailabilityStatus.SUSPENDED.equals(request.getAvailabilityStatus())
                    || AvailabilityStatus.SUSPENDED.equals(profile.getAvailabilityStatus())) {
                log.info("Availability is not self assignable for user: {}", userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_UNAUTHORISED,
                        ResponseMessage.AVAILABILITY_NOT_SELF_ASSIGNABLE);
            }

            profile.setAvailabilityStatus(request.getAvailabilityStatus());
            profile.setLastActiveAt(LocalDateTime.now());

            log.info("Going to persist the availability for user: {}", userId);
            employeeProfileRepository.save(profile);

            log.info("Response after updating availability for user {}: {}", userId,
                    JsonUtility.toJson(request.getAvailabilityStatus()));
            return responseProcessor.processGeneralResponse(ResponseMessage.AVAILABILITY_UPDATED);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to update availability for user: {}. Exception: ",
                    userId, e);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.AVAILABILITY_UPDATE_FAILED);
        }
    }

    public EmployeeSkillResponse addSkill(String userId, EmployeeSkillRequest request) {
        try {
            log.info("Going to process skill addition for user: {}", userId);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to add a skill to for user: {}", userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            if (!StringUtils.hasText(request.getSkillId())) {
                log.info("No skill was sent by user: {}", userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.NO_SKILL_FOUND);
            }

            if (Objects.isNull(request.getProficiencyLevel())) {
                log.info("No proficiency level was sent by user: {}", userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.NO_PROFICIENCY_LEVEL_FOUND);
            }

            if (Objects.nonNull(request.getYearsOfExperience())
                    && (request.getYearsOfExperience() < 0 || request.getYearsOfExperience() > MAX_YEARS_OF_EXPERIENCE)) {
                log.info("An unusable years of experience was sent by user: {}", userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.INVALID_YEARS_OF_EXPERIENCE);
            }

            Optional<Skill> catalogued = skillRepository.findById(request.getSkillId());
            if (catalogued.isEmpty() || !catalogued.get().isActive()) {
                log.info("Skill {} is not one we recognise, asked for by user: {}", request.getSkillId(), userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.SKILL_NOT_RECOGNISED);
            }

            Skill skill = catalogued.get();
            EmployeeProfile profile = found.get();

            if (employeeSkillRepository.existsByEmployeeProfileIdAndSkillId(profile.getId(), skill.getId())) {
                log.info("Skill {} is already on the profile of user: {}", skill.getId(), userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_DUPLICATE_RECORD,
                        ResponseMessage.SKILL_ALREADY_ON_PROFILE);
            }

            // a trade the platform says needs papers cannot be claimed without them. this is the
            // gate that stops an uncertified electrician appearing beside a certified one
            if (skill.isRequiresCertification() && !hasCertification(request)) {
                log.info("Skill {} requires a certification the user {} did not send", skill.getId(), userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.CERTIFICATION_REQUIRED, skill.getName()));
            }

            EmployeeSkill employeeSkill = requestBuilder.buildEmployeeSkill(profile.getId(), request);

            log.info("Going to persist the skill {} for user: {}", skill.getId(), userId);
            EmployeeSkill saved = employeeSkillRepository.save(employeeSkill);

            log.info("Response after adding skill for user {}: {}", userId, JsonUtility.toJson(saved.getId()));
            return responseProcessor.processSkillResponse(ResponseMessage.SKILL_ADDED, saved, skill);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to add a skill for user: {}. Exception: ", userId, e);
            return responseProcessor.failedSkill(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.SKILL_ADD_FAILED);
        }
    }

    public EmployeeSkillResponse updateSkill(String userId, String employeeSkillId, EmployeeSkillRequest request) {
        try {
            log.info("Going to process skill update {} for user: {}", employeeSkillId, userId);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to update a skill on for user: {}", userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            Optional<EmployeeSkill> owned = ownedSkill(found.get().getId(), employeeSkillId);
            if (owned.isEmpty()) {
                log.info("Skill {} is not on the profile of user: {}", employeeSkillId, userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.SKILL_NOT_ON_PROFILE);
            }

            EmployeeSkill skill = owned.get();

            // the platform has already checked this one. letting the worker edit it would leave
            // a verified badge sitting over claims nobody looked at
            if (skill.isVerified()) {
                log.info("Skill {} is verified and not editable by user: {}", employeeSkillId, userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_UNAUTHORISED,
                        ResponseMessage.VERIFIED_SKILL_NOT_EDITABLE);
            }

            if (Objects.nonNull(request.getYearsOfExperience())
                    && (request.getYearsOfExperience() < 0 || request.getYearsOfExperience() > MAX_YEARS_OF_EXPERIENCE)) {
                log.info("An unusable years of experience was sent by user: {}", userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.INVALID_YEARS_OF_EXPERIENCE);
            }

            Skill catalogue = skillRepository.findById(skill.getSkillId()).orElse(null);

            // the certification is asked of the request alone, not of what the row already holds.
            // an update is the whole record, so a trade that needs a certificate and arrives
            // without one would otherwise pass this check and then clear the certificate it
            // passed on
            if (Objects.nonNull(catalogue) && catalogue.isRequiresCertification() && !hasCertification(request)) {
                log.info("Skill {} requires a certification the user {} did not send", employeeSkillId, userId);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.CERTIFICATION_REQUIRED, catalogue.getName()));
            }

            log.info("Going to persist the updated skill {} for user: {}", employeeSkillId, userId);
            EmployeeSkill saved = employeeSkillRepository.save(requestBuilder.buildSkillUpdate(skill, request));

            log.info("Response after updating skill for user {}: {}", userId, JsonUtility.toJson(saved.getId()));
            return responseProcessor.processSkillResponse(ResponseMessage.SKILL_UPDATED, saved, catalogue);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to update skill {} for user: {}. Exception: ",
                    employeeSkillId, userId, e);
            return responseProcessor.failedSkill(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.SKILL_UPDATE_FAILED);
        }
    }

    public GeneralResponse removeSkill(String userId, String employeeSkillId) {
        try {
            log.info("Going to process skill removal {} for user: {}", employeeSkillId, userId);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to remove a skill from for user: {}", userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            Optional<EmployeeSkill> owned = ownedSkill(found.get().getId(), employeeSkillId);
            if (owned.isEmpty()) {
                log.info("Skill {} is not on the profile of user: {}", employeeSkillId, userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.SKILL_NOT_ON_PROFILE);
            }

            if (owned.get().isVerified()) {
                log.info("Skill {} is verified and not removable by user: {}", employeeSkillId, userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_UNAUTHORISED,
                        ResponseMessage.VERIFIED_SKILL_NOT_EDITABLE);
            }

            log.info("Going to remove the skill {} for user: {}", employeeSkillId, userId);
            employeeSkillRepository.delete(owned.get());

            log.info("Response after removing skill for user {}: {}", userId, JsonUtility.toJson(employeeSkillId));
            return responseProcessor.processGeneralResponse(ResponseMessage.SKILL_REMOVED);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to remove skill {} for user: {}. Exception: ",
                    employeeSkillId, userId, e);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.SKILL_REMOVE_FAILED);
        }
    }

    public EmployeeSkillListResponse listSkills(String userId) {
        try {
            log.info("Going to process skill listing for user: {}", userId);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to list skills for user: {}", userId);
                return responseProcessor.failedSkillList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            List<EmployeeSkill> heldSkills = employeeSkillRepository.findByEmployeeProfileId(found.get().getId());

            log.info("Response after listing skills for user {}: {}", userId, JsonUtility.toJson(heldSkills.size()));
            return responseProcessor.processSkillListResponse(heldSkills, cataloguedBy(heldSkills));

        } catch (Exception e) {
            log.info("An exception occurred when attempting to list skills for user: {}. Exception: ", userId, e);
            return responseProcessor.failedSkillList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.SKILLS_RETRIEVAL_FAILED);
        }
    }

    // every approved job with a vacancy, scored against this worker but not filtered by the
    // score - a worker browsing is entitled to see work they are a poor match for and decide
    // for themselves
    public JobListResponse browseJobs(String userId, String search, Integer page, Integer size) {
        return listJobs(userId, search, page, size, false);
    }

    // the same list with everything the worker is not eligible for dropped, which is the screen
    // the platform pushes work from
    public JobListResponse matchingJobs(String userId, Integer page, Integer size) {
        return listJobs(userId, null, page, size, true);
    }

    public JobResponse getJob(String userId, String jobId) {
        try {
            log.info("Going to process job retrieval {} for user: {}", jobId, userId);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to read a job for user: {}", userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            // an unapproved job answers exactly as a job that does not exist, so browsing cannot
            // be used to read the drafts employers have not had approved yet
            Optional<Job> approved = jobRepository.findByIdAndStatus(jobId, JobStatus.APPROVED);
            if (approved.isEmpty()) {
                log.info("No approved job found for id: {} asked for by user: {}", jobId, userId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND, ResponseMessage.NO_JOB_FOUND);
            }

            Job job = approved.get();
            EmployeeProfile profile = found.get();

            List<EmployeeSkill> heldSkills = employeeSkillRepository.findByEmployeeProfileId(profile.getId());
            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(jobId);

            MatchResult match = jobMatchingService.score(job, profile, heldSkills, requirements,
                    catalogueFor(requirements, heldSkills));

            boolean alreadyApplied = jobApplicationRepository.existsByJobIdAndEmployeeProfileId(jobId, profile.getId());

            log.info("Response after retrieving job {} for user {}: {}", jobId, userId,
                    JsonUtility.toJson(match.getMatchScore()));
            return responseProcessor.processJobResponse(job, match, alreadyApplied);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to retrieve job {} for user: {}. Exception: ",
                    jobId, userId, e);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOB_RETRIEVAL_FAILED);
        }
    }

    public JobApplicationResponse applyForJob(String userId, String jobId, JobApplicationRequest request) {
        try {
            log.info("Going to process job application {} for user: {}", jobId, userId);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to apply with for user: {}", userId);
                return responseProcessor.failedApplication(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            EmployeeProfile profile = found.get();

            if (AvailabilityStatus.SUSPENDED.equals(profile.getAvailabilityStatus())) {
                log.info("A suspended profile cannot apply. User: {}", userId);
                return responseProcessor.failedApplication(ResponseCode.CODE_UNAUTHORISED,
                        ResponseMessage.ACCOUNT_DEACTIVATED);
            }

            Optional<Job> approved = jobRepository.findByIdAndStatus(jobId, JobStatus.APPROVED);
            if (approved.isEmpty()) {
                log.info("No approved job found for id: {} applied for by user: {}", jobId, userId);
                return responseProcessor.failedApplication(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_JOB_FOUND);
            }

            Job job = approved.get();

            if (fullyStaffed(job)) {
                log.info("Job {} is already fully staffed, applied for by user: {}", jobId, userId);
                return responseProcessor.failedApplication(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.JOB_FULLY_STAFFED);
            }

            if (jobApplicationRepository.existsByJobIdAndEmployeeProfileId(jobId, profile.getId())) {
                log.info("User {} has already applied for job: {}", userId, jobId);
                return responseProcessor.failedApplication(ResponseCode.CODE_DUPLICATE_RECORD,
                        ResponseMessage.ALREADY_APPLIED);
            }

            if (Objects.nonNull(request.getProposedRate())
                    && request.getProposedRate().compareTo(BigDecimal.ZERO) <= 0) {
                log.info("An unusable proposed rate was sent by user: {}", userId);
                return responseProcessor.failedApplication(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.INVALID_PROPOSED_RATE);
            }

            List<EmployeeSkill> heldSkills = employeeSkillRepository.findByEmployeeProfileId(profile.getId());
            if (heldSkills.isEmpty()) {
                log.info("User {} holds no skills and cannot be matched to job: {}", userId, jobId);
                return responseProcessor.failedApplication(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.NO_SKILLS_ON_PROFILE);
            }

            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(jobId);
            MatchResult match = jobMatchingService.score(job, profile, heldSkills, requirements,
                    catalogueFor(requirements, heldSkills));

            // the score is the gate on applying, not merely a number shown beside the job. an
            // employer opening their applicant list should not have to filter out people who
            // do not hold what the job asked for
            if (!match.isEligible()) {
                String reason = StringUtils.hasText(match.getIneligibilityReason())
                        ? match.getIneligibilityReason()
                        : ResponseMessage.MANDATORY_SKILL_MISSING;
                log.info("User {} is not eligible for job {}. Reason: {}", userId, jobId, reason);
                return responseProcessor.failedApplication(ResponseCode.CODE_INVALID_REQUEST, reason);
            }

            if (match.getMatchScore().compareTo(jobMatchingService.getMinimumMatchScore()) < 0) {
                log.info("User {} scores {} for job {} and the minimum is {}", userId, match.getMatchScore(), jobId,
                        jobMatchingService.getMinimumMatchScore());
                return responseProcessor.failedApplication(ResponseCode.CODE_INVALID_REQUEST,
                        String.format(ResponseMessage.MATCH_SCORE_TOO_LOW, match.getMatchScore(),
                                jobMatchingService.getMinimumMatchScore()));
            }

            JobApplication application = requestBuilder.buildApplication(job, profile, request, match);

            // the application is written FIRST and the job's running count second. there is no
            // transaction here, so one of the two can land without the other: an application the
            // count has not caught up with is a tally an employer can see is off, where a count
            // raised against an application that was never written is a job that looks busier
            // than it is and a worker who was never recorded as having asked
            log.info("Going to persist the application for job {} by user: {}", jobId, userId);
            JobApplication saved = jobApplicationRepository.save(application);

            job.setApplicationCount(Objects.nonNull(job.getApplicationCount()) ? job.getApplicationCount() + 1 : 1);
            jobRepository.save(job);

            profile.setLastActiveAt(LocalDateTime.now());
            employeeProfileRepository.save(profile);

            log.info("Response after applying for job {} by user {}: {}", jobId, userId,
                    JsonUtility.toJson(saved.getId()));
            return responseProcessor.processApplicationResponse(ResponseMessage.APPLICATION_SUBMITTED, saved, job);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to apply for job {} by user: {}. Exception: ",
                    jobId, userId, e);
            return responseProcessor.failedApplication(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPLICATION_FAILED);
        }
    }

    public JobApplicationListResponse listApplications(String userId, ApplicationStatus status) {
        try {
            log.info("Going to process application listing for user: {} with status: {}", userId, status);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to list applications for user: {}", userId);
                return responseProcessor.failedApplicationList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            String profileId = found.get().getId();
            List<JobApplication> applications = Objects.nonNull(status)
                    ? jobApplicationRepository.findByEmployeeProfileIdAndStatusOrderByAppliedAtDesc(profileId, status)
                    : jobApplicationRepository.findByEmployeeProfileIdOrderByAppliedAtDesc(profileId);

            Map<String, Job> jobsById = jobsFor(applications.stream()
                    .map(JobApplication::getJobId)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList());

            List<JobApplicationDetail> details = applications.stream()
                    .map(application -> responseProcessor.toApplicationDetail(application,
                            jobsById.get(application.getJobId())))
                    .toList();

            log.info("Response after listing applications for user {}: {}", userId,
                    JsonUtility.toJson(details.size()));
            return responseProcessor.processApplicationListResponse(details);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to list applications for user: {}. Exception: ", userId, e);
            return responseProcessor.failedApplicationList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPLICATIONS_RETRIEVAL_FAILED);
        }
    }

    public JobApplicationResponse withdrawApplication(String userId, String applicationId) {
        try {
            log.info("Going to process application withdrawal {} for user: {}", applicationId, userId);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to withdraw an application for user: {}", userId);
                return responseProcessor.failedApplication(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            // read by id AND owner, so another worker's application id answers exactly as an id
            // that does not exist - the response cannot be used to learn who applied for what
            Optional<JobApplication> owned = jobApplicationRepository.findByIdAndEmployeeProfileId(applicationId,
                    found.get().getId());
            if (owned.isEmpty()) {
                log.info("No application found for id: {} belonging to user: {}", applicationId, userId);
                return responseProcessor.failedApplication(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_APPLICATION_FOUND);
            }

            JobApplication application = owned.get();

            if (!withdrawable(application.getStatus())) {
                log.info("Application {} is {} and cannot be withdrawn by user: {}", applicationId,
                        application.getStatus(), userId);
                return responseProcessor.failedApplication(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.APPLICATION_NOT_WITHDRAWABLE);
            }

            application.setStatus(ApplicationStatus.WITHDRAWN);
            application.setRespondedAt(LocalDateTime.now());

            log.info("Going to persist the withdrawal of application: {} for user: {}", applicationId, userId);
            JobApplication saved = jobApplicationRepository.save(application);

            Job job = StringUtils.hasText(saved.getJobId())
                    ? jobRepository.findById(saved.getJobId()).orElse(null)
                    : null;

            log.info("Response after withdrawing application {} for user {}: {}", applicationId, userId,
                    JsonUtility.toJson(saved.getStatus()));
            return responseProcessor.processApplicationResponse(ResponseMessage.APPLICATION_WITHDRAWN, saved, job);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to withdraw application {} for user: {}. Exception: ",
                    applicationId, userId, e);
            return responseProcessor.failedApplication(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPLICATION_WITHDRAWAL_FAILED);
        }
    }

    // one body for browsing and for matches, because they are the same read with one filter
    // between them. the whole pickable list is scored and only then paged - a worker's best
    // match is not necessarily on the most recently published page, and paging before scoring
    // would hide it
    private JobListResponse listJobs(String userId, String search, Integer page, Integer size, boolean eligibleOnly) {
        try {
            log.info("Going to process job listing for user: {}. Eligible only: {}", userId, eligibleOnly);

            Optional<EmployeeProfile> found = employeeProfileRepository.findByUserId(userId);
            if (found.isEmpty()) {
                log.info("No profile found to list jobs for user: {}", userId);
                return responseProcessor.failedJobList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_PROFILE_FOUND);
            }

            EmployeeProfile profile = found.get();

            List<Job> pickable = StringUtils.hasText(search)
                    ? jobRepository.searchPickable(JobStatus.APPROVED, search.trim())
                    : jobRepository.findPickable(JobStatus.APPROVED);

            if (pickable.isEmpty()) {
                log.info("No pickable jobs found for user: {}", userId);
                return responseProcessor.processJobListResponse(List.of(), 0, 0, resolvedSize(size));
            }

            List<EmployeeSkill> heldSkills = employeeSkillRepository.findByEmployeeProfileId(profile.getId());

            // the whole page's requirements and the whole catalogue are read once each, so
            // scoring a list of jobs is two queries rather than two per job
            Map<String, List<JobRequirement>> requirementsByJob = requirementsFor(pickable);
            Map<String, Skill> skillsById = catalogueFor(requirementsByJob.values().stream()
                    .flatMap(List::stream)
                    .toList(), heldSkills);

            Set<String> appliedFor = jobApplicationRepository.findByEmployeeProfileId(profile.getId()).stream()
                    .map(JobApplication::getJobId)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());

            List<JobDetail> scored = new ArrayList<>();
            for (Job job : pickable) {
                MatchResult match = jobMatchingService.score(job, profile, heldSkills,
                        requirementsByJob.getOrDefault(job.getId(), List.of()), skillsById);

                if (eligibleOnly && !match.isEligible()) {
                    continue;
                }
                scored.add(responseProcessor.toJobDetail(job, match, appliedFor.contains(job.getId())));
            }

            // best match first, and an urgent job wins a tie - a worker who wants work today
            // should not have to scroll past three equal matches to find the one starting now
            Comparator<JobDetail> byScore = Comparator.comparing(JobDetail::getMatchScore,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            Comparator<JobDetail> byUrgency = Comparator.comparing(JobDetail::isUrgent, Comparator.reverseOrder());
            scored.sort(byScore.thenComparing(byUrgency));

            int resolvedSize = resolvedSize(size);
            int resolvedPage = Objects.nonNull(page) && page > 0 ? page : 0;
            int from = Math.min(resolvedPage * resolvedSize, scored.size());
            int to = Math.min(from + resolvedSize, scored.size());

            List<JobDetail> pageOfJobs = scored.subList(from, to);

            log.info("Response after listing jobs for user {}: {}", userId, JsonUtility.toJson(scored.size()));
            return responseProcessor.processJobListResponse(pageOfJobs, scored.size(), resolvedPage, resolvedSize);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to list jobs for user: {}. Exception: ", userId, e);
            return responseProcessor.failedJobList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOBS_RETRIEVAL_FAILED);
        }
    }

    // null is the answer when nothing is wrong, so a caller reads one string rather than a
    // chain of booleans. every field is optional, so each check only runs on what was sent
    private String rejectUnusableProfile(EmployeeProfileRequest request) {
        if (Objects.nonNull(request.getYearsOfExperience())
                && (request.getYearsOfExperience() < 0 || request.getYearsOfExperience() > MAX_YEARS_OF_EXPERIENCE)) {
            return ResponseMessage.INVALID_YEARS_OF_EXPERIENCE;
        }
        if (Objects.nonNull(request.getHourlyRate()) && request.getHourlyRate().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseMessage.INVALID_RATE;
        }
        if (Objects.nonNull(request.getDailyRate()) && request.getDailyRate().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseMessage.INVALID_RATE;
        }
        if (Objects.nonNull(request.getServiceRadiusKm())
                && (request.getServiceRadiusKm() < MIN_SERVICE_RADIUS_KM
                || request.getServiceRadiusKm() > MAX_SERVICE_RADIUS_KM)) {
            return ResponseMessage.INVALID_SERVICE_RADIUS;
        }
        // half a coordinate locates nothing. a profile carrying one of the two would be scored
        // as though it had no location at all, which is not what the worker asked for
        if (Objects.nonNull(request.getBaseLatitude()) != Objects.nonNull(request.getBaseLongitude())) {
            return ResponseMessage.INCOMPLETE_BASE_LOCATION;
        }
        if (Objects.nonNull(request.getBaseLatitude())
                && (!GeoUtility.isValidLatitude(request.getBaseLatitude())
                || !GeoUtility.isValidLongitude(request.getBaseLongitude()))) {
            return ResponseMessage.INVALID_COORDINATES;
        }
        return null;
    }

    private Optional<EmployeeSkill> ownedSkill(String employeeProfileId, String employeeSkillId) {
        if (!StringUtils.hasText(employeeSkillId)) {
            return Optional.empty();
        }
        return employeeSkillRepository.findById(employeeSkillId)
                .filter(skill -> employeeProfileId.equals(skill.getEmployeeProfileId()));
    }

    private boolean hasCertification(EmployeeSkillRequest request) {
        return Boolean.TRUE.equals(request.getCertified())
                && StringUtils.hasText(request.getCertificationName())
                && StringUtils.hasText(request.getCertificationUrl());
    }

    // only an application nobody has answered yet can be taken back. withdrawing one an
    // employer has already accepted would leave them a worker short with no word of it
    private boolean withdrawable(ApplicationStatus status) {
        return ApplicationStatus.APPLIED.equals(status) || ApplicationStatus.SHORTLISTED.equals(status);
    }

    private boolean fullyStaffed(Job job) {
        int needed = Objects.nonNull(job.getWorkersNeeded()) ? job.getWorkersNeeded() : 0;
        int assigned = Objects.nonNull(job.getWorkersAssigned()) ? job.getWorkersAssigned() : 0;
        return assigned >= needed;
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

    // the catalogue has to cover both sides of the comparison: what the job asked for and what
    // the worker holds. resolving only the requirements leaves a held skill with no name on it
    private Map<String, Skill> catalogueFor(List<JobRequirement> requirements, List<EmployeeSkill> heldSkills) {
        List<String> skillIds = new ArrayList<>();
        requirements.stream().map(JobRequirement::getSkillId).filter(StringUtils::hasText).forEach(skillIds::add);
        heldSkills.stream().map(EmployeeSkill::getSkillId).filter(StringUtils::hasText).forEach(skillIds::add);
        return skillsById(skillIds.stream().distinct().toList());
    }

    private Map<String, Skill> cataloguedBy(List<EmployeeSkill> heldSkills) {
        return skillsById(heldSkills.stream()
                .map(EmployeeSkill::getSkillId)
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

    private Map<String, Job> jobsFor(List<String> jobIds) {
        if (jobIds.isEmpty()) {
            return Map.of();
        }
        return jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity(), (first, second) -> first));
    }

    private User userOf(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    private int resolvedSize(Integer size) {
        if (Objects.isNull(size) || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
