package com.greensnow.worker;

import com.greensnow.worker.model.enums.ApprovalAction;
import com.greensnow.worker.model.enums.JobStatus;
import com.greensnow.worker.model.enums.JobType;
import com.greensnow.worker.model.enums.ProficiencyLevel;
import com.greensnow.worker.model.enums.UserType;
import com.greensnow.worker.model.request.AccountStatusRequest;
import com.greensnow.worker.model.request.EmployeeProfileRequest;
import com.greensnow.worker.model.request.EmployeeSkillRequest;
import com.greensnow.worker.model.request.EmployerProfileRequest;
import com.greensnow.worker.model.request.JobRequest;
import com.greensnow.worker.model.request.JobRequirementRequest;
import com.greensnow.worker.model.request.JobReviewRequest;
import com.greensnow.worker.model.request.RegistrationRequest;
import com.greensnow.worker.model.request.SkillRequest;
import com.greensnow.worker.model.response.JobResponse;
import com.greensnow.worker.model.response.PlatformOverviewResponse;
import com.greensnow.worker.model.response.SkillResponse;
import com.greensnow.worker.model.response.UserResponse;
import com.greensnow.worker.repository.EmployeeSkillRepository;
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
class AdminSmokeTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private EmployerService employerService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeSkillRepository employeeSkillRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Test
    void anApproverWorksTheQueueAndTheJobTheyApproveIsTheOneWorkersCanThenSee() {
        String adminId = registerAdmin("queue");
        String employerId = registerEmployer("queued");
        employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Queued Works").build());

        String plumbing = catalogued(adminId, "Queue Plumbing");
        String jobId = employerService.createJob(employerId, job(plumbing)).getJob().getJobId();

        // a draft is nobody's to decide on yet - it has not been handed to the platform
        assertThat(adminService.approveJob(adminId, jobId, reason("Looks fine")).getResponseCode())
                .isEqualTo("110");
        assertThat(adminService.listApprovalQueue(adminId).getJobs()).isEmpty();

        employerService.submitForApproval(employerId, jobId);

        // and now it is in front of the approver, oldest first and nothing paged away
        assertThat(adminService.listApprovalQueue(adminId).getJobs())
                .extracting("jobId").contains(jobId);

        // the approver reads the same job the company posted, requirements and all
        JobResponse read = adminService.getJob(adminId, jobId);
        assertThat(read.getResponseCode()).isEqualTo("01");
        assertThat(read.getJob().getRequirements()).hasSize(1);

        JobResponse approved = adminService.approveJob(adminId, jobId, reason("Site and rate are clear"));
        assertThat(approved.getResponseCode()).isEqualTo("01");
        assertThat(approved.getJob().getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(approved.getJob().getPublishedAt()).isNotNull();

        // the queue has emptied by exactly the job that was answered
        assertThat(adminService.listApprovalQueue(adminId).getJobs())
                .extracting("jobId").doesNotContain(jobId);

        // a worker can now find it, which is the whole point of the approval step
        String workerId = registerWorker("queue");
        employeeService.createProfile(workerId, EmployeeProfileRequest.builder()
                .headline("Plumber").yearsOfExperience(5).build());
        assertThat(employeeService.getJob(workerId, jobId).getResponseCode()).isEqualTo("01");

        // the company and the approver read one history, not two
        assertThat(employerService.approvalHistory(employerId, jobId).getApprovals())
                .extracting("action").containsExactly(ApprovalAction.APPROVED, ApprovalAction.SUBMITTED);
        assertThat(adminService.listJobApprovals(adminId, jobId).getApprovals())
                .extracting("action").containsExactly(ApprovalAction.APPROVED, ApprovalAction.SUBMITTED);

        // and the approver can be named for what they decided
        assertThat(adminService.listMyReviews(adminId).getApprovals())
                .extracting("jobId").contains(jobId);

        // a job already answered is not answered twice - a second approval landing on a job that
        // is out being worked would reopen it under the worker standing on the site
        assertThat(adminService.approveJob(adminId, jobId, reason("Again")).getResponseCode())
                .isEqualTo("110");
    }

    @Test
    void aRefusalAndAChangeRequestBothCostAReasonAndOnlyOneOfThemEndsTheJob() {
        String adminId = registerAdmin("refusing");
        String employerId = registerEmployer("refused");
        employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Refused Works").build());

        String wiring = catalogued(adminId, "Queue Wiring");
        String turnedDown = employerService.createJob(employerId, job(wiring)).getJob().getJobId();
        employerService.submitForApproval(employerId, turnedDown);

        // "no" with nothing attached is a company guessing at what to change, and a guessed
        // correction comes straight back into the same queue
        assertThat(adminService.rejectJob(adminId, turnedDown, reason(null)).getResponseCode()).isEqualTo("110");

        assertThat(adminService.rejectJob(adminId, turnedDown, reason("The rate is below the legal minimum"))
                .getJob().getStatus()).isEqualTo(JobStatus.REJECTED);

        String sentBack = employerService.createJob(employerId, job(wiring)).getJob().getJobId();
        employerService.submitForApproval(employerId, sentBack);

        assertThat(adminService.requestChanges(adminId, sentBack, reason(null)).getResponseCode()).isEqualTo("110");

        assertThat(adminService.requestChanges(adminId, sentBack, reason("Name the site more precisely"))
                .getJob().getStatus()).isEqualTo(JobStatus.CHANGES_REQUESTED);

        // the difference between the two is what the company can do next: a refusal is the end of
        // that job, a change request hands it back editable and it can come round again
        assertThat(employerService.updateJob(employerId, sentBack, job(wiring)).getResponseCode())
                .isEqualTo("01");
        assertThat(employerService.submitForApproval(employerId, sentBack).getJob().getStatus())
                .isEqualTo(JobStatus.PENDING_APPROVAL);

        // a job that is not there is answered as not there rather than as refused
        assertThat(adminService.getJob(adminId, "not-a-job").getResponseCode()).isEqualTo("104");
        assertThat(adminService.approveJob(adminId, "not-a-job", reason("x")).getResponseCode()).isEqualTo("104");
        assertThat(adminService.listJobApprovals(adminId, "not-a-job").getResponseCode()).isEqualTo("104");
    }

    @Test
    void theCatalogueIsTheVocabularyEverybodyWritesInSoATradeIsNeverDuplicatedAndNeverDeleted() {
        String adminId = registerAdmin("catalogue");
        String name = "Gas Fitting " + UUID.randomUUID().toString().substring(0, 8);

        assertThat(adminService.createSkill(adminId, SkillRequest.builder().category("Plumbing").build())
                .getResponseCode()).isEqualTo("110");
        assertThat(adminService.createSkill(adminId, SkillRequest.builder().name(name).build())
                .getResponseCode()).isEqualTo("110");

        SkillResponse created = adminService.createSkill(adminId, SkillRequest.builder()
                .name(name).category("Plumbing").requiresCertification(true).build());

        assertThat(created.getResponseCode()).isEqualTo("01");
        // a trade is born usable - there is no route that creates a retired one
        assertThat(created.getSkill().getActive()).isTrue();
        assertThat(created.getSkill().getRequiresCertification()).isTrue();
        // and it is addressable by something a person can type, derived when nobody supplied one
        assertThat(created.getSkill().getSlug()).isNotBlank();

        String skillId = created.getSkill().getSkillId();

        // a duplicate name would split one trade in two and a company searching for gas fitters
        // would find half of them
        assertThat(adminService.createSkill(adminId, SkillRequest.builder()
                .name(name.toLowerCase()).category("Plumbing").build()).getResponseCode()).isEqualTo("110");

        // an edit is the whole record, so what is left out is cleared
        SkillResponse updated = adminService.updateSkill(adminId, skillId, SkillRequest.builder()
                .name(name).category("Heating").build());
        assertThat(updated.getResponseCode()).isEqualTo("01");
        assertThat(updated.getSkill().getCategory()).isEqualTo("Heating");
        assertThat(updated.getSkill().getRequiresCertification()).isFalse();
        // but active is not on the request, so correcting a spelling never retires a trade
        assertThat(updated.getSkill().getActive()).isTrue();
        // and a trade is not reported as a duplicate of itself
        assertThat(adminService.updateSkill(adminId, skillId, SkillRequest.builder()
                .name(name).category("Heating").build()).getResponseCode()).isEqualTo("01");

        // a trade cannot be its own parent, and cannot hang off one the catalogue does not hold
        assertThat(adminService.updateSkill(adminId, skillId, SkillRequest.builder()
                .name(name).category("Heating").parentSkillId(skillId).build()).getResponseCode())
                .isEqualTo("110");
        assertThat(adminService.updateSkill(adminId, skillId, SkillRequest.builder()
                .name(name).category("Heating").parentSkillId("not-a-skill").build()).getResponseCode())
                .isEqualTo("110");

        // retiring is not deleting. the plumber who holds it keeps holding it, and the one person
        // who can bring it back can still see it
        assertThat(adminService.retireSkill(adminId, skillId).getSkill().getActive()).isFalse();
        assertThat(adminService.retireSkill(adminId, skillId).getResponseCode()).isEqualTo("110");
        assertThat(adminService.listSkills(adminId, null).getSkills())
                .extracting("skillId").contains(skillId);
        // where every reading that offers a trade to be chosen no longer holds it
        assertThat(skillRepository.findByActiveTrue()).extracting("id").doesNotContain(skillId);

        assertThat(adminService.restoreSkill(adminId, skillId).getSkill().getActive()).isTrue();
        assertThat(adminService.restoreSkill(adminId, skillId).getResponseCode()).isEqualTo("110");

        assertThat(adminService.getSkill(adminId, "not-a-skill").getResponseCode()).isEqualTo("104");
    }

    @Test
    void verifyingIsHeldApartPerThingVouchedForAndWithdrawingOneTakesWhatRestedOnItWithIt() {
        String adminId = registerAdmin("vouching");
        String employerId = registerEmployer("vouched");
        employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Vouched Works").build());

        // skipping the queue means a company's jobs reach workers with nobody having read them,
        // so it is only ever granted to a company that has already been checked out
        assertThat(adminService.setEmployerAutoApproval(adminId, employerId, true).getResponseCode())
                .isEqualTo("110");

        assertThat(adminService.setEmployerVerification(adminId, employerId, true).getResponseCode())
                .isEqualTo("01");
        assertThat(adminService.setEmployerAutoApproval(adminId, employerId, true).getResponseCode())
                .isEqualTo("01");

        UserResponse verified = adminService.getUser(adminId, employerId);
        assertThat(verified.getUser().getVerified()).isTrue();
        assertThat(verified.getUser().getHasProfile()).isTrue();

        // and withdrawing the verification takes the auto approval with it, always - the two were
        // never independent, so an unverified company publishing straight to workers is the exact
        // hole the approval step exists to close
        assertThat(adminService.setEmployerVerification(adminId, employerId, false).getResponseCode())
                .isEqualTo("01");

        String employerJob = employerService.createJob(employerId,
                job(catalogued(adminId, "Vouch Plumbing"))).getJob().getJobId();
        assertThat(employerService.submitForApproval(employerId, employerJob).getJob().getStatus())
                .isEqualTo(JobStatus.PENDING_APPROVAL);

        // a worker's identity and their background check answer two different questions, so they
        // are never collapsed into one flag
        String workerId = registerWorker("vouched");
        String gasFitting = catalogued(adminId, "Vouch Gas");
        employeeService.createProfile(workerId, EmployeeProfileRequest.builder()
                .headline("Gas fitter").yearsOfExperience(9).build());
        employeeService.addSkill(workerId, EmployeeSkillRequest.builder()
                .skillId(gasFitting).proficiencyLevel(ProficiencyLevel.EXPERT).yearsOfExperience(9).build());

        assertThat(adminService.setWorkerIdVerification(adminId, workerId, true).getResponseCode()).isEqualTo("01");
        assertThat(adminService.getUser(adminId, workerId).getUser().getVerified()).isTrue();
        assertThat(adminService.setWorkerBackgroundCheck(adminId, workerId, true).getResponseCode()).isEqualTo("01");
        assertThat(adminService.setWorkerIdVerification(adminId, workerId, false).getResponseCode()).isEqualTo("01");
        assertThat(adminService.getUser(adminId, workerId).getUser().getVerified()).isFalse();

        // a certification is verified one trade at a time. a man who really is a certified gas
        // fitter may have claimed an electrical ticket he does not hold
        String employeeSkillId = employeeSkillRepository.findByEmployeeProfileId(
                        employeeService.getProfile(workerId).getProfileId()).getFirst().getId();
        assertThat(adminService.setWorkerSkillVerification(adminId, employeeSkillId, true).getResponseCode())
                .isEqualTo("01");
        assertThat(adminService.setWorkerSkillVerification(adminId, "not-a-certification", true).getResponseCode())
                .isEqualTo("104");

        // a company or a worker that is not there is answered as not there
        assertThat(adminService.setEmployerVerification(adminId, workerId, true).getResponseCode()).isEqualTo("104");
        assertThat(adminService.setWorkerIdVerification(adminId, employerId, true).getResponseCode())
                .isEqualTo("104");
    }

    @Test
    void suspendingCostsAReasonAndAnApproverCannotLockThemselvesOutOfThePlatform() {
        String adminId = registerAdmin("suspending");
        String workerId = registerWorker("suspended");

        // the heaviest thing on this side: the account cannot sign in afterwards, so a worker
        // stops earning the moment it is done
        assertThat(adminService.suspendUser(adminId, workerId, new AccountStatusRequest()).getResponseCode())
                .isEqualTo("110");

        assertThat(adminService.suspendUser(adminId, workerId,
                AccountStatusRequest.builder().reason("Repeatedly failed to turn up").build())
                .getResponseCode()).isEqualTo("01");
        assertThat(adminService.getUser(adminId, workerId).getUser().getActive()).isFalse();

        assertThat(adminService.suspendUser(adminId, workerId,
                AccountStatusRequest.builder().reason("Again").build()).getResponseCode()).isEqualTo("110");

        assertThat(adminService.restoreUser(adminId, workerId).getResponseCode()).isEqualTo("01");
        assertThat(adminService.restoreUser(adminId, workerId).getResponseCode()).isEqualTo("110");

        // locking the last approver out is a mistake nobody can undo from inside the platform
        assertThat(adminService.suspendUser(adminId, adminId,
                AccountStatusRequest.builder().reason("Oops").build()).getResponseCode()).isEqualTo("110");

        assertThat(adminService.suspendUser(adminId, "not-an-account",
                AccountStatusRequest.builder().reason("x").build()).getResponseCode()).isEqualTo("104");
        assertThat(adminService.getUser(adminId, "not-an-account").getResponseCode()).isEqualTo("104");

        // the listing answers in pages and does not look a profile up per row, so the two columns
        // it could not answer are left unanswered rather than answered falsely
        assertThat(adminService.listUsers(adminId, UserType.EMPLOYEE, 1, 5).getUsers()).hasSizeLessThanOrEqualTo(5);
        assertThat(adminService.listUsers(adminId, UserType.EMPLOYEE, 1, 5).getUsers().getFirst().getHasProfile())
                .isNull();

        // an admin was never asked for a profile, so it is not reported as missing one
        assertThat(adminService.getUser(adminId, adminId).getUser().getHasProfile()).isNull();
    }

    @Test
    void theOverviewIsCountedOnReadBecauseAStoredTallyThatDriftsIsWorseThanNoTally() {
        String adminId = registerAdmin("overview");

        PlatformOverviewResponse before = adminService.getOverview(adminId);
        assertThat(before.getResponseCode()).isEqualTo("01");

        String employerId = registerEmployer("counted");
        employerService.createProfile(employerId, EmployerProfileRequest.builder()
                .companyName("Counted Works").build());
        String jobId = employerService.createJob(employerId,
                job(catalogued(adminId, "Count Plumbing"))).getJob().getJobId();
        employerService.submitForApproval(employerId, jobId);

        PlatformOverviewResponse after = adminService.getOverview(adminId);
        assertThat(after.getEmployers()).isEqualTo(before.getEmployers() + 1);
        // the queue depth in particular is the number the approver decides their day on
        assertThat(after.getJobsAwaitingApproval()).isEqualTo(before.getJobsAwaitingApproval() + 1);

        adminService.approveJob(adminId, jobId, reason("Fine"));

        PlatformOverviewResponse settled = adminService.getOverview(adminId);
        assertThat(settled.getJobsAwaitingApproval()).isEqualTo(before.getJobsAwaitingApproval());
        assertThat(settled.getJobsApproved()).isEqualTo(after.getJobsApproved() + 1);
    }

    private JobReviewRequest reason(String reason) {
        return JobReviewRequest.builder().reason(reason).comments("Reviewed").build();
    }

    // every trade these tests use is created through the admin route rather than written straight
    // to the table, because adding to the catalogue is itself one of the things being proven
    private String catalogued(String adminId, String name) {
        return adminService.createSkill(adminId, SkillRequest.builder()
                .name(name + "-" + UUID.randomUUID().toString().substring(0, 8))
                .category("Construction")
                .build()).getSkill().getSkillId();
    }

    private JobRequest job(String skillId) {
        return JobRequest.builder()
                .title("Bathroom pipework")
                .description("Replace pipework in a two bedroom flat")
                .jobType(JobType.MULTI_DAY)
                .siteAddress("Labone, Accra")
                .siteLatitude(new BigDecimal("5.5600"))
                .siteLongitude(new BigDecimal("-0.1750"))
                .hourlyRate(new BigDecimal("50.00"))
                .scheduledStartDate(LocalDate.now().plusDays(2))
                .workersNeeded(1)
                .requirements(List.of(JobRequirementRequest.builder()
                        .skillId(skillId)
                        .minimumProficiency(ProficiencyLevel.INTERMEDIATE)
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
                .firstName("Kofi")
                .lastName("Asante")
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
