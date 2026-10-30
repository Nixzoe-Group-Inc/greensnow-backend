package com.ayitech.worker.service.employee;

import com.ayitech.worker.model.entity.EmployeeProfile;
import com.ayitech.worker.model.entity.EmployeeSkill;
import com.ayitech.worker.model.entity.Job;
import com.ayitech.worker.model.entity.JobApplication;
import com.ayitech.worker.model.enums.ApplicationStatus;
import com.ayitech.worker.model.enums.AvailabilityStatus;
import com.ayitech.worker.model.request.EmployeeProfileRequest;
import com.ayitech.worker.model.request.EmployeeSkillRequest;
import com.ayitech.worker.model.request.JobApplicationRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class EmployeeRequestBuilder {

    public EmployeeProfile buildProfile(String userId, EmployeeProfileRequest request) {
        return EmployeeProfile.builder()
                .userId(userId)
                .headline(trimmed(request.getHeadline()))
                .bio(trimmed(request.getBio()))
                .yearsOfExperience(request.getYearsOfExperience())
                .educationLevel(request.getEducationLevel())
                // a new worker is available unless they say otherwise - the whole point of
                // signing up is to be offered work
                .availabilityStatus(Objects.nonNull(request.getAvailabilityStatus())
                        ? request.getAvailabilityStatus()
                        : AvailabilityStatus.AVAILABLE)
                .hourlyRate(request.getHourlyRate())
                .dailyRate(request.getDailyRate())
                .baseAddress(trimmed(request.getBaseAddress()))
                .baseLatitude(request.getBaseLatitude())
                .baseLongitude(request.getBaseLongitude())
                .serviceRadiusKm(request.getServiceRadiusKm())
                .hasOwnTools(Boolean.TRUE.equals(request.getHasOwnTools()))
                .hasTransport(Boolean.TRUE.equals(request.getHasTransport()))
                // none of these are the worker's to claim. verification is the platform's and
                // the counts are what the platform has watched them do
                .idVerified(false)
                .backgroundChecked(false)
                .averageRating(BigDecimal.ZERO)
                .totalRatings(0)
                .jobsCompleted(0)
                .jobsCancelled(0)
                .completionRate(BigDecimal.ZERO)
                .lastActiveAt(LocalDateTime.now())
                .build();
    }

    // an update is the whole profile, not a patch: what the request carries is what the worker
    // reads as afterwards, and a field left out is cleared. the platform-owned fields are absent
    // from the request and so are never touched here - verification, the ratings and the counts
    // all stay exactly as the platform left them
    public EmployeeProfile buildProfileUpdate(EmployeeProfile profile, EmployeeProfileRequest request) {
        profile.setHeadline(trimmed(request.getHeadline()));
        profile.setBio(trimmed(request.getBio()));
        profile.setYearsOfExperience(request.getYearsOfExperience());
        profile.setEducationLevel(request.getEducationLevel());
        profile.setHourlyRate(request.getHourlyRate());
        profile.setDailyRate(request.getDailyRate());
        profile.setBaseAddress(trimmed(request.getBaseAddress()));
        profile.setBaseLatitude(request.getBaseLatitude());
        profile.setBaseLongitude(request.getBaseLongitude());
        profile.setServiceRadiusKm(request.getServiceRadiusKm());
        profile.setHasOwnTools(Boolean.TRUE.equals(request.getHasOwnTools()));
        profile.setHasTransport(Boolean.TRUE.equals(request.getHasTransport()));
        // availability is the one field an update must not clear. it is also set from its own
        // route, so a worker who marked themselves busy this morning and then edited their bio
        // would otherwise be offered work again without ever saying they were free
        profile.setAvailabilityStatus(Objects.nonNull(request.getAvailabilityStatus())
                ? request.getAvailabilityStatus()
                : profile.getAvailabilityStatus());
        profile.setLastActiveAt(LocalDateTime.now());
        return profile;
    }

    public EmployeeSkill buildEmployeeSkill(String employeeProfileId, EmployeeSkillRequest request) {
        return EmployeeSkill.builder()
                .employeeProfileId(employeeProfileId)
                .skillId(request.getSkillId())
                .proficiencyLevel(request.getProficiencyLevel())
                .yearsOfExperience(Objects.nonNull(request.getYearsOfExperience()) ? request.getYearsOfExperience() : 0)
                .certified(Boolean.TRUE.equals(request.getCertified()))
                .certificationName(trimmed(request.getCertificationName()))
                .certificationUrl(trimmed(request.getCertificationUrl()))
                .certificationExpiresOn(request.getCertificationExpiresOn())
                // the worker says they hold it; the platform says whether it checked. a worker
                // who could set this would be marking their own certificate as seen
                .verified(false)
                .build();
    }

    // the same whole-record rule. the skill the row is for is not restated, because a worker
    // editing what they claim for plumbing is not thereby claiming carpentry, and verified stays
    // where it is - a worker who could clear it would un-check their own certificate
    public EmployeeSkill buildSkillUpdate(EmployeeSkill skill, EmployeeSkillRequest request) {
        skill.setProficiencyLevel(request.getProficiencyLevel());
        skill.setYearsOfExperience(Objects.nonNull(request.getYearsOfExperience()) ? request.getYearsOfExperience() : 0);
        skill.setCertified(Boolean.TRUE.equals(request.getCertified()));
        skill.setCertificationName(trimmed(request.getCertificationName()));
        skill.setCertificationUrl(trimmed(request.getCertificationUrl()));
        skill.setCertificationExpiresOn(request.getCertificationExpiresOn());
        return skill;
    }

    // the scores are copied onto the row rather than referenced, because they are the answer to
    // "how did this worker look on the day they applied". the profile moves underneath them -
    // skills get added, ratings change, the worker moves house - so a score recomputed next week
    // is not the one the employer is being asked about
    public JobApplication buildApplication(Job job, EmployeeProfile profile,
                                           JobApplicationRequest request, MatchResult match) {
        return JobApplication.builder()
                .jobId(job.getId())
                .employeeProfileId(profile.getId())
                .status(ApplicationStatus.APPLIED)
                .matchScore(match.getMatchScore())
                .skillScore(match.getSkillScore())
                .experienceScore(match.getExperienceScore())
                .locationScore(match.getLocationScore())
                .educationScore(match.getEducationScore())
                .distanceKm(match.getDistanceKm())
                .coverNote(trimmed(request.getCoverNote()))
                .proposedRate(request.getProposedRate())
                .appliedAt(LocalDateTime.now())
                .build();
    }

    private String trimmed(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
