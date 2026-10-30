package com.greensnow.worker.service.employer;

import com.greensnow.worker.model.constant.ResponseCode;
import com.greensnow.worker.model.constant.ResponseMessage;
import com.greensnow.worker.model.entity.EmployeeProfile;
import com.greensnow.worker.model.entity.EmployeeSkill;
import com.greensnow.worker.model.entity.EmployerProfile;
import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.entity.JobApplication;
import com.greensnow.worker.model.entity.JobApproval;
import com.greensnow.worker.model.entity.JobRequirement;
import com.greensnow.worker.model.entity.Skill;
import com.greensnow.worker.model.entity.User;
import com.greensnow.worker.model.enums.ApplicationStatus;
import com.greensnow.worker.model.response.ApplicantDetail;
import com.greensnow.worker.model.response.ApplicantListResponse;
import com.greensnow.worker.model.response.ApplicantResponse;
import com.greensnow.worker.model.response.EmployeeSkillDetail;
import com.greensnow.worker.model.response.EmployerProfileResponse;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.JobApprovalDetail;
import com.greensnow.worker.model.response.JobApprovalListResponse;
import com.greensnow.worker.model.response.JobDetail;
import com.greensnow.worker.model.response.JobListResponse;
import com.greensnow.worker.model.response.JobRequirementDetail;
import com.greensnow.worker.model.response.JobResponse;
import com.greensnow.worker.service.employee.EmployeeResponseProcessor;
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
public class EmployerResponseProcessor {

    // the eight things a worker looks at before they will travel to a site for somebody they
    // have never worked for. counted on read rather than stored, so it cannot go stale
    private static final int COMPLETION_FIELDS = 8;

    private final EmployeeResponseProcessor employeeResponseProcessor;

