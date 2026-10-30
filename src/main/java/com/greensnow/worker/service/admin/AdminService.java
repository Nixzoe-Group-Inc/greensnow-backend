package com.greensnow.worker.service.admin;

import com.greensnow.worker.model.constant.ResponseCode;
import com.greensnow.worker.model.constant.ResponseMessage;
import com.greensnow.worker.model.entity.EmployeeProfile;
import com.greensnow.worker.model.entity.EmployeeSkill;
import com.greensnow.worker.model.entity.EmployerProfile;
import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.entity.JobApproval;
import com.greensnow.worker.model.entity.JobRequirement;
import com.greensnow.worker.model.entity.Skill;
import com.greensnow.worker.model.entity.User;
import com.greensnow.worker.model.enums.JobStatus;
import com.greensnow.worker.model.enums.UserType;
import com.greensnow.worker.model.request.AccountStatusRequest;
import com.greensnow.worker.model.request.JobReviewRequest;
import com.greensnow.worker.model.request.SkillRequest;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.JobApprovalListResponse;
import com.greensnow.worker.model.response.JobDetail;
import com.greensnow.worker.model.response.JobListResponse;
import com.greensnow.worker.model.response.JobResponse;
import com.greensnow.worker.model.response.PlatformOverviewResponse;
import com.greensnow.worker.model.response.SkillListResponse;
import com.greensnow.worker.model.response.SkillResponse;
import com.greensnow.worker.model.response.UserDetail;
import com.greensnow.worker.model.response.UserListResponse;
import com.greensnow.worker.model.response.UserResponse;
import com.greensnow.worker.repository.EmployeeProfileRepository;
import com.greensnow.worker.repository.EmployeeSkillRepository;
import com.greensnow.worker.repository.EmployerProfileRepository;
import com.greensnow.worker.repository.JobApprovalRepository;
import com.greensnow.worker.repository.JobRepository;
import com.greensnow.worker.repository.JobRequirementRepository;
import com.greensnow.worker.repository.SkillRepository;
import com.greensnow.worker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final JobRequirementRepository jobRequirementRepository;
    private final JobApprovalRepository jobApprovalRepository;
    private final AdminRequestBuilder requestBuilder;
    private final AdminResponseProcessor responseProcessor;

    // ------------------------------------------------------------------------------------------
    // the approval queue
    // ------------------------------------------------------------------------------------------

    // oldest first, and deliberately not paged. the queue is the one place on this platform where
    // a company's work is sitting still and a worker's earning is waiting on someone's attention,
    // so the job that has waited longest is the one at the top and nothing is hidden behind a
    // second request
    public JobListResponse listApprovalQueue(String userId) {
        try {
            log.info("Admin {} is loading the approval queue", userId);
            List<Job> jobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatus.PENDING_APPROVAL);
            return responseProcessor.processJobListResponse(ResponseMessage.APPROVAL_QUEUE_RETRIEVED,
                    detailsFor(jobs));
        } catch (Exception exception) {
            log.error("Could not load the approval queue for admin {}", userId, exception);
            return responseProcessor.failedJobList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPROVAL_QUEUE_RETRIEVAL_FAILED);
        }
    }

    // the wider browse, newest first and paged, for everything that is not the queue. an approver
    // looking back at what was decided wants the most recent answer, not the oldest wait
    public JobListResponse listJobs(String userId, JobStatus status, Integer page, Integer size) {
        try {
            log.info("Admin {} is loading jobs with status: {}", userId, status);
            List<Job> jobs = Objects.nonNull(status)
                    ? jobRepository.findByStatusOrderByCreatedAtDesc(status)
                    : jobRepository.findAllByOrderByCreatedAtDesc();

            int resolvedPage = Objects.nonNull(page) && page > 0 ? page : 1;
            int resolvedSize = resolvedSize(size);
            int from = Math.min((resolvedPage - 1) * resolvedSize, jobs.size());
            int to = Math.min(from + resolvedSize, jobs.size());
            List<Job> pageOfJobs = jobs.subList(from, to);

            JobListResponse response = responseProcessor.processJobListResponse(ResponseMessage.JOBS_RETRIEVED,
                    detailsFor(pageOfJobs));
            response.setTotalJobs(jobs.size());
            response.setPage(resolvedPage);
            response.setSize(resolvedSize);
            response.setTotalPages((int) Math.ceil((double) jobs.size() / resolvedSize));
            return response;
        } catch (Exception exception) {
            log.error("Could not load jobs for admin {}", userId, exception);
            return responseProcessor.failedJobList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOBS_RETRIEVAL_FAILED);
        }
    }

    // an approver reads the whole job, requirements and all, because what is being decided is
    // whether a worker can safely be sent to it - and that is answered by the trades it asks for
    // as much as by the words in the description
    public JobResponse getJob(String userId, String jobId) {
        try {
            log.info("Admin {} is loading job: {}", userId, jobId);
            Optional<Job> job = jobRepository.findById(jobId);
            if (job.isEmpty()) {
                log.warn("Admin {} asked for a job that does not exist: {}", userId, jobId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_JOB_FOUND);
            }

            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(jobId);
            return responseProcessor.processJobResponse(ResponseMessage.JOB_RETRIEVED, job.get(), requirements,
                    cataloguedBy(requirements));
        } catch (Exception exception) {
            log.error("Could not load job {} for admin {}", jobId, userId, exception);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOB_RETRIEVAL_FAILED);
        }
    }

    public JobResponse approveJob(String userId, String jobId, JobReviewRequest request) {
        try {
            log.info("Admin {} is approving job: {}", userId, jobId);
            Optional<Job> existing = jobRepository.findById(jobId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to approve a job that does not exist: {}", userId, jobId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_JOB_FOUND);
            }

            Job job = existing.get();
            String unreviewable = rejectUnreviewable(job);
            if (StringUtils.hasText(unreviewable)) {
                log.warn("Admin {} tried to approve job {} which is {}", userId, jobId, job.getStatus());
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST, unreviewable);
            }

            JobApproval review = requestBuilder.buildApproval(job, userId, request);

            // the status is written first and the history row second, and the order matters
            // because there is no transaction holding the two together. a job that went open
            // with no history entry is a job workers can see and an approver can still explain,
            // where a history entry against a job still sitting in the queue would tell the
            // company it had been approved while no worker could find it
            job.setStatus(JobStatus.APPROVED);
            job.setApprovedAt(LocalDateTime.now());
            job.setPublishedAt(LocalDateTime.now());
            Job saved = jobRepository.save(job);
            jobApprovalRepository.save(review);

            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(jobId);
            return responseProcessor.processJobResponse(ResponseMessage.JOB_APPROVED, saved, requirements,
                    cataloguedBy(requirements));
        } catch (Exception exception) {
            log.error("Could not approve job {} for admin {}", jobId, userId, exception);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOB_APPROVAL_FAILED);
        }
    }

    public JobResponse rejectJob(String userId, String jobId, JobReviewRequest request) {
        try {
            log.info("Admin {} is turning down job: {}", userId, jobId);
            Optional<Job> existing = jobRepository.findById(jobId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to turn down a job that does not exist: {}", userId, jobId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_JOB_FOUND);
            }

            Job job = existing.get();
            String unanswerable = rejectUnanswerable(job, request);
            if (StringUtils.hasText(unanswerable)) {
                log.warn("Admin {} could not turn down job {}: {}", userId, jobId, unanswerable);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST, unanswerable);
            }

            JobApproval review = requestBuilder.buildRejection(job, userId, request);

            // the refusal is written to the job first for the same reason the approval is. a
            // refused job with no history is one the company can be told about and an approver
            // can still explain; a history row against a job still in the queue would have the
            // company answering a refusal that nobody had actually made
            job.setStatus(JobStatus.REJECTED);
            Job saved = jobRepository.save(job);
            jobApprovalRepository.save(review);

            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(jobId);
            return responseProcessor.processJobResponse(ResponseMessage.JOB_REJECTED, saved, requirements,
                    cataloguedBy(requirements));
        } catch (Exception exception) {
            log.error("Could not turn down job {} for admin {}", jobId, userId, exception);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOB_REJECTION_FAILED);
        }
    }

    // not a refusal. the job goes back to the company editable, with the reason attached, and the
    // company can send it round again - which is why the reason is required here too
    public JobResponse requestChanges(String userId, String jobId, JobReviewRequest request) {
        try {
            log.info("Admin {} is sending job {} back for changes", userId, jobId);
            Optional<Job> existing = jobRepository.findById(jobId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to send back a job that does not exist: {}", userId, jobId);
                return responseProcessor.failedJob(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_JOB_FOUND);
            }

            Job job = existing.get();
            String unanswerable = rejectUnanswerable(job, request);
            if (StringUtils.hasText(unanswerable)) {
                log.warn("Admin {} could not send job {} back: {}", userId, jobId, unanswerable);
                return responseProcessor.failedJob(ResponseCode.CODE_INVALID_REQUEST, unanswerable);
            }

            JobApproval review = requestBuilder.buildChangesRequested(job, userId, request);

            job.setStatus(JobStatus.CHANGES_REQUESTED);
            Job saved = jobRepository.save(job);
            jobApprovalRepository.save(review);

            List<JobRequirement> requirements = jobRequirementRepository.findByJobId(jobId);
            return responseProcessor.processJobResponse(ResponseMessage.JOB_CHANGES_REQUESTED, saved, requirements,
                    cataloguedBy(requirements));
        } catch (Exception exception) {
            log.error("Could not send job {} back for admin {}", jobId, userId, exception);
            return responseProcessor.failedJob(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.JOB_CHANGES_REQUEST_FAILED);
        }
    }

    // the same history the company reads. an approver and an employer arguing from two different
    // records is how a disagreement becomes unresolvable
    public JobApprovalListResponse listJobApprovals(String userId, String jobId) {
        try {
            log.info("Admin {} is loading the approval history for job: {}", userId, jobId);
            if (!jobRepository.existsById(jobId)) {
                log.warn("Admin {} asked for the history of a job that does not exist: {}", userId, jobId);
                return responseProcessor.failedApprovalList(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_JOB_FOUND);
            }

            List<JobApproval> approvals = jobApprovalRepository.findByJobIdOrderByCreatedAtDesc(jobId);
            return responseProcessor.processApprovalListResponse(ResponseMessage.APPROVALS_RETRIEVED, approvals);
        } catch (Exception exception) {
            log.error("Could not load the approval history for job {} and admin {}", jobId, userId, exception);
            return responseProcessor.failedApprovalList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.APPROVALS_RETRIEVAL_FAILED);
        }
    }

    // what this approver personally decided. the reviewer id is written on every row precisely so
    // that this reading is possible - a queue where nobody can be named is a queue nobody answers for
    public JobApprovalListResponse listMyReviews(String userId) {
        try {
            log.info("Admin {} is loading their own reviews", userId);
            List<JobApproval> approvals = jobApprovalRepository.findByReviewerIdOrderByCreatedAtDesc(userId);
            return responseProcessor.processApprovalListResponse(ResponseMessage.REVIEWS_RETRIEVED, approvals);
        } catch (Exception exception) {
            log.error("Could not load the reviews of admin {}", userId, exception);
            return responseProcessor.failedApprovalList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.REVIEWS_RETRIEVAL_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------------
    // the trade catalogue
    // ------------------------------------------------------------------------------------------

    public SkillResponse createSkill(String userId, SkillRequest request) {
        try {
            log.info("Admin {} is adding a skill to the catalogue: {}", userId, request.getName());
            String unusable = rejectUnusableSkill(request, null);
            if (StringUtils.hasText(unusable)) {
                log.warn("Admin {} could not add a skill: {}", userId, unusable);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST, unusable);
            }

            Skill saved = skillRepository.save(requestBuilder.buildSkill(request));
            return responseProcessor.processSkillResponse(ResponseMessage.SKILL_CREATED, saved);
        } catch (Exception exception) {
            log.error("Could not add a skill for admin {}", userId, exception);
            return responseProcessor.failedSkill(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.SKILL_CREATION_FAILED);
        }
    }

    public SkillResponse updateSkill(String userId, String skillId, SkillRequest request) {
        try {
            log.info("Admin {} is updating skill: {}", userId, skillId);
            Optional<Skill> existing = skillRepository.findById(skillId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to update a skill that does not exist: {}", userId, skillId);
                return responseProcessor.failedSkill(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_CATALOGUE_SKILL_FOUND);
            }

            Skill skill = existing.get();
            String unusable = rejectUnusableSkill(request, skill);
            if (StringUtils.hasText(unusable)) {
                log.warn("Admin {} could not update skill {}: {}", userId, skillId, unusable);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST, unusable);
            }

            Skill saved = skillRepository.save(requestBuilder.buildSkillUpdate(skill, request));
            return responseProcessor.processSkillResponse(ResponseMessage.CATALOGUE_SKILL_UPDATED, saved);
        } catch (Exception exception) {
            log.error("Could not update skill {} for admin {}", skillId, userId, exception);
            return responseProcessor.failedSkill(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.CATALOGUE_SKILL_UPDATE_FAILED);
        }
    }

    // the retired trades are listed here alongside the live ones, unlike every worker-facing
    // reading of the catalogue. the one person who can bring a trade back has to be able to see it
    public SkillListResponse listSkills(String userId, String category) {
        try {
            log.info("Admin {} is loading the catalogue for category: {}", userId, category);
            List<Skill> skills = StringUtils.hasText(category)
                    ? skillRepository.findByCategoryIgnoreCaseOrderByNameAsc(category.trim())
                    : skillRepository.findAllByOrderByNameAsc();
            return responseProcessor.processSkillListResponse(skills);
        } catch (Exception exception) {
            log.error("Could not load the catalogue for admin {}", userId, exception);
            return responseProcessor.failedSkillList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.CATALOGUE_SKILLS_RETRIEVAL_FAILED);
        }
    }

    public SkillResponse getSkill(String userId, String skillId) {
        try {
            log.info("Admin {} is loading skill: {}", userId, skillId);
            Optional<Skill> skill = skillRepository.findById(skillId);
            if (skill.isEmpty()) {
                log.warn("Admin {} asked for a skill that does not exist: {}", userId, skillId);
                return responseProcessor.failedSkill(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_CATALOGUE_SKILL_FOUND);
            }
            return responseProcessor.processSkillResponse(ResponseMessage.CATALOGUE_SKILL_RETRIEVED, skill.get());
        } catch (Exception exception) {
            log.error("Could not load skill {} for admin {}", skillId, userId, exception);
            return responseProcessor.failedSkill(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.CATALOGUE_SKILL_RETRIEVAL_FAILED);
        }
    }

    // retiring is not deleting, and it is important that it is not. a plumber who holds this trade
    // keeps holding it and a job that was built around it keeps asking for it - what stops is new
    // profiles and new jobs being able to choose it. deleting the row would break every record
    // that ever pointed at it
    public SkillResponse retireSkill(String userId, String skillId) {
        try {
            log.info("Admin {} is retiring skill: {}", userId, skillId);
            Optional<Skill> existing = skillRepository.findById(skillId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to retire a skill that does not exist: {}", userId, skillId);
                return responseProcessor.failedSkill(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_CATALOGUE_SKILL_FOUND);
            }

            Skill skill = existing.get();
            if (!skill.isActive()) {
                log.warn("Admin {} tried to retire skill {} which is already retired", userId, skillId);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.SKILL_ALREADY_RETIRED);
            }

            skill.setActive(false);
            return responseProcessor.processSkillResponse(ResponseMessage.SKILL_RETIRED,
                    skillRepository.save(skill));
        } catch (Exception exception) {
            log.error("Could not retire skill {} for admin {}", skillId, userId, exception);
            return responseProcessor.failedSkill(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.SKILL_RETIRE_FAILED);
        }
    }

    public SkillResponse restoreSkill(String userId, String skillId) {
        try {
            log.info("Admin {} is restoring skill: {}", userId, skillId);
            Optional<Skill> existing = skillRepository.findById(skillId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to restore a skill that does not exist: {}", userId, skillId);
                return responseProcessor.failedSkill(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_CATALOGUE_SKILL_FOUND);
            }

            Skill skill = existing.get();
            if (skill.isActive()) {
                log.warn("Admin {} tried to restore skill {} which is already active", userId, skillId);
                return responseProcessor.failedSkill(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.SKILL_ALREADY_ACTIVE);
            }

            skill.setActive(true);
            return responseProcessor.processSkillResponse(ResponseMessage.SKILL_RESTORED,
                    skillRepository.save(skill));
        } catch (Exception exception) {
            log.error("Could not restore skill {} for admin {}", skillId, userId, exception);
            return responseProcessor.failedSkill(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.SKILL_RESTORE_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------------
    // the accounts
    // ------------------------------------------------------------------------------------------

    // the listing deliberately does not look up a profile per account. a thousand rows would be a
    // thousand extra reads to answer a question nobody asked on a list screen, so hasProfile and
    // verified are left unanswered here and filled in when a single account is opened
    public UserListResponse listUsers(String userId, UserType userType, Integer page, Integer size) {
        try {
            log.info("Admin {} is loading accounts of type: {}", userId, userType);
            List<User> users = Objects.nonNull(userType)
                    ? userRepository.findByUserTypeOrderByCreatedAtDesc(userType)
                    : userRepository.findAllByOrderByCreatedAtDesc();

            int resolvedPage = Objects.nonNull(page) && page > 0 ? page : 1;
            int resolvedSize = resolvedSize(size);
            int from = Math.min((resolvedPage - 1) * resolvedSize, users.size());
            int to = Math.min(from + resolvedSize, users.size());

            List<UserDetail> entries = users.subList(from, to).stream()
                    .map(user -> responseProcessor.toUserDetail(user, null, null))
                    .toList();

            return responseProcessor.processUserListResponse(entries, users.size(), resolvedPage, resolvedSize);
        } catch (Exception exception) {
            log.error("Could not load accounts for admin {}", userId, exception);
            return responseProcessor.failedUserList(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.USERS_RETRIEVAL_FAILED);
        }
    }

    public UserResponse getUser(String userId, String accountId) {
        try {
            log.info("Admin {} is loading account: {}", userId, accountId);
            Optional<User> account = userRepository.findById(accountId);
            if (account.isEmpty()) {
                log.warn("Admin {} asked for an account that does not exist: {}", userId, accountId);
                return responseProcessor.failedUser(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_USER_ACCOUNT_FOUND);
            }

            User user = account.get();
            return responseProcessor.processUserResponse(ResponseMessage.USER_RETRIEVED, user, hasProfile(user),
                    verificationOf(user));
        } catch (Exception exception) {
            log.error("Could not load account {} for admin {}", accountId, userId, exception);
            return responseProcessor.failedUser(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.USER_RETRIEVAL_FAILED);
        }
    }

    // suspending is the heaviest thing on this side of the platform: the account cannot sign in
    // afterwards, so a worker stops earning and a company stops hiring the moment it is done. that
    // is why it costs a written reason, and why an approver cannot do it to themselves - locking
    // the last admin out of the platform is a mistake nobody can undo from inside it
    public GeneralResponse suspendUser(String userId, String accountId, AccountStatusRequest request) {
        try {
            log.info("Admin {} is suspending account: {}", userId, accountId);
            if (Objects.equals(userId, accountId)) {
                log.warn("Admin {} tried to suspend their own account", userId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.ADMIN_CANNOT_SUSPEND_SELF);
            }

            if (Objects.isNull(request) || !StringUtils.hasText(request.getReason())) {
                log.warn("Admin {} tried to suspend account {} with no reason", userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.NO_SUSPENSION_REASON_FOUND);
            }

            Optional<User> existing = userRepository.findById(accountId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to suspend an account that does not exist: {}", userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_USER_ACCOUNT_FOUND);
            }

            User user = existing.get();
            if (!user.isActive()) {
                log.warn("Admin {} tried to suspend account {} which is already suspended", userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.USER_ALREADY_SUSPENDED);
            }

            user.setActive(false);
            userRepository.save(user);
            log.info("Admin {} suspended account {} because: {}", userId, accountId, request.getReason().trim());
            return responseProcessor.processGeneralResponse(ResponseMessage.USER_SUSPENDED);
        } catch (Exception exception) {
            log.error("Could not suspend account {} for admin {}", accountId, userId, exception);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.USER_SUSPENSION_FAILED);
        }
    }

    public GeneralResponse restoreUser(String userId, String accountId) {
        try {
            log.info("Admin {} is restoring account: {}", userId, accountId);
            Optional<User> existing = userRepository.findById(accountId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to restore an account that does not exist: {}", userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_USER_ACCOUNT_FOUND);
            }

            User user = existing.get();
            if (user.isActive()) {
                log.warn("Admin {} tried to restore account {} which is already active", userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.USER_ALREADY_ACTIVE);
            }

            user.setActive(true);
            userRepository.save(user);
            return responseProcessor.processGeneralResponse(ResponseMessage.USER_RESTORED);
        } catch (Exception exception) {
            log.error("Could not restore account {} for admin {}", accountId, userId, exception);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.USER_RESTORE_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------------
    // verification
    // ------------------------------------------------------------------------------------------

    // withdrawing a company's verification takes its auto-approval with it, always. the two were
    // never independent: skipping the queue is something a verified company is trusted with, so a
    // company that is no longer verified publishing straight to workers is the exact hole this
    // platform's approval step exists to close
    public GeneralResponse setEmployerVerification(String userId, String accountId, boolean verified) {
        try {
            log.info("Admin {} is setting the verification of company {} to: {}", userId, accountId, verified);
            Optional<EmployerProfile> existing = employerProfileRepository.findByUserId(accountId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to verify a company that does not exist: {}", userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_COMPANY_FOUND);
            }

            EmployerProfile profile = existing.get();
            profile.setVerified(verified);
            if (!verified) {
                profile.setAutoApproveJobs(false);
            }
            employerProfileRepository.save(profile);

            return responseProcessor.processGeneralResponse(verified
                    ? ResponseMessage.EMPLOYER_VERIFIED
                    : ResponseMessage.EMPLOYER_UNVERIFIED);
        } catch (Exception exception) {
            log.error("Could not set the verification of company {} for admin {}", accountId, userId, exception);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EMPLOYER_VERIFICATION_FAILED);
        }
    }

    // letting a company past the queue means its jobs reach workers with nobody having read them,
    // so it is only ever granted to a company that has already been verified. withdrawing it is
    // allowed at any time, because stopping something is never the dangerous direction
    public GeneralResponse setEmployerAutoApproval(String userId, String accountId, boolean autoApprove) {
        try {
            log.info("Admin {} is setting auto approval for company {} to: {}", userId, accountId, autoApprove);
            Optional<EmployerProfile> existing = employerProfileRepository.findByUserId(accountId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to change auto approval for a company that does not exist: {}",
                        userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_COMPANY_FOUND);
            }

            EmployerProfile profile = existing.get();
            if (autoApprove && !profile.isVerified()) {
                log.warn("Admin {} tried to let unverified company {} skip the queue", userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_INVALID_REQUEST,
                        ResponseMessage.EMPLOYER_NOT_VERIFIED);
            }

            profile.setAutoApproveJobs(autoApprove);
            employerProfileRepository.save(profile);

            return responseProcessor.processGeneralResponse(autoApprove
                    ? ResponseMessage.EMPLOYER_AUTO_APPROVAL_GRANTED
                    : ResponseMessage.EMPLOYER_AUTO_APPROVAL_WITHDRAWN);
        } catch (Exception exception) {
            log.error("Could not change auto approval for company {} and admin {}", accountId, userId, exception);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.EMPLOYER_AUTO_APPROVAL_FAILED);
        }
    }

    // a worker's identity is what a company is relying on when it lets a stranger into a house, so
    // it is held apart from the background check: one says this is who they say they are, the
    // other says what is known about them. a platform that collapsed the two would be telling a
    // company it had checked something it had not
    public GeneralResponse setWorkerIdVerification(String userId, String accountId, boolean verified) {
        try {
            log.info("Admin {} is setting the identity verification of worker {} to: {}", userId, accountId, verified);
            Optional<EmployeeProfile> existing = employeeProfileRepository.findByUserId(accountId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to verify a worker that does not exist: {}", userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_WORKER_PROFILE_FOUND);
            }

            EmployeeProfile profile = existing.get();
            profile.setIdVerified(verified);
            employeeProfileRepository.save(profile);

            return responseProcessor.processGeneralResponse(verified
                    ? ResponseMessage.WORKER_ID_VERIFIED
                    : ResponseMessage.WORKER_ID_UNVERIFIED);
        } catch (Exception exception) {
            log.error("Could not set the identity verification of worker {} for admin {}",
                    accountId, userId, exception);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.WORKER_VERIFICATION_FAILED);
        }
    }

    public GeneralResponse setWorkerBackgroundCheck(String userId, String accountId, boolean checked) {
        try {
            log.info("Admin {} is setting the background check of worker {} to: {}", userId, accountId, checked);
            Optional<EmployeeProfile> existing = employeeProfileRepository.findByUserId(accountId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to record a background check for a worker that does not exist: {}",
                        userId, accountId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_WORKER_PROFILE_FOUND);
            }

            EmployeeProfile profile = existing.get();
            profile.setBackgroundChecked(checked);
            employeeProfileRepository.save(profile);

            return responseProcessor.processGeneralResponse(checked
                    ? ResponseMessage.WORKER_BACKGROUND_CHECKED
                    : ResponseMessage.WORKER_BACKGROUND_CHECK_WITHDRAWN);
        } catch (Exception exception) {
            log.error("Could not set the background check of worker {} for admin {}", accountId, userId, exception);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.WORKER_VERIFICATION_FAILED);
        }
    }

    // a certification is verified one trade at a time, never per worker. a man who really is a
    // certified gas fitter may have claimed an electrical ticket he does not hold, and a single
    // verified flag on the worker would have this platform vouching for both
    public GeneralResponse setWorkerSkillVerification(String userId, String employeeSkillId, boolean verified) {
        try {
            log.info("Admin {} is setting the verification of certification {} to: {}",
                    userId, employeeSkillId, verified);
            Optional<EmployeeSkill> existing = employeeSkillRepository.findById(employeeSkillId);
            if (existing.isEmpty()) {
                log.warn("Admin {} tried to verify a certification that does not exist: {}", userId, employeeSkillId);
                return responseProcessor.failedGeneral(ResponseCode.CODE_RECORD_NOT_FOUND,
                        ResponseMessage.NO_WORKER_SKILL_FOUND);
            }

            EmployeeSkill employeeSkill = existing.get();
            employeeSkill.setVerified(verified);
            employeeSkillRepository.save(employeeSkill);

            return responseProcessor.processGeneralResponse(verified
                    ? ResponseMessage.WORKER_SKILL_VERIFIED
                    : ResponseMessage.WORKER_SKILL_UNVERIFIED);
        } catch (Exception exception) {
            log.error("Could not set the verification of certification {} for admin {}",
                    employeeSkillId, userId, exception);
            return responseProcessor.failedGeneral(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.WORKER_SKILL_VERIFICATION_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------------
    // the overview
    // ------------------------------------------------------------------------------------------

    public PlatformOverviewResponse getOverview(String userId) {
        try {
            log.info("Admin {} is loading the platform overview", userId);
            return responseProcessor.processOverviewResponse(
                    userRepository.countByUserType(UserType.EMPLOYEE),
                    userRepository.countByUserType(UserType.EMPLOYER),
                    employerProfileRepository.countByVerifiedTrue(),
                    jobRepository.countByStatus(JobStatus.PENDING_APPROVAL),
                    jobRepository.countByStatus(JobStatus.APPROVED),
                    jobRepository.countByStatus(JobStatus.REJECTED),
                    jobRepository.countByStatus(JobStatus.CHANGES_REQUESTED));
        } catch (Exception exception) {
            log.error("Could not load the platform overview for admin {}", userId, exception);
            return responseProcessor.failedOverview(ResponseCode.CODE_GENERAL_FAILURE,
                    ResponseMessage.OVERVIEW_RETRIEVAL_FAILED);
        }
    }

    // ------------------------------------------------------------------------------------------
    // guards
    // ------------------------------------------------------------------------------------------

    // only a job that is actually waiting can be decided on. a second approval landing on a job
    // that is already out being worked would reopen it underneath the worker standing on the site
    private String rejectUnreviewable(Job job) {
        if (!JobStatus.PENDING_APPROVAL.equals(job.getStatus())) {
            return String.format(ResponseMessage.JOB_NOT_AWAITING_APPROVAL, job.getStatus());
        }
        return null;
    }

    // a refusal and a request for changes both leave the company with something to do, so both
    // cost a reason. "no" with nothing attached is a company guessing at what to change, and a
    // guessed correction comes straight back into the same queue
    private String rejectUnanswerable(Job job, JobReviewRequest request) {
        String unreviewable = rejectUnreviewable(job);
        if (StringUtils.hasText(unreviewable)) {
            return unreviewable;
        }
        if (Objects.isNull(request) || !StringUtils.hasText(request.getReason())) {
            return ResponseMessage.NO_REVIEW_REASON_FOUND;
        }
        return null;
    }

    // the catalogue is the vocabulary every profile and every job is written in, so a duplicate
    // name splits one trade into two and a worker searching for plumbers finds half of them. the
    // existing record is passed in on an edit so a trade is never reported as a duplicate of itself
    private String rejectUnusableSkill(SkillRequest request, Skill existing) {
        if (Objects.isNull(request) || !StringUtils.hasText(request.getName())) {
            return ResponseMessage.NO_SKILL_NAME_FOUND;
        }
        if (!StringUtils.hasText(request.getCategory())) {
            return ResponseMessage.NO_SKILL_CATEGORY_FOUND;
        }

        String name = request.getName().trim();
        boolean renamed = Objects.isNull(existing) || !name.equalsIgnoreCase(existing.getName());
        if (renamed && skillRepository.existsByNameIgnoreCase(name)) {
            return String.format(ResponseMessage.SKILL_NAME_ALREADY_IN_CATALOGUE, name);
        }

        if (StringUtils.hasText(request.getSlug())) {
            String slug = request.getSlug().trim();
            boolean reslugged = Objects.isNull(existing) || !slug.equalsIgnoreCase(existing.getSlug());
            if (reslugged && skillRepository.existsBySlugIgnoreCase(slug)) {
                return String.format(ResponseMessage.SKILL_SLUG_ALREADY_IN_CATALOGUE, slug);
            }
        }

        if (StringUtils.hasText(request.getParentSkillId())) {
            String parentSkillId = request.getParentSkillId().trim();
            if (Objects.nonNull(existing) && Objects.equals(parentSkillId, existing.getId())) {
                return ResponseMessage.SKILL_CANNOT_PARENT_ITSELF;
            }
            if (!skillRepository.existsById(parentSkillId)) {
                return String.format(ResponseMessage.PARENT_SKILL_NOT_RECOGNISED, parentSkillId);
            }
        }

        return null;
    }

    // ------------------------------------------------------------------------------------------
    // shared reads
    // ------------------------------------------------------------------------------------------

    // the requirements of a whole page of jobs are read in one go rather than one job at a time.
    // an approval queue that issued a query per row would slow down exactly as the queue got long,
    // which is the moment it most needs to be quick
    private List<JobDetail> detailsFor(List<Job> jobs) {
        if (jobs.isEmpty()) {
            return List.of();
        }

        List<String> jobIds = jobs.stream().map(Job::getId).toList();
        List<JobRequirement> requirements = jobRequirementRepository.findByJobIdIn(jobIds);
        Map<String, List<JobRequirement>> requirementsByJob = requirements.stream()
                .collect(Collectors.groupingBy(JobRequirement::getJobId));
        Map<String, Skill> skillsById = cataloguedBy(requirements);

        return jobs.stream()
                .map(job -> responseProcessor.toJobDetail(job,
                        requirementsByJob.getOrDefault(job.getId(), List.of()), skillsById))
                .toList();
    }

    // the trades named by a set of requirements, read whole so a job reads out in words rather
    // than in ids. a requirement pointing at a trade that is no longer in the catalogue is simply
    // absent from the map rather than a failure - the job still has to be readable
    private Map<String, Skill> cataloguedBy(List<JobRequirement> requirements) {
        if (requirements.isEmpty()) {
            return Map.of();
        }
        List<String> skillIds = requirements.stream()
                .map(JobRequirement::getSkillId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (skillIds.isEmpty()) {
            return Map.of();
        }
        return skillRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity(), (first, second) -> first));
    }

    // whether the account ever finished signing up. an admin account has no profile of either kind
    // and is reported as such rather than as missing something it was never asked for
    private Boolean hasProfile(User user) {
        return switch (user.getUserType()) {
            case EMPLOYER -> employerProfileRepository.existsByUserId(user.getId());
            case EMPLOYEE -> employeeProfileRepository.existsByUserId(user.getId());
            case ADMIN -> null;
        };
    }

    // verified means a different thing on each side - a company that has been checked out, or a
    // worker whose identity has been. both answer the one question an approver is asking, which is
    // whether this platform has already vouched for them
    private Boolean verificationOf(User user) {
        return switch (user.getUserType()) {
            case EMPLOYER -> employerProfileRepository.findByUserId(user.getId())
                    .map(EmployerProfile::isVerified)
                    .orElse(null);
            case EMPLOYEE -> employeeProfileRepository.findByUserId(user.getId())
                    .map(EmployeeProfile::isIdVerified)
                    .orElse(null);
            case ADMIN -> null;
        };
    }

    // a page size nobody asked for settles on the default, and one nobody could render settles on
    // the ceiling. a caller asking for a hundred thousand accounts in one response gets a hundred
    private int resolvedSize(Integer size) {
        if (Objects.isNull(size) || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
