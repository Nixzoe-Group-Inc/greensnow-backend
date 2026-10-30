package com.ayitech.worker.service.employee;

import com.ayitech.worker.model.constant.ResponseMessage;
import com.ayitech.worker.model.entity.EmployeeProfile;
import com.ayitech.worker.model.entity.EmployeeSkill;
import com.ayitech.worker.model.entity.Job;
import com.ayitech.worker.model.entity.JobRequirement;
import com.ayitech.worker.model.entity.Skill;
import com.ayitech.worker.model.enums.ExperienceLevel;
import com.ayitech.worker.model.enums.ProficiencyLevel;
import com.ayitech.worker.model.response.JobRequirementDetail;
import com.ayitech.worker.util.GeoUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class JobMatchingService {

    private static final int SCORE_SCALE = 2;
    private static final BigDecimal FULL_SCORE = BigDecimal.valueOf(100);
    private static final BigDecimal NO_SCORE = BigDecimal.ZERO;

    // a requirement is two thirds about being good enough at the trade and one third about
    // having done it long enough - someone rated EXPERT with one year is not the same hire
    // as someone rated ADVANCED with eight
    private static final BigDecimal PROFICIENCY_SHARE = BigDecimal.valueOf(0.65);
    private static final BigDecimal YEARS_SHARE = BigDecimal.valueOf(0.35);

    // inside the worker's own service radius a job scores between 100 at the doorstep and 60
    // at the edge, so near work is preferred without burying work that is merely across town
    private static final BigDecimal WITHIN_RADIUS_FLOOR = BigDecimal.valueOf(60);
    private static final BigDecimal WITHIN_RADIUS_RANGE = BigDecimal.valueOf(40);

    // neither side has coordinates, so the distance is unknown rather than zero. scoring it
    // 100 would make every job without a site location look local; scoring it 0 would bury
    // every job an employer has not geocoded yet
    private static final BigDecimal UNKNOWN_LOCATION_SCORE = BigDecimal.valueOf(50);
    private static final BigDecimal UNKNOWN_EDUCATION_SCORE = BigDecimal.valueOf(70);

    private static final int DEFAULT_SERVICE_RADIUS_KM = 25;

    @Value("${app.matching.skill-weight}")
    private BigDecimal skillWeight;

    @Value("${app.matching.experience-weight}")
    private BigDecimal experienceWeight;

    @Value("${app.matching.location-weight}")
    private BigDecimal locationWeight;

    @Value("${app.matching.education-weight}")
    private BigDecimal educationWeight;

    @Value("${app.matching.min-match-score}")
    private BigDecimal minimumMatchScore;

    public MatchResult score(Job job, EmployeeProfile profile, List<EmployeeSkill> heldSkills,
                             List<JobRequirement> requirements, Map<String, Skill> skillsById) {

        Map<String, EmployeeSkill> heldBySkillId = heldSkills.stream()
                .filter(held -> Objects.nonNull(held.getSkillId()))
                .collect(java.util.stream.Collectors.toMap(EmployeeSkill::getSkillId, held -> held, (a, b) -> a));

        List<JobRequirementDetail> assessed = requirements.stream()
                .map(requirement -> assess(requirement, heldBySkillId.get(requirement.getSkillId()), skillsById))
                .toList();

        BigDecimal skillScore = skillScore(assessed, requirements, heldSkills);
        BigDecimal experienceScore = experienceScore(job.getExperienceLevel(), profile.getYearsOfExperience());
        BigDecimal distanceKm = GeoUtility.distanceKm(profile.getBaseLatitude(), profile.getBaseLongitude(),
                job.getSiteLatitude(), job.getSiteLongitude());
        BigDecimal locationScore = locationScore(distanceKm, profile.getServiceRadiusKm());
        BigDecimal educationScore = educationScore(profile);

        BigDecimal matchScore = weighted(skillScore, skillWeight)
                .add(weighted(experienceScore, experienceWeight))
                .add(weighted(locationScore, locationWeight))
                .add(weighted(educationScore, educationWeight))
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);

        String ineligibilityReason = ineligibilityReason(assessed, matchScore);

        return MatchResult.builder()
                .matchScore(matchScore)
                .skillScore(skillScore)
                .experienceScore(experienceScore)
                .locationScore(locationScore)
                .educationScore(educationScore)
                .distanceKm(distanceKm)
                .eligible(Objects.isNull(ineligibilityReason))
                .ineligibilityReason(ineligibilityReason)
                .requirements(assessed)
                .build();
    }

    public BigDecimal getMinimumMatchScore() {
        return minimumMatchScore;
    }

    private JobRequirementDetail assess(JobRequirement requirement, EmployeeSkill held, Map<String, Skill> skillsById) {
        Skill skill = skillsById.get(requirement.getSkillId());

        boolean proficiencyMet = Objects.nonNull(held)
                && Objects.nonNull(held.getProficiencyLevel())
                && held.getProficiencyLevel().meets(requirement.getMinimumProficiency());

        boolean yearsMet = Objects.nonNull(held) && years(held) >= minimumYears(requirement);

        // a certificate the worker no longer holds is not a certificate. an expired one is
        // exactly the case an employer cannot check on site, so it is read as absent
        boolean certificationMet = !requirement.isCertificationRequired()
                || (Objects.nonNull(held) && held.isCertified() && !expired(held));

        return JobRequirementDetail.builder()
                .skillId(requirement.getSkillId())
                .skillName(Objects.nonNull(skill) ? skill.getName() : null)
                .minimumProficiency(requirement.getMinimumProficiency())
                .minimumYears(requirement.getMinimumYears())
                .mandatory(requirement.isMandatory())
                .certificationRequired(requirement.isCertificationRequired())
                .weight(weightOf(requirement))
                .met(proficiencyMet && yearsMet && certificationMet)
                .heldProficiency(Objects.nonNull(held) ? held.getProficiencyLevel() : null)
                .heldYears(Objects.nonNull(held) ? held.getYearsOfExperience() : null)
                .build();
    }

    private BigDecimal skillScore(List<JobRequirementDetail> assessed, List<JobRequirement> requirements,
                                  List<EmployeeSkill> heldSkills) {

        // a job that lists no skills at all cannot be scored on skills. anyone holding a trade
        // scores full marks rather than zero, which would make an unspecified job unreachable
        if (requirements.isEmpty()) {
            return heldSkills.isEmpty() ? NO_SCORE : FULL_SCORE;
        }

        BigDecimal earned = BigDecimal.ZERO;
        BigDecimal available = BigDecimal.ZERO;

        for (int index = 0; index < assessed.size(); index++) {
            JobRequirementDetail detail = assessed.get(index);
            BigDecimal weight = weightOf(requirements.get(index));
            available = available.add(weight);
            earned = earned.add(weight.multiply(requirementScore(detail, requirements.get(index))));
        }

        if (available.compareTo(BigDecimal.ZERO) == 0) {
            return NO_SCORE;
        }

        return earned.divide(available, SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal requirementScore(JobRequirementDetail detail, JobRequirement requirement) {
        if (Objects.isNull(detail.getHeldProficiency())) {
            return NO_SCORE;
        }

        // a certificate the job insists on is not something partial credit can stand in for
        if (requirement.isCertificationRequired() && !detail.isMet()
                && Objects.nonNull(detail.getHeldProficiency())) {
            boolean onlyCertificationMissing = detail.getHeldProficiency().meets(requirement.getMinimumProficiency())
                    && heldYears(detail) >= minimumYears(requirement);
            if (onlyCertificationMissing) {
                return NO_SCORE;
            }
        }

        BigDecimal proficiencyPoints = ratio(detail.getHeldProficiency().getRank(),
                rankOf(requirement.getMinimumProficiency()));
        BigDecimal yearsPoints = ratio(heldYears(detail), minimumYears(requirement));

        return proficiencyPoints.multiply(PROFICIENCY_SHARE)
                .add(yearsPoints.multiply(YEARS_SHARE))
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal experienceScore(ExperienceLevel required, Integer yearsOfExperience) {
        int years = Objects.nonNull(yearsOfExperience) ? yearsOfExperience : 0;

        // a job that names no experience level is asking for none
        if (Objects.isNull(required) || required.getMinYears() <= 0) {
            return FULL_SCORE;
        }

        return ratio(years, required.getMinYears());
    }

    private BigDecimal locationScore(BigDecimal distanceKm, Integer serviceRadiusKm) {
        if (Objects.isNull(distanceKm)) {
            return UNKNOWN_LOCATION_SCORE;
        }

        int radius = Objects.nonNull(serviceRadiusKm) && serviceRadiusKm > 0
                ? serviceRadiusKm
                : DEFAULT_SERVICE_RADIUS_KM;

        BigDecimal radiusKm = BigDecimal.valueOf(radius);

        if (distanceKm.compareTo(radiusKm) <= 0) {
            BigDecimal travelled = distanceKm.divide(radiusKm, 4, RoundingMode.HALF_UP);
            return FULL_SCORE.subtract(WITHIN_RADIUS_RANGE.multiply(travelled))
                    .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
        }

        // beyond the radius the worker said they travel. still reachable, and still offered -
        // a worker who wants the job can take it - but it falls away the further out it is
        if (distanceKm.compareTo(BigDecimal.ZERO) == 0) {
            return WITHIN_RADIUS_FLOOR;
        }

        return WITHIN_RADIUS_FLOOR.multiply(radiusKm)
                .divide(distanceKm, SCORE_SCALE, RoundingMode.HALF_UP);
    }

    // jobs carry no education requirement, so this scores the worker's own level against what
    // skilled trade work actually asks for: a vocational qualification or a served
    // apprenticeship is the ceiling, and a degree above it is worth no more to a wiring job.
    // the moment jobs carry a required level, this is the method that reads it
    private BigDecimal educationScore(EmployeeProfile profile) {
        if (Objects.isNull(profile.getEducationLevel())) {
            return UNKNOWN_EDUCATION_SCORE;
        }

        return switch (profile.getEducationLevel()) {
            case NONE -> BigDecimal.valueOf(40);
            case PRIMARY -> BigDecimal.valueOf(55);
            case SECONDARY -> BigDecimal.valueOf(70);
            default -> FULL_SCORE;
        };
    }

    private String ineligibilityReason(List<JobRequirementDetail> assessed, BigDecimal matchScore) {
        boolean mandatoryMissing = assessed.stream()
                .anyMatch(detail -> detail.isMandatory() && !detail.isMet());

        if (mandatoryMissing) {
            return ResponseMessage.MANDATORY_SKILL_MISSING;
        }

        if (matchScore.compareTo(minimumMatchScore) < 0) {
            return String.format(ResponseMessage.MATCH_SCORE_TOO_LOW,
                    matchScore.stripTrailingZeros().toPlainString(),
                    minimumMatchScore.stripTrailingZeros().toPlainString());
        }

        return null;
    }

    private BigDecimal weighted(BigDecimal score, BigDecimal weight) {
        return score.multiply(weight);
    }

    // held over required, capped at full marks - exceeding what was asked for is not worth
    // more than meeting it, or one veteran would crowd out every qualified applicant
    private BigDecimal ratio(int held, int required) {
        if (required <= 0) {
            return FULL_SCORE;
        }
        if (held >= required) {
            return FULL_SCORE;
        }
        return BigDecimal.valueOf(held)
                .multiply(FULL_SCORE)
                .divide(BigDecimal.valueOf(required), SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private int rankOf(ProficiencyLevel level) {
        return Objects.nonNull(level) ? level.getRank() : 0;
    }

    private int minimumYears(JobRequirement requirement) {
        return Objects.nonNull(requirement.getMinimumYears()) ? requirement.getMinimumYears() : 0;
    }

    private int years(EmployeeSkill held) {
        return Objects.nonNull(held.getYearsOfExperience()) ? held.getYearsOfExperience() : 0;
    }

    private int heldYears(JobRequirementDetail detail) {
        return Objects.nonNull(detail.getHeldYears()) ? detail.getHeldYears() : 0;
    }

    private boolean expired(EmployeeSkill held) {
        return Objects.nonNull(held.getCertificationExpiresOn())
                && held.getCertificationExpiresOn().isBefore(LocalDate.now());
    }

    private BigDecimal weightOf(JobRequirement requirement) {
        return Objects.nonNull(requirement.getWeight()) && requirement.getWeight().compareTo(BigDecimal.ZERO) > 0
                ? requirement.getWeight()
                : BigDecimal.ONE;
    }
}
