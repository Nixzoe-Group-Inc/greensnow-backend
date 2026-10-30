package com.greensnow.worker.service.admin;

import com.greensnow.worker.model.constant.ResponseCode;
import com.greensnow.worker.model.constant.ResponseMessage;
import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.entity.JobApproval;
import com.greensnow.worker.model.entity.JobRequirement;
import com.greensnow.worker.model.entity.Skill;
import com.greensnow.worker.model.entity.User;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.JobApprovalDetail;
import com.greensnow.worker.model.response.JobApprovalListResponse;
import com.greensnow.worker.model.response.JobDetail;
import com.greensnow.worker.model.response.JobListResponse;
import com.greensnow.worker.model.response.JobResponse;
import com.greensnow.worker.model.response.PlatformOverviewResponse;
import com.greensnow.worker.model.response.SkillDetail;
import com.greensnow.worker.model.response.SkillListResponse;
import com.greensnow.worker.model.response.SkillResponse;
import com.greensnow.worker.model.response.UserDetail;
import com.greensnow.worker.model.response.UserListResponse;
import com.greensnow.worker.model.response.UserResponse;
import com.greensnow.worker.service.employer.EmployerResponseProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminResponseProcessor {

    // a job reads the same to an approver as it does to the company that posted it. that is the
    // point: an approver deciding on something the employer cannot see would be deciding on a
    // different job from the one that was submitted
    private final EmployerResponseProcessor employerResponseProcessor;

    public SkillResponse processSkillResponse(String responseMessage, Skill skill) {
        return SkillResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .skill(toSkillDetail(skill))
                .build();
    }

    public SkillResponse failedSkill(String responseCode, String responseMessage) {
        return SkillResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public SkillListResponse processSkillListResponse(List<Skill> skills) {
        List<SkillDetail> entries = skills.stream()
                .map(this::toSkillDetail)
                .toList();

        return SkillListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.CATALOGUE_SKILLS_RETRIEVED)
                .totalSkills(entries.size())
                .skills(entries)
                .build();
    }

    public SkillListResponse failedSkillList(String responseCode, String responseMessage) {
        return SkillListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public UserResponse processUserResponse(String responseMessage, User user, Boolean hasProfile, Boolean verified) {
        return UserResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .user(toUserDetail(user, hasProfile, verified))
                .build();
    }

    public UserResponse failedUser(String responseCode, String responseMessage) {
        return UserResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public UserListResponse processUserListResponse(List<UserDetail> users, int totalUsers, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalUsers / size) : 0;
        return UserListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.USERS_RETRIEVED)
                .totalUsers(totalUsers)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .users(users)
                .build();
    }

    public UserListResponse failedUserList(String responseCode, String responseMessage) {
        return UserListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public JobResponse processJobResponse(String responseMessage, Job job, List<JobRequirement> requirements,
                                          Map<String, Skill> skillsById) {
        return employerResponseProcessor.processJobResponse(responseMessage, job, requirements, skillsById);
    }

    public JobResponse failedJob(String responseCode, String responseMessage) {
        return employerResponseProcessor.failedJob(responseCode, responseMessage);
    }

    // the queue is not paged. an approver works it until it is empty, and a page boundary that
    // hid the oldest waiting job behind a second request is how a job gets forgotten
    public JobListResponse processJobListResponse(String responseMessage, List<JobDetail> jobs) {
        return JobListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .totalJobs(jobs.size())
                .page(0)
                .size(jobs.size())
                .totalPages(jobs.isEmpty() ? 0 : 1)
                .jobs(jobs)
                .build();
    }

    public JobListResponse failedJobList(String responseCode, String responseMessage) {
        return employerResponseProcessor.failedJobList(responseCode, responseMessage);
    }

    public JobApprovalListResponse processApprovalListResponse(String responseMessage, List<JobApproval> approvals) {
        List<JobApprovalDetail> entries = approvals.stream()
                .map(this::toApprovalDetail)
                .toList();

        return JobApprovalListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .totalEntries(entries.size())
                .approvals(entries)
                .build();
    }

    public JobApprovalListResponse failedApprovalList(String responseCode, String responseMessage) {
        return employerResponseProcessor.failedApprovalList(responseCode, responseMessage);
    }

    public PlatformOverviewResponse processOverviewResponse(long workers, long employers, long verifiedEmployers,
                                                            long awaitingApproval, long approved, long rejected,
                                                            long changesRequested) {
        return PlatformOverviewResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.OVERVIEW_RETRIEVED)
                .workers(workers)
                .employers(employers)
                .verifiedEmployers(verifiedEmployers)
                .jobsAwaitingApproval(awaitingApproval)
                .jobsApproved(approved)
                .jobsRejected(rejected)
                .jobsWithChangesRequested(changesRequested)
                .build();
    }

    public PlatformOverviewResponse failedOverview(String responseCode, String responseMessage) {
        return PlatformOverviewResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public GeneralResponse processGeneralResponse(String responseMessage) {
        return employerResponseProcessor.processGeneralResponse(responseMessage);
    }

    public GeneralResponse failedGeneral(String responseCode, String responseMessage) {
        return employerResponseProcessor.failedGeneral(responseCode, responseMessage);
    }

    public JobDetail toJobDetail(Job job, List<JobRequirement> requirements, Map<String, Skill> skillsById) {
        return employerResponseProcessor.toJobDetail(job, requirements, skillsById);
    }

    public SkillDetail toSkillDetail(Skill skill) {
        return SkillDetail.builder()
                .skillId(skill.getId())
                .name(skill.getName())
                .slug(skill.getSlug())
                .category(skill.getCategory())
                .description(skill.getDescription())
                .parentSkillId(skill.getParentSkillId())
                .requiresCertification(skill.isRequiresCertification())
                .active(skill.isActive())
                .createdAt(skill.getCreatedAt())
                .updatedAt(skill.getUpdatedAt())
                .build();
    }

    // hasProfile and verified are left null rather than false where the caller did not look
    // them up. a listing of a thousand accounts does not read a thousand profiles, and a false
    // there would read as "this company applied and was turned down" rather than "not asked"
    public UserDetail toUserDetail(User user, Boolean hasProfile, Boolean verified) {
        return UserDetail.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(composeFullName(user))
                .userType(user.getUserType())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .active(user.isActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .hasProfile(hasProfile)
                .verified(verified)
                .build();
    }

    // the reviewer's id is carried on the entity but not out of here. the history an approver
    // reads is the same history the employer reads, so that the two are never arguing about
    // different records - and who reviewed what is an internal matter answered off the table
    private JobApprovalDetail toApprovalDetail(JobApproval approval) {
        return JobApprovalDetail.builder()
                .approvalId(approval.getId())
                .jobId(approval.getJobId())
                .status(approval.getStatus())
                .action(approval.getAction())
                .comments(approval.getComments())
                .rejectionReason(approval.getRejectionReason())
                .reviewedAt(approval.getReviewedAt())
                .createdAt(approval.getCreatedAt())
                .build();
    }

    private String composeFullName(User user) {
        if (Objects.isNull(user)) {
            return null;
        }
        return Stream.of(user.getFirstName(), user.getLastName())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }
}
