package com.greensnow.worker;

import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.entity.JobRequirement;
import com.greensnow.worker.model.entity.Skill;
import com.greensnow.worker.model.enums.ApplicationStatus;
import com.greensnow.worker.model.enums.AvailabilityStatus;
import com.greensnow.worker.model.enums.EducationLevel;
import com.greensnow.worker.model.enums.ExperienceLevel;
import com.greensnow.worker.model.enums.JobStatus;
import com.greensnow.worker.model.enums.JobType;
import com.greensnow.worker.model.enums.ProficiencyLevel;
import com.greensnow.worker.model.enums.UserType;
import com.greensnow.worker.model.request.AvailabilityRequest;
import com.greensnow.worker.model.request.EmployeeProfileRequest;
import com.greensnow.worker.model.request.EmployeeSkillRequest;
import com.greensnow.worker.model.request.JobApplicationRequest;
import com.greensnow.worker.model.request.RegistrationRequest;
import com.greensnow.worker.model.response.EmployeeProfileResponse;
import com.greensnow.worker.model.response.EmployeeSkillResponse;
import com.greensnow.worker.model.response.JobApplicationResponse;
import com.greensnow.worker.model.response.JobListResponse;
import com.greensnow.worker.model.response.JobResponse;
import com.greensnow.worker.repository.JobRepository;
import com.greensnow.worker.repository.JobRequirementRepository;
import com.greensnow.worker.repository.SkillRepository;
import com.greensnow.worker.service.auth.AuthService;
import com.greensnow.worker.service.employee.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmployeeSmokeTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobRequirementRepository jobRequirementRepository;

    @Test
    void aWorkerBuildsAProfilePicksUpASkillFindsAJobAndAppliesForIt() {
        String userId = registerWorker("ama");

        // nothing exists until the worker creates it, and reading before that says so rather
        // than inventing an empty profile
        assertThat(employeeService.getProfile(userId).getResponseCode()).isEqualTo("104");

        EmployeeProfileResponse created = employeeService.createProfile(userId, EmployeeProfileRequest.builder()
                .headline("Plumber, 9 years")
                .bio("Pipework, bathrooms, emergency callouts")
                .yearsOfExperience(9)
                .educationLevel(EducationLevel.VOCATIONAL)
                .hourlyRate(new BigDecimal("45.00"))
                .baseAddress("Osu, Accra")
                .baseLatitude(new BigDecimal("5.5560"))
                .baseLongitude(new BigDecimal("-0.1869"))
                .serviceRadiusKm(30)
                .hasOwnTools(true)
                .hasTransport(true)
                .build());

        assertThat(created.getResponseCode()).isEqualTo("01");
        // a new worker is available unless they said otherwise, and nothing the platform owns
        // can be claimed on the way in
        assertThat(created.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(created.isIdVerified()).isFalse();
        assertThat(created.getFullName()).isEqualTo("Ama Owusu");

        // a second profile for the same worker is refused rather than shadowing the first
        assertThat(employeeService.createProfile(userId, EmployeeProfileRequest.builder().build())
                .getResponseCode()).isEqualTo("109");

        Skill plumbing = skillRepository.save(Skill.builder()
                .name("Plumbing").slug("plumbing-" + UUID.randomUUID()).category("Construction")
                .requiresCertification(false).active(true).build());

        EmployeeSkillResponse addedSkill = employeeService.addSkill(userId, EmployeeSkillRequest.builder()
                .skillId(plumbing.getId())
                .proficiencyLevel(ProficiencyLevel.ADVANCED)
                .yearsOfExperience(9)
                .build());

        assertThat(addedSkill.getResponseCode()).isEqualTo("01");
        // the worker says they hold it; the platform says whether it checked
        assertThat(addedSkill.getSkill().isVerified()).isFalse();
        assertThat(addedSkill.getSkill().getSkillName()).isEqualTo("Plumbing");

        // the same trade twice is refused
        assertThat(employeeService.addSkill(userId, EmployeeSkillRequest.builder()
                .skillId(plumbing.getId()).proficiencyLevel(ProficiencyLevel.EXPERT).build())
                .getResponseCode()).isEqualTo("109");

        // a trade the catalogue does not hold is refused too
        assertThat(employeeService.addSkill(userId, EmployeeSkillRequest.builder()
                .skillId("not-a-skill").proficiencyLevel(ProficiencyLevel.EXPERT).build())
                .getResponseCode()).isEqualTo("110");

        Job job = approvedJob(plumbing.getId(), ProficiencyLevel.INTERMEDIATE);

        JobListResponse browsed = employeeService.browseJobs(userId, null, null, null);
        assertThat(browsed.getResponseCode()).isEqualTo("01");
        assertThat(browsed.getJobs()).extracting("jobId").contains(job.getId());

        JobResponse read = employeeService.getJob(userId, job.getId());
        assertThat(read.getResponseCode()).isEqualTo("01");
        assertThat(read.getJob().getEligible()).isTrue();
        assertThat(read.getJob().getAlreadyApplied()).isFalse();
        // the distance is worked out from both sets of coordinates, so a near job reads as near
        assertThat(read.getJob().getDistanceKm()).isNotNull();

        JobApplicationResponse applied = employeeService.applyForJob(userId, job.getId(),
                JobApplicationRequest.builder().coverNote("Available from Monday").build());

        assertThat(applied.getResponseCode()).isEqualTo("01");
        assertThat(applied.getApplication().getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        // the score is copied onto the row, so it answers how the worker looked on the day
        assertThat(applied.getApplication().getMatchScore()).isNotNull();

        // asking twice for the same job is refused
        assertThat(employeeService.applyForJob(userId, job.getId(), JobApplicationRequest.builder().build())
                .getResponseCode()).isEqualTo("109");

        assertThat(employeeService.listApplications(userId, null).getTotalApplications()).isEqualTo(1);
        assertThat(employeeService.listApplications(userId, ApplicationStatus.ACCEPTED)
                .getTotalApplications()).isZero();

        String applicationId = applied.getApplication().getApplicationId();
        assertThat(employeeService.withdrawApplication(userId, applicationId)
                .getApplication().getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);

        // once taken back it cannot be taken back again
        assertThat(employeeService.withdrawApplication(userId, applicationId).getResponseCode()).isEqualTo("110");
    }

    @Test
    void aWorkerWhoHoldsNoneOfWhatTheJobAsksForIsNotLetThrough() {
        String userId = registerWorker("kofi");

        employeeService.createProfile(userId, EmployeeProfileRequest.builder()
                .headline("Carpenter").yearsOfExperience(4).build());

        Skill carpentry = skillRepository.save(Skill.builder()
                .name("Carpentry").slug("carpentry-" + UUID.randomUUID()).category("Construction")
                .requiresCertification(false).active(true).build());

        Skill welding = skillRepository.save(Skill.builder()
                .name("Welding").slug("welding-" + UUID.randomUUID()).category("Construction")
                .requiresCertification(false).active(true).build());

        employeeService.addSkill(userId, EmployeeSkillRequest.builder()
                .skillId(carpentry.getId()).proficiencyLevel(ProficiencyLevel.ADVANCED).yearsOfExperience(4).build());

        Job weldingJob = approvedJob(welding.getId(), ProficiencyLevel.ADVANCED);

        // it is still browsable - a worker is entitled to see work they are a poor match for
        assertThat(employeeService.browseJobs(userId, null, null, null).getJobs())
                .extracting("jobId").contains(weldingJob.getId());

        // but the matches screen does not offer it, and applying is refused
        assertThat(employeeService.matchingJobs(userId, null, null).getJobs())
                .extracting("jobId").doesNotContain(weldingJob.getId());

        assertThat(employeeService.applyForJob(userId, weldingJob.getId(),
                JobApplicationRequest.builder().build()).getResponseCode()).isEqualTo("110");
    }

    @Test
    void theWorkerCannotPutThemselvesUnderOrOutOfASuspension() {
        String userId = registerWorker("yaw");
        employeeService.createProfile(userId, EmployeeProfileRequest.builder().headline("Electrician").build());

        assertThat(employeeService.updateAvailability(userId,
                AvailabilityRequest.builder().availabilityStatus(AvailabilityStatus.BUSY).build())
                .getResponseCode()).isEqualTo("01");

        // suspension is the platform's to set, so posting it is refused
        assertThat(employeeService.updateAvailability(userId,
                AvailabilityRequest.builder().availabilityStatus(AvailabilityStatus.SUSPENDED).build())
                .getResponseCode()).isEqualTo("105");

        assertThat(employeeService.updateAvailability(userId, AvailabilityRequest.builder().build())
                .getResponseCode()).isEqualTo("110");
    }

    @Test
    void anUnapprovedJobAnswersExactlyAsAJobThatDoesNotExist() {
        String userId = registerWorker("esi");
        employeeService.createProfile(userId, EmployeeProfileRequest.builder().headline("Mason").build());

        Job draft = jobRepository.save(Job.builder()
                .employerProfileId("employer-1").title("Unapproved roof repair").description("Not yet approved")
                .jobType(JobType.ONE_OFF).experienceLevel(ExperienceLevel.MID).status(JobStatus.PENDING_APPROVAL)
                .workersNeeded(1).workersAssigned(0).applicationCount(0).build());

        assertThat(employeeService.getJob(userId, draft.getId()).getResponseCode()).isEqualTo("104");
        assertThat(employeeService.getJob(userId, "no-such-job").getResponseCode()).isEqualTo("104");

        // and it is never offered on the browse list either
        assertThat(employeeService.browseJobs(userId, null, null, null).getJobs())
                .extracting("jobId").doesNotContain(draft.getId());
    }

    private String registerWorker(String handle) {
        String unique = handle + "-" + UUID.randomUUID().toString().substring(0, 8);
        return authService.registerUser(RegistrationRequest.builder()
                .userType(UserType.EMPLOYEE)
                .email(unique + "@worker.test")
                .phoneNumber("2332" + Math.abs(UUID.randomUUID().hashCode() % 100000000))
                .firstName("Ama")
                .lastName("Owusu")
                .password("worker-pass-1")
                .build()).getUserId();
    }

    private Job approvedJob(String skillId, ProficiencyLevel minimumProficiency) {
        Job job = jobRepository.save(Job.builder()
                .employerProfileId("employer-1")
                .title("Bathroom pipework")
                .description("Replace pipework in a two bedroom flat")
                .jobType(JobType.MULTI_DAY)
                .experienceLevel(ExperienceLevel.SENIOR)
                .status(JobStatus.APPROVED)
                .siteAddress("Labone, Accra")
                .siteLatitude(new BigDecimal("5.5600"))
                .siteLongitude(new BigDecimal("-0.1750"))
                .hourlyRate(new BigDecimal("50.00"))
                .currency("GHS")
                .scheduledStartDate(LocalDate.now().plusDays(2))
                .estimatedDurationHours(24)
                .maxDailyHours(8)
                .extensionsAllowed(2)
                .workersNeeded(2)
                .workersAssigned(0)
                .toolsProvided(false)
                .transportProvided(false)
                .urgent(true)
                .publishedAt(LocalDateTime.now())
                .applicationCount(0)
                .build());

        jobRequirementRepository.save(JobRequirement.builder()
                .jobId(job.getId())
                .skillId(skillId)
                .minimumProficiency(minimumProficiency)
                .minimumYears(2)
                .mandatory(true)
                .weight(BigDecimal.ONE)
                .build());

        return job;
    }
}
