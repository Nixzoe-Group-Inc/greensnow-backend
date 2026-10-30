package com.greensnow.worker.service.employee;

import com.greensnow.worker.model.constant.ResponseCode;
import com.greensnow.worker.model.constant.ResponseMessage;
import com.greensnow.worker.model.entity.EmployeeProfile;
import com.greensnow.worker.model.entity.EmployeeSkill;
import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.entity.JobApplication;
import com.greensnow.worker.model.entity.Skill;
import com.greensnow.worker.model.entity.User;
import com.greensnow.worker.model.response.EmployeeProfileResponse;
import com.greensnow.worker.model.response.EmployeeSkillDetail;
import com.greensnow.worker.model.response.EmployeeSkillListResponse;
import com.greensnow.worker.model.response.EmployeeSkillResponse;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.JobApplicationDetail;
import com.greensnow.worker.model.response.JobApplicationListResponse;
import com.greensnow.worker.model.response.JobApplicationResponse;
import com.greensnow.worker.model.response.JobDetail;
import com.greensnow.worker.model.response.JobListResponse;
import com.greensnow.worker.model.response.JobResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EmployeeResponseProcessor {

    // the ten things an employer looks for before they will hire somebody they have never met.
    // counted on read rather than stored, so it cannot go stale against the fields it counts
    private static final int COMPLETION_FIELDS = 10;

    public EmployeeProfileResponse processProfileResponse(String responseMessage, EmployeeProfile profile, User user,
                                                          List<EmployeeSkill> heldSkills, Map<String, Skill> skillsById) {
        List<EmployeeSkillDetail> skills = heldSkills.stream()
                .map(held -> toSkillDetail(held, skillsById.get(held.getSkillId())))
                .toList();

        return EmployeeProfileResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .fullName(composeFullName(user))
                .email(Objects.nonNull(user) ? user.getEmail() : null)
                .phoneNumber(Objects.nonNull(user) ? user.getPhoneNumber() : null)
                .headline(profile.getHeadline())
                .bio(profile.getBio())
                .yearsOfExperience(profile.getYearsOfExperience())
                .educationLevel(profile.getEducationLevel())
                .availabilityStatus(profile.getAvailabilityStatus())
                .hourlyRate(profile.getHourlyRate())
                .dailyRate(profile.getDailyRate())
                .baseAddress(profile.getBaseAddress())
                .baseLatitude(profile.getBaseLatitude())
                .baseLongitude(profile.getBaseLongitude())
                .serviceRadiusKm(profile.getServiceRadiusKm())
                .hasOwnTools(profile.isHasOwnTools())
                .hasTransport(profile.isHasTransport())
                .idVerified(profile.isIdVerified())
                .backgroundChecked(profile.isBackgroundChecked())
                .averageRating(profile.getAverageRating())
                .totalRatings(profile.getTotalRatings())
                .jobsCompleted(profile.getJobsCompleted())
                .jobsCancelled(profile.getJobsCancelled())
                .completionRate(profile.getCompletionRate())
                .profileCompletionPercentage(completionPercentage(profile, heldSkills))
                .lastActiveAt(profile.getLastActiveAt())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .totalSkills(skills.size())
                .skills(skills)
                .build();
    }

    public EmployeeProfileResponse failedProfile(String responseCode, String responseMessage) {
        return EmployeeProfileResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public EmployeeSkillResponse processSkillResponse(String responseMessage, EmployeeSkill skill, Skill catalogue) {
        return EmployeeSkillResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .skill(toSkillDetail(skill, catalogue))
                .build();
    }

    public EmployeeSkillResponse failedSkill(String responseCode, String responseMessage) {
        return EmployeeSkillResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public EmployeeSkillListResponse processSkillListResponse(List<EmployeeSkill> heldSkills,
                                                              Map<String, Skill> skillsById) {
        List<EmployeeSkillDetail> skills = heldSkills.stream()
                .map(held -> toSkillDetail(held, skillsById.get(held.getSkillId())))
                .toList();

        return EmployeeSkillListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.SKILLS_RETRIEVED)
                .totalSkills(skills.size())
                .skills(skills)
                .build();
    }

    public EmployeeSkillListResponse failedSkillList(String responseCode, String responseMessage) {
        return EmployeeSkillListResponse.builder()
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

    public JobResponse processJobResponse(Job job, MatchResult match, boolean alreadyApplied) {
        return JobResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.JOB_RETRIEVED)
                .job(toJobDetail(job, match, alreadyApplied))
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

    public JobApplicationResponse processApplicationResponse(String responseMessage, JobApplication application, Job job) {
        return JobApplicationResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(responseMessage)
                .application(toApplicationDetail(application, job))
                .build();
    }

    public JobApplicationResponse failedApplication(String responseCode, String responseMessage) {
        return JobApplicationResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public JobApplicationListResponse processApplicationListResponse(List<JobApplicationDetail> applications) {
        return JobApplicationListResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.APPLICATIONS_RETRIEVED)
                .totalApplications(applications.size())
                .applications(applications)
                .build();
    }

    public JobApplicationListResponse failedApplicationList(String responseCode, String responseMessage) {
        return JobApplicationListResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public EmployeeSkillDetail toSkillDetail(EmployeeSkill skill, Skill catalogue) {
        return EmployeeSkillDetail.builder()
                .employeeSkillId(skill.getId())
                .skillId(skill.getSkillId())
                .skillName(Objects.nonNull(catalogue) ? catalogue.getName() : null)
                .skillCategory(Objects.nonNull(catalogue) ? catalogue.getCategory() : null)
                .proficiencyLevel(skill.getProficiencyLevel())
                .yearsOfExperience(skill.getYearsOfExperience())
                .certified(skill.isCertified())
                .certificationName(skill.getCertificationName())
                .certificationUrl(skill.getCertificationUrl())
                .certificationExpiresOn(skill.getCertificationExpiresOn())
                // said out loud rather than left for the worker to work out from a date, because
                // an expired certificate is read as no certificate everywhere it is scored
                .certificationExpired(Objects.nonNull(skill.getCertificationExpiresOn())
                        && skill.getCertificationExpiresOn().isBefore(LocalDate.now()))
                .verified(skill.isVerified())
                .build();
    }

    public JobDetail toJobDetail(Job job, MatchResult match, boolean alreadyApplied) {
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
                .distanceKm(Objects.nonNull(match) ? match.getDistanceKm() : null)
                .matchScore(Objects.nonNull(match) ? match.getMatchScore() : null)
                .skillScore(Objects.nonNull(match) ? match.getSkillScore() : null)
                .experienceScore(Objects.nonNull(match) ? match.getExperienceScore() : null)
                .locationScore(Objects.nonNull(match) ? match.getLocationScore() : null)
                .educationScore(Objects.nonNull(match) ? match.getEducationScore() : null)
                .eligible(Objects.nonNull(match) ? match.isEligible() : null)
                .ineligibilityReason(Objects.nonNull(match) ? match.getIneligibilityReason() : null)
                .requirements(Objects.nonNull(match) ? match.getRequirements() : null)
                .alreadyApplied(alreadyApplied)
                .build();
    }

    public JobApplicationDetail toApplicationDetail(JobApplication application, Job job) {
        return JobApplicationDetail.builder()
                .applicationId(application.getId())
                .jobId(application.getJobId())
                .jobTitle(Objects.nonNull(job) ? job.getTitle() : null)
                .jobStatus(Objects.nonNull(job) ? job.getStatus() : null)
                .siteAddress(Objects.nonNull(job) ? job.getSiteAddress() : null)
                .scheduledStartDate(Objects.nonNull(job) ? job.getScheduledStartDate() : null)
                .employerProfileId(Objects.nonNull(job) ? job.getEmployerProfileId() : null)
                .status(application.getStatus())
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
                .build();
    }

    private int vacancies(Job job) {
        int needed = Objects.nonNull(job.getWorkersNeeded()) ? job.getWorkersNeeded() : 0;
        int assigned = Objects.nonNull(job.getWorkersAssigned()) ? job.getWorkersAssigned() : 0;
        return Math.max(0, needed - assigned);
    }

    private int completionPercentage(EmployeeProfile profile, List<EmployeeSkill> heldSkills) {
        int filled = 0;
        if (StringUtils.hasText(profile.getHeadline())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getBio())) {
            filled++;
        }
        if (Objects.nonNull(profile.getYearsOfExperience())) {
            filled++;
        }
        if (Objects.nonNull(profile.getEducationLevel())) {
            filled++;
        }
        if (positive(profile.getHourlyRate()) || positive(profile.getDailyRate())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getBaseAddress())) {
            filled++;
        }
        if (Objects.nonNull(profile.getBaseLatitude()) && Objects.nonNull(profile.getBaseLongitude())) {
            filled++;
        }
        if (Objects.nonNull(profile.getServiceRadiusKm()) && profile.getServiceRadiusKm() > 0) {
            filled++;
        }
        if (!heldSkills.isEmpty()) {
            filled++;
        }
        if (profile.isIdVerified()) {
            filled++;
        }
        return filled * 100 / COMPLETION_FIELDS;
    }

    private boolean positive(BigDecimal value) {
        return Objects.nonNull(value) && value.compareTo(BigDecimal.ZERO) > 0;
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