    public EmployerProfileResponse processProfileResponse(String responseMessage, EmployerProfile profile, User user) {
        return EmployerProfileResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .contactName(composeFullName(user))
                .email(Objects.nonNull(user) ? user.getEmail() : null)
                .phoneNumber(Objects.nonNull(user) ? user.getPhoneNumber() : null)
                .companyName(profile.getCompanyName())
                .registrationNumber(profile.getRegistrationNumber())
                .companySize(profile.getCompanySize())
                .industry(profile.getIndustry())
                .website(profile.getWebsite())
                .description(profile.getDescription())
                .address(profile.getAddress())
                .city(profile.getCity())
                .country(profile.getCountry())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .verified(profile.isVerified())
                .autoApproveJobs(profile.isAutoApproveJobs())
                .averageRating(profile.getAverageRating())
                .totalRatings(profile.getTotalRatings())
                .jobsPosted(profile.getJobsPosted())
                .jobsCompleted(profile.getJobsCompleted())
                .profileCompletionPercentage(completionPercentage(profile))
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    public EmployerProfileResponse failedProfile(String responseCode, String responseMessage) {
        return EmployerProfileResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public JobResponse processJobResponse(String responseMessage, Job job, List<JobRequirement> requirements,
                                          Map<String, Skill> skillsById) {
        return JobResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .job(toJobDetail(job, requirements, skillsById))
                .build();
    }

    public JobResponse failedJob(String responseCode, String responseMessage) {
        return JobResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public JobListResponse processJobListResponse(List<JobDetail> jobs, int totalJobs, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalJobs / size) : 0;
        return JobListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.JOBS_RETRIEVED)
                .totalJobs(totalJobs)
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .jobs(jobs)
                .build();
    }

    public JobListResponse failedJobList(String responseCode, String responseMessage) {
        return JobListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public ApplicantListResponse processApplicantListResponse(Job job, List<ApplicantDetail> applicants) {
        return ApplicantListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.APPLICANTS_RETRIEVED)
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .workersNeeded(job.getWorkersNeeded())
                .workersAssigned(job.getWorkersAssigned())
                .vacancies(vacancies(job))
                .totalApplicants(applicants.size())
                .applicants(applicants)
                .build();
    }

    public ApplicantListResponse failedApplicantList(String responseCode, String responseMessage) {
        return ApplicantListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public ApplicantResponse processApplicantResponse(String responseMessage, ApplicantDetail applicant) {
        return ApplicantResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .applicant(applicant)
                .build();
    }

    public ApplicantResponse failedApplicant(String responseCode, String responseMessage) {
        return ApplicantResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public JobApprovalListResponse processApprovalListResponse(String jobId, List<JobApproval> approvals) {
        List<JobApprovalDetail> entries = approvals.stream()
                .map(this::toApprovalDetail)
                .toList();

        return JobApprovalListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.APPROVALS_RETRIEVED)
                .jobId(jobId)
                .totalEntries(entries.size())
                .approvals(entries)
                .build();
    }

    public JobApprovalListResponse failedApprovalList(String responseCode, String responseMessage) {
        return JobApprovalListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public GeneralResponse processGeneralResponse(String responseMessage) {
        return GeneralResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .build();
    }

    public GeneralResponse failedGeneral(String responseCode, String responseMessage) {
        return GeneralResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    // the same JobDetail the worker side returns, with every per-worker field left null. a score
    // is how one worker sits against this job and there is no worker here - filling it with a
    // zero would read as a job that matches nobody
    public JobDetail toJobDetail(Job job, List<JobRequirement> requirements, Map<String, Skill> skillsById) {
        return JobDetail.builder()
                .jobId(job.getId())
                .employerProfileId(job.getEmployerProfileId())
                .title(job.getTitle())
                .description(job.getDescription())
                .jobType(job.getJobType())
                .experienceLevel(job.getExperienceLevel())
                .status(job.getStatus())
                .siteAddress(job.getSiteAddress())
                .siteLatitude(job.getSiteLatitude())
                .siteLongitude(job.getSiteLongitude())
                .hourlyRate(job.getHourlyRate())
                .dailyRate(job.getDailyRate())
                .totalBudget(job.getTotalBudget())
                .currency(job.getCurrency())
                .scheduledStartDate(job.getScheduledStartDate())
                .scheduledEndDate(job.getScheduledEndDate())
                .estimatedDurationHours(job.getEstimatedDurationHours())
                .maxDailyHours(job.getMaxDailyHours())
                .extensionsAllowed(job.getExtensionsAllowed())
                .workersNeeded(job.getWorkersNeeded())
                .workersAssigned(job.getWorkersAssigned())
                .vacancies(vacancies(job))
                .toolsProvided(job.isToolsProvided())
                .transportProvided(job.isTransportProvided())
                .urgent(job.isUrgent())
                .publishedAt(job.getPublishedAt())
                .applicationCount(job.getApplicationCount())
                .requirements(toRequirementDetails(requirements, skillsById))
                .build();
    }

    // the worker's contact details are released only once they have been accepted. a shortlist
    // is not a hire, and an employer who could read a phone number off every applicant would
    // have no reason to accept anybody through the platform at all
    public ApplicantDetail toApplicantDetail(JobApplication application, EmployeeProfile profile, User user,
                                             List<EmployeeSkill> heldSkills, Map<String, Skill> skillsById) {
        boolean accepted = ApplicationStatus.ACCEPTED.equals(application.getStatus());

        List<EmployeeSkillDetail> skills = heldSkills.stream()
                .map(held -> employeeResponseProcessor.toSkillDetail(held, skillsById.get(held.getSkillId())))
                .toList();

        return ApplicantDetail.builder()
                .applicationId(application.getId())
                .jobId(application.getJobId())
                .status(application.getStatus())
                .employeeProfileId(application.getEmployeeProfileId())
                .workerName(composeFullName(user))
                .headline(Objects.nonNull(profile) ? profile.getHeadline() : null)
                .bio(Objects.nonNull(profile) ? profile.getBio() : null)
                .yearsOfExperience(Objects.nonNull(profile) ? profile.getYearsOfExperience() : null)
                .educationLevel(Objects.nonNull(profile) ? profile.getEducationLevel() : null)
                .availabilityStatus(Objects.nonNull(profile) ? profile.getAvailabilityStatus() : null)
                .email(accepted && Objects.nonNull(user) ? user.getEmail() : null)
                .phoneNumber(accepted && Objects.nonNull(user) ? user.getPhoneNumber() : null)
                .hasOwnTools(Objects.nonNull(profile) && profile.isHasOwnTools())
                .hasTransport(Objects.nonNull(profile) && profile.isHasTransport())
                .idVerified(Objects.nonNull(profile) && profile.isIdVerified())
                .backgroundChecked(Objects.nonNull(profile) && profile.isBackgroundChecked())
                .averageRating(Objects.nonNull(profile) ? profile.getAverageRating() : null)
                .totalRatings(Objects.nonNull(profile) ? profile.getTotalRatings() : null)
                .jobsCompleted(Objects.nonNull(profile) ? profile.getJobsCompleted() : null)
                .completionRate(Objects.nonNull(profile) ? profile.getCompletionRate() : null)
                .matchScore(application.getMatchScore())
                .skillScore(application.getSkillScore())
                .experienceScore(application.getExperienceScore())
                .locationScore(application.getLocationScore())
                .educationScore(application.getEducationScore())
                .distanceKm(application.getDistanceKm())
                .coverNote(application.getCoverNote())
                .proposedRate(application.getProposedRate())
                .appliedAt(application.getAppliedAt())
                .respondedAt(application.getRespondedAt())
                .rejectionReason(application.getRejectionReason())
                .skills(skills)
                .build();
    }

    // met, heldProficiency and heldYears stay null here. they answer "does this worker hold it",
    // and on the employer's own job there is no worker to answer it against
    private List<JobRequirementDetail> toRequirementDetails(List<JobRequirement> requirements,
                                                            Map<String, Skill> skillsById) {
        if (Objects.isNull(requirements)) {
            return List.of();
        }
        return requirements.stream()
                .map(requirement -> JobRequirementDetail.builder()
                        .skillId(requirement.getSkillId())
                        .skillName(skillName(requirement.getSkillId(), skillsById))
                        .minimumProficiency(requirement.getMinimumProficiency())
                        .minimumYears(requirement.getMinimumYears())
                        .mandatory(requirement.isMandatory())
                        .certificationRequired(requirement.isCertificationRequired())
                        .weight(requirement.getWeight())
                        .build())
                .toList();
    }

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

    private String skillName(String skillId, Map<String, Skill> skillsById) {
        Skill skill = Objects.nonNull(skillsById) ? skillsById.get(skillId) : null;
        return Objects.nonNull(skill) ? skill.getName() : null;
    }

    private int vacancies(Job job) {
        int needed = Objects.nonNull(job.getWorkersNeeded()) ? job.getWorkersNeeded() : 0;
        int assigned = Objects.nonNull(job.getWorkersAssigned()) ? job.getWorkersAssigned() : 0;
        return Math.max(0, needed - assigned);
    }

    private int completionPercentage(EmployerProfile profile) {
        int filled = 0;
        if (StringUtils.hasText(profile.getCompanyName())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getRegistrationNumber())) {
            filled++;
        }
        if (Objects.nonNull(profile.getCompanySize())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getIndustry())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getDescription())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getAddress())) {
            filled++;
        }
        if (Objects.nonNull(profile.getLatitude()) && Objects.nonNull(profile.getLongitude())) {
            filled++;
        }
        if (profile.isVerified()) {
            filled++;
        }
        return filled * 100 / COMPLETION_FIELDS;
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
