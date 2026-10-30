package com.greensnow.worker.service.employer;

import com.greensnow.worker.model.entity.EmployerProfile;
import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.entity.JobApproval;
import com.greensnow.worker.model.entity.JobRequirement;
import com.greensnow.worker.model.enums.ApprovalAction;
import com.greensnow.worker.model.enums.ApprovalStatus;
import com.greensnow.worker.model.enums.JobStatus;
import com.greensnow.worker.model.request.EmployerProfileRequest;
import com.greensnow.worker.model.request.JobRequest;
import com.greensnow.worker.model.request.JobRequirementRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class EmployerRequestBuilder {

    private static final String DEFAULT_CURRENCY = "GHS";
    private static final BigDecimal DEFAULT_REQUIREMENT_WEIGHT = BigDecimal.ONE;

    public EmployerProfile buildProfile(String userId, EmployerProfileRequest request) {
        return EmployerProfile.builder()
                .userId(userId)
                .companyName(trimmed(request.getCompanyName()))
                .registrationNumber(trimmed(request.getRegistrationNumber()))
                .companySize(request.getCompanySize())
                .industry(trimmed(request.getIndustry()))
                .website(trimmed(request.getWebsite()))
                .description(trimmed(request.getDescription()))
                .address(trimmed(request.getAddress()))
                .city(trimmed(request.getCity()))
                .country(trimmed(request.getCountry()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                // none of these are the employer's to claim. the badge is the platform's, the
                // switch that lets a company's jobs skip the approval queue is the platform's,
                // and the counts are what the platform has watched them do
                .verified(false)
                .autoApproveJobs(false)
                .averageRating(BigDecimal.ZERO)
                .totalRatings(0)
                .jobsPosted(0)
                .jobsCompleted(0)
                .build();
    }

    // an update is the whole profile, not a patch: what the request carries is what the company
    // reads as afterwards, and a field left out is cleared. the platform-owned fields are absent
    // from the request and so are never touched here - the badge, the approval switch, the
    // ratings and the counts all stay exactly as the platform left them
    public EmployerProfile buildProfileUpdate(EmployerProfile profile, EmployerProfileRequest request) {
        profile.setCompanyName(trimmed(request.getCompanyName()));
        profile.setRegistrationNumber(trimmed(request.getRegistrationNumber()));
        profile.setCompanySize(request.getCompanySize());
        profile.setIndustry(trimmed(request.getIndustry()));
        profile.setWebsite(trimmed(request.getWebsite()));
        profile.setDescription(trimmed(request.getDescription()));
        profile.setAddress(trimmed(request.getAddress()));
        profile.setCity(trimmed(request.getCity()));
        profile.setCountry(trimmed(request.getCountry()));
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        return profile;
    }

    // a job is always born a draft. nothing on the request can set a status, so the only way to
    // reach APPROVED is through the approval step - which is the whole reason that step exists
    public Job buildJob(String employerProfileId, JobRequest request) {
        return Job.builder()
                .employerProfileId(employerProfileId)
                .title(trimmed(request.getTitle()))
                .description(trimmed(request.getDescription()))
                .jobType(request.getJobType())
                .experienceLevel(request.getExperienceLevel())
                .status(JobStatus.DRAFT)
                .siteAddress(trimmed(request.getSiteAddress()))
                .siteLatitude(request.getSiteLatitude())
                .siteLongitude(request.getSiteLongitude())
                .siteRadiusMeters(request.getSiteRadiusMeters())
                .hourlyRate(request.getHourlyRate())
                .dailyRate(request.getDailyRate())
                .totalBudget(request.getTotalBudget())
                .currency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim() : DEFAULT_CURRENCY)
                .scheduledStartDate(request.getScheduledStartDate())
                .scheduledEndDate(request.getScheduledEndDate())
                .estimatedDurationHours(request.getEstimatedDurationHours())
                .maxDailyHours(request.getMaxDailyHours())
                .extensionsAllowed(Objects.nonNull(request.getExtensionsAllowed()) ? request.getExtensionsAllowed() : 0)
                .workersNeeded(Objects.nonNull(request.getWorkersNeeded()) ? request.getWorkersNeeded() : 1)
                .workersAssigned(0)
                .toolsProvided(Boolean.TRUE.equals(request.getToolsProvided()))
                .transportProvided(Boolean.TRUE.equals(request.getTransportProvided()))
                .urgent(Boolean.TRUE.equals(request.getUrgent()))
                .viewCount(0)
                .applicationCount(0)
                .build();
    }

    // the same whole-record rule as the profile. only a draft or a job sent back for changes ever
    // reaches here, so nothing a worker has already read can be rewritten underneath them - and
    // the status, the staffing tally and the view and application counts are not on the request,
    // so an edit cannot move a job along or restate how much interest it has had
    public Job buildJobUpdate(Job job, JobRequest request) {
        job.setTitle(trimmed(request.getTitle()));
        job.setDescription(trimmed(request.getDescription()));
        job.setJobType(request.getJobType());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setSiteAddress(trimmed(request.getSiteAddress()));
        job.setSiteLatitude(request.getSiteLatitude());
        job.setSiteLongitude(request.getSiteLongitude());
        job.setSiteRadiusMeters(request.getSiteRadiusMeters());
        job.setHourlyRate(request.getHourlyRate());
        job.setDailyRate(request.getDailyRate());
        job.setTotalBudget(request.getTotalBudget());
        job.setCurrency(StringUtils.hasText(request.getCurrency()) ? request.getCurrency().trim() : DEFAULT_CURRENCY);
        job.setScheduledStartDate(request.getScheduledStartDate());
        job.setScheduledEndDate(request.getScheduledEndDate());
        job.setEstimatedDurationHours(request.getEstimatedDurationHours());
        job.setMaxDailyHours(request.getMaxDailyHours());
        job.setExtensionsAllowed(Objects.nonNull(request.getExtensionsAllowed()) ? request.getExtensionsAllowed() : 0);
        job.setWorkersNeeded(Objects.nonNull(request.getWorkersNeeded()) ? request.getWorkersNeeded() : 1);
        job.setToolsProvided(Boolean.TRUE.equals(request.getToolsProvided()));
        job.setTransportProvided(Boolean.TRUE.equals(request.getTransportProvided()));
        job.setUrgent(Boolean.TRUE.equals(request.getUrgent()));
        return job;
    }

    public List<JobRequirement> buildRequirements(String jobId, List<JobRequirementRequest> requests) {
        return requests.stream()
                .map(requirement -> buildRequirement(jobId, requirement))
                .toList();
    }

    // weight defaults to one rather than zero. a requirement weighted zero counts for nothing in
    // the match, so an employer who simply did not fill the field in would be listing a skill
    // the scoring then ignores
    private JobRequirement buildRequirement(String jobId, JobRequirementRequest request) {
        return JobRequirement.builder()
                .jobId(jobId)
                .skillId(request.getSkillId())
                .minimumProficiency(request.getMinimumProficiency())
                .minimumYears(Objects.nonNull(request.getMinimumYears()) ? request.getMinimumYears() : 0)
                .mandatory(Boolean.TRUE.equals(request.getMandatory()))
                .certificationRequired(Boolean.TRUE.equals(request.getCertificationRequired()))
                .weight(Objects.nonNull(request.getWeight()) ? request.getWeight() : DEFAULT_REQUIREMENT_WEIGHT)
                .build();
    }

    // the employer's own entry in the job's approval history. the reviewer is left null because
    // nobody has reviewed it yet - this row is the submission itself, not an answer to it
    public JobApproval buildSubmission(String jobId) {
        return JobApproval.builder()
                .jobId(jobId)
                .status(ApprovalStatus.PENDING)
                .action(ApprovalAction.SUBMITTED)
                .build();
    }

    public JobApproval buildResubmission(String jobId) {
        return JobApproval.builder()
                .jobId(jobId)
                .status(ApprovalStatus.PENDING)
                .action(ApprovalAction.RESUBMITTED)
                .build();
    }

    // a company the platform has already trusted skips the queue, and the row says so rather
    // than naming a reviewer who never looked at it
    public JobApproval buildAutoApproval(String jobId) {
        return JobApproval.builder()
                .jobId(jobId)
                .status(ApprovalStatus.APPROVED)
                .action(ApprovalAction.AUTO_APPROVED)
                .reviewedAt(LocalDateTime.now())
                .reviewDurationSeconds(0L)
                .build();
    }

    private String trimmed(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
