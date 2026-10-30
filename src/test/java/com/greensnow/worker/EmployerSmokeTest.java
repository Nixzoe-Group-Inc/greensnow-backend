package com.greensnow.worker;

import com.greensnow.worker.model.entity.EmployerProfile;
import com.greensnow.worker.model.entity.Skill;
import com.greensnow.worker.model.enums.ApplicationStatus;
import com.greensnow.worker.model.enums.ApprovalAction;
import com.greensnow.worker.model.enums.CompanySize;
import com.greensnow.worker.model.enums.ExperienceLevel;
import com.greensnow.worker.model.enums.JobStatus;
import com.greensnow.worker.model.enums.JobType;
import com.greensnow.worker.model.enums.ProficiencyLevel;
import com.greensnow.worker.model.enums.UserType;
import com.greensnow.worker.model.request.ApplicationDecisionRequest;
import com.greensnow.worker.model.request.EmployeeProfileRequest;
import com.greensnow.worker.model.request.EmployeeSkillRequest;
import com.greensnow.worker.model.request.EmployerProfileRequest;
import com.greensnow.worker.model.request.JobApplicationRequest;
import com.greensnow.worker.model.request.JobRequest;
import com.greensnow.worker.model.request.JobRequirementRequest;
import com.greensnow.worker.model.request.JobReviewRequest;
import com.greensnow.worker.model.request.RegistrationRequest;
import com.greensnow.worker.model.response.ApplicantListResponse;
import com.greensnow.worker.model.response.ApplicantResponse;
import com.greensnow.worker.model.response.EmployerProfileResponse;
import com.greensnow.worker.model.response.JobApprovalListResponse;
import com.greensnow.worker.model.response.JobResponse;
import com.greensnow.worker.repository.EmployerProfileRepository;
import com.greensnow.worker.repository.SkillRepository;
import com.greensnow.worker.service.admin.AdminService;
import com.greensnow.worker.service.auth.AuthService;
import com.greensnow.worker.service.employee.EmployeeService;
import com.greensnow.worker.service.employer.EmployerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmployerSmokeTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmployerService employerService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployerProfileRepository employerProfileRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private SkillRepository skillRepository;

    @Test
    void aCompanyBuildsAProfilePostsAJobSendsItForApprovalAndHiresTheWorkerWhoApplied() {
        String employerId = registerEmployer("sunrise");

        // nothing exists until the company creates it, and reading before that says so rather
        // than inventing an empty profile
        assertThat(employerService.getProfile(employerId).getResponseCode()).isEqualTo("104");

        EmployerProfileResponse created = employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Sunrise Builders")
                .registrationNumber("CS-100-2024")
                .companySize(CompanySize.SMALL)
                .industry("Construction")
                .address("Spintex Road, Accra")
                .city("Accra")
                .country("Ghana")
                .latitude(new BigDecimal("5.6200"))
                .longitude(new BigDecimal("-0.1200"))
                .build());

        assertThat(created.getResponseCode()).isEqualTo("01");
        assertThat(created.getCompanyName()).isEqualTo("Sunrise Builders");
        // the badge and the switch that lets a company's jobs skip the queue are the platform's,
        // so a company cannot arrive already trusted
        assertThat(created.isVerified()).isFalse();
        assertThat(created.isAutoApproveJobs()).isFalse();

        // a second profile for the same company is refused rather than shadowing the first
        assertThat(employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Sunrise Again").build()).getResponseCode()).isEqualTo("109");

        Skill plumbing = catalogued("Plumbing");

        JobResponse posted = employerService.createJob(employerId, job(plumbing.getId()));

        assertThat(posted.getResponseCode()).isEqualTo("01");
        // a job is posted as a draft and nothing on the request can say otherwise - an employer
        // who could send APPROVED would approve their own job
        assertThat(posted.getJob().getStatus()).isEqualTo(JobStatus.DRAFT);
        assertThat(posted.getJob().getRequirements()).hasSize(1);
        assertThat(posted.getJob().getVacancies()).isEqualTo(2);

        String jobId = posted.getJob().getJobId();

        // a draft is not in front of anybody yet, so the worker side answers for it exactly as
        // it answers for a job that does not exist
        String workerId = registerWorker("kwame");
        employeeService.createProfile(workerId, EmployeeProfileRequest.builder()
                .headline("Plumber, 7 years")
                .yearsOfExperience(7)
                .baseLatitude(new BigDecimal("5.6100"))
                .baseLongitude(new BigDecimal("-0.1300"))
                .build());
        employeeService.addSkill(workerId, EmployeeSkillRequest.builder()
                .skillId(plumbing.getId()).proficiencyLevel(ProficiencyLevel.ADVANCED).yearsOfExperience(7).build());

        assertThat(employeeService.getJob(workerId, jobId).getResponseCode()).isEqualTo("104");

        JobResponse submitted = employerService.submitForApproval(employerId, jobId);
        assertThat(submitted.getResponseCode()).isEqualTo("01");
        // an untrusted company's job waits for an approver rather than going straight out
        assertThat(submitted.getJob().getStatus()).isEqualTo(JobStatus.PENDING_APPROVAL);

        JobApprovalListResponse history = employerService.approvalHistory(employerId, jobId);
        assertThat(history.getResponseCode()).isEqualTo("01");
        assertThat(history.getApprovals()).extracting("action").containsExactly(ApprovalAction.SUBMITTED);

        // once it is in the queue it is no longer the employer's to rewrite or to delete
        assertThat(employerService.updateJob(employerId, jobId, job(plumbing.getId())).getResponseCode())
                .isEqualTo("110");
        assertThat(employerService.deleteJob(employerId, jobId).getResponseCode()).isEqualTo("110");

        // the approver's half of the flow is the admin side, and it is what opens the job
        approve(jobId);

        assertThat(employeeService.applyForJob(workerId, jobId,
                JobApplicationRequest.builder().coverNote("Free from Monday").build())
                .getResponseCode()).isEqualTo("01");

        ApplicantListResponse applicants = employerService.listApplicants(employerId, jobId, null);
        assertThat(applicants.getResponseCode()).isEqualTo("01");
        assertThat(applicants.getTotalApplicants()).isEqualTo(1);
        assertThat(applicants.getWorkersNeeded()).isEqualTo(2);
        assertThat(applicants.getApplicants().getFirst().getWorkerName()).isEqualTo("Ama Owusu");
        // the score was copied onto the row on the day the worker applied
        assertThat(applicants.getApplicants().getFirst().getMatchScore()).isNotNull();

        String applicationId = applicants.getApplicants().getFirst().getApplicationId();

        // a shortlist is not a hire, so the worker's own contact details are still withheld
        ApplicantResponse shortlisted = employerService.shortlistApplicant(employerId, jobId, applicationId);
        assertThat(shortlisted.getResponseCode()).isEqualTo("01");
        assertThat(shortlisted.getApplicant().getStatus()).isEqualTo(ApplicationStatus.SHORTLISTED);
        assertThat(shortlisted.getApplicant().getEmail()).isNull();
        assertThat(shortlisted.getApplicant().getPhoneNumber()).isNull();

        // shortlisting is a step forward from a fresh application and nothing else
        assertThat(employerService.shortlistApplicant(employerId, jobId, applicationId).getResponseCode())
                .isEqualTo("110");

        ApplicantResponse accepted = employerService.acceptApplicant(employerId, jobId, applicationId);
        assertThat(accepted.getResponseCode()).isEqualTo("01");
        assertThat(accepted.getApplicant().getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        // and only now are they released
        assertThat(accepted.getApplicant().getEmail()).isNotNull();
        assertThat(accepted.getApplicant().getPhoneNumber()).isNotNull();

        // the job still wants a second worker, so it has not moved to ASSIGNED
        assertThat(employerService.getJob(employerId, jobId).getJob().getWorkersAssigned()).isEqualTo(1);
        assertThat(employerService.getJob(employerId, jobId).getJob().getStatus()).isEqualTo(JobStatus.APPROVED);

        // an answer already given is not given again
        assertThat(employerService.rejectApplicant(employerId, jobId, applicationId,
                ApplicationDecisionRequest.builder().reason("Changed our mind").build())
                .getResponseCode()).isEqualTo("110");

        assertThat(employerService.closeJob(employerId, jobId).getJob().getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(employerService.closeJob(employerId, jobId).getResponseCode()).isEqualTo("110");
    }

    @Test
    void aTrustedCompanysJobSkipsTheQueueAndNoOneElsesJobIsVisibleToIt() {
        String employerId = registerEmployer("trusted");
        employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Trusted Works").build());

        // the switch is the platform's to set, which is why it is turned on here rather than
        // sent on the request
        EmployerProfile profile = employerProfileRepository.findByUserId(employerId).orElseThrow();
        profile.setAutoApproveJobs(true);
        employerProfileRepository.save(profile);

        Skill wiring = catalogued("Electrical wiring");
        String jobId = employerService.createJob(employerId, job(wiring.getId())).getJob().getJobId();

        JobResponse submitted = employerService.submitForApproval(employerId, jobId);
        assertThat(submitted.getResponseCode()).isEqualTo("01");
        assertThat(submitted.getJob().getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(submitted.getJob().getPublishedAt()).isNotNull();

        assertThat(employerService.approvalHistory(employerId, jobId).getApprovals())
                .extracting("action").containsExactly(ApprovalAction.AUTO_APPROVED);

        // another company asking after it is answered as though it were not there - ownership is
        // phrased as not found, never as refused, so nothing confirms the job exists
        String otherId = registerEmployer("other");
        employerService.createProfile(otherId, EmployerProfileRequest.builder().companyName("Other Co").build());

        assertThat(employerService.getJob(otherId, jobId).getResponseCode()).isEqualTo("104");
        assertThat(employerService.updateJob(otherId, jobId, job(wiring.getId())).getResponseCode()).isEqualTo("104");
        assertThat(employerService.deleteJob(otherId, jobId).getResponseCode()).isEqualTo("104");
        assertThat(employerService.listApplicants(otherId, jobId, null).getResponseCode()).isEqualTo("104");
        assertThat(employerService.approvalHistory(otherId, jobId).getResponseCode()).isEqualTo("104");
        assertThat(employerService.listJobs(otherId, null, null, null).getJobs()).isEmpty();
    }

    @Test
    void aHalfWrittenDraftIsKeptButNeverGoesInFrontOfWorkers() {
        String employerId = registerEmployer("halfway");
        employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Halfway Contracts").build());

        // a draft may be saved with only what a job cannot be read without
        JobResponse draft = employerService.createJob(employerId, JobRequest.builder()
                .title("Roof repair")
                .description("Re-sheet a leaking roof")
                .jobType(JobType.ONE_OFF)
                .build());

        assertThat(draft.getResponseCode()).isEqualTo("01");
        String jobId = draft.getJob().getJobId();

        // but it cannot be submitted: a worker will not travel to a site they cannot locate
        assertThat(employerService.submitForApproval(employerId, jobId).getResponseCode()).isEqualTo("110");

        Skill roofing = catalogued("Roofing");
        assertThat(employerService.updateJob(employerId, jobId, JobRequest.builder()
                .title("Roof repair")
                .description("Re-sheet a leaking roof")
                .jobType(JobType.ONE_OFF)
                .siteAddress("Adenta, Accra")
                .hourlyRate(new BigDecimal("40.00"))
                .workersNeeded(1)
                .requirements(List.of(JobRequirementRequest.builder()
                        .skillId(roofing.getId())
                        .minimumProficiency(ProficiencyLevel.INTERMEDIATE)
                        .mandatory(true)
                        .build()))
                .build()).getResponseCode()).isEqualTo("01");

        assertThat(employerService.submitForApproval(employerId, jobId).getJob().getStatus())
                .isEqualTo(JobStatus.PENDING_APPROVAL);
    }

    @Test
    void aJobTheCompanyCouldNotDeliverOnIsRefusedBeforeItIsEverSaved() {
        String employerId = registerEmployer("refused");

        // a company that does not exist has nothing to post a job against
        assertThat(employerService.createJob(employerId, job("whatever")).getResponseCode()).isEqualTo("104");

        // and a company with no name is refused on the way in, because every job on the platform
        // is posted under that name
        assertThat(employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .city("Accra").build()).getResponseCode()).isEqualTo("110");

        // half a coordinate locates nothing
        assertThat(employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Refused Ltd").latitude(new BigDecimal("5.6")).build())
                .getResponseCode()).isEqualTo("110");

        employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Refused Ltd").build());

        Skill plumbing = catalogued("Plumbing");

        assertThat(employerService.createJob(employerId, JobRequest.builder()
                .description("No title").jobType(JobType.ONE_OFF).build()).getResponseCode()).isEqualTo("110");

        assertThat(employerService.createJob(employerId, JobRequest.builder()
                .title("Nothing to pay").description("A job").jobType(JobType.ONE_OFF)
                .hourlyRate(BigDecimal.ZERO).build()).getResponseCode()).isEqualTo("110");

        assertThat(employerService.createJob(employerId, JobRequest.builder()
                .title("Too many hands").description("A job").jobType(JobType.ONE_OFF)
                .workersNeeded(500).build()).getResponseCode()).isEqualTo("110");

        assertThat(employerService.createJob(employerId, JobRequest.builder()
                .title("Backwards").description("A job").jobType(JobType.ONE_OFF)
                .scheduledStartDate(LocalDate.now().plusDays(5))
                .scheduledEndDate(LocalDate.now().plusDays(2))
                .build()).getResponseCode()).isEqualTo("110");

        // the same trade listed twice is weighted twice, which quietly shifts the score
        assertThat(employerService.createJob(employerId, JobRequest.builder()
                .title("Twice over").description("A job").jobType(JobType.ONE_OFF)
                .requirements(List.of(
                        JobRequirementRequest.builder().skillId(plumbing.getId())
                                .minimumProficiency(ProficiencyLevel.BEGINNER).build(),
                        JobRequirementRequest.builder().skillId(plumbing.getId())
                                .minimumProficiency(ProficiencyLevel.EXPERT).build()))
                .build()).getResponseCode()).isEqualTo("110");

        // a trade the catalogue does not hold is refused too
        assertThat(employerService.createJob(employerId, JobRequest.builder()
                .title("Unknown trade").description("A job").jobType(JobType.ONE_OFF)
                .requirements(List.of(JobRequirementRequest.builder().skillId("not-a-skill")
                        .minimumProficiency(ProficiencyLevel.BEGINNER).build()))
                .build()).getResponseCode()).isEqualTo("110");

        // and a refusal with no reason tells the worker nothing they can act on next time
        assertThat(employerService.rejectApplicant(employerId, "no-job", "no-application",
                ApplicationDecisionRequest.builder().build()).getResponseCode()).isEqualTo("110");
    }

    @Test
    void anUpdateIsTheWholeProfileSoWhatIsLeftOutIsCleared() {
        String employerId = registerEmployer("whole");

        employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Whole Record Ltd")
                .industry("Construction")
                .city("Accra")
                .website("https://whole.test")
                .build());

        EmployerProfileResponse updated = employerService.updateProfile(employerId,
                EmployerProfileRequest.builder().companyName("Whole Record Ltd").city("Kumasi").build());

        assertThat(updated.getResponseCode()).isEqualTo("01");
        assertThat(updated.getCity()).isEqualTo("Kumasi");
        // what the request carries is what the company reads as afterwards
        assertThat(updated.getIndustry()).isNull();
        assertThat(updated.getWebsite()).isNull();

        // but the platform's own fields are not on the request and so are never touched
        assertThat(updated.isVerified()).isFalse();
        assertThat(updated.getJobsPosted()).isNotNull();

        // an edit without a name would clear the name every job is posted under, so it is refused
        assertThat(employerService.updateProfile(employerId, EmployerProfileRequest.builder().city("Tema").build())
                .getResponseCode()).isEqualTo("110");
    }

    // the approval is made through the admin route by an admin account registered for the
    // purpose - deliberately not through any employer method and no longer against the record
    // directly, because approving its own job is the one thing a company must never be able to
    // do, and a test that wrote APPROVED itself would still pass if that gate ever came off
    private void approve(String jobId) {
        String adminId = registerAdmin("approver");
        assertThat(adminService.approveJob(adminId, jobId,
                JobReviewRequest.builder().reason("Site and rate check out").build())
                .getResponseCode()).isEqualTo("01");
    }

    private Skill catalogued(String name) {
        return skillRepository.save(Skill.builder()
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID())
                .category("Construction")
                .requiresCertification(false)
                .active(true)
                .build());
    }

    private JobRequest job(String skillId) {
        return JobRequest.builder()
                .title("Bathroom pipework")
                .description("Replace pipework in a two bedroom flat")
                .jobType(JobType.MULTI_DAY)
                .experienceLevel(ExperienceLevel.MID)
                .siteAddress("Labone, Accra")
                .siteLatitude(new BigDecimal("5.5600"))
                .siteLongitude(new BigDecimal("-0.1750"))
                .hourlyRate(new BigDecimal("50.00"))
                .scheduledStartDate(LocalDate.now().plusDays(2))
                .estimatedDurationHours(24)
                .maxDailyHours(8)
                .extensionsAllowed(2)
                .workersNeeded(2)
                .urgent(true)
                .requirements(List.of(JobRequirementRequest.builder()
                        .skillId(skillId)
                        .minimumProficiency(ProficiencyLevel.INTERMEDIATE)
                        .minimumYears(2)
                        .mandatory(true)
                        .build()))
                .build();
    }

    private String registerAdmin(String handle) {
        String unique = handle + "-" + UUID.randomUUID().toString().substring(0, 8);
        return authService.registerUser(RegistrationRequest.builder()
                .userType(UserType.ADMIN)
                .email(unique + "@admin.test")
                .phoneNumber("2332" + Math.abs(UUID.randomUUID().hashCode() % 100000000))
                .firstName("Yaw")
                .lastName("Asare")
                .password("admin-pass-1")
                .build()).getUserId();
    }

    private String registerEmployer(String handle) {
        String unique = handle + "-" + UUID.randomUUID().toString().substring(0, 8);
        return authService.registerUser(RegistrationRequest.builder()
                .userType(UserType.EMPLOYER)
                .email(unique + "@company.test")
                .phoneNumber("2332" + Math.abs(UUID.randomUUID().hashCode() % 100000000))
                .firstName("Adwoa")
                .lastName("Mensah")
                .password("company-pass-1")
                .build()).getUserId();
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
}
