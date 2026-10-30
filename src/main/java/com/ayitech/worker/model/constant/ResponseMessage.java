package com.ayitech.worker.model.constant;

public class ResponseMessage {
    public static final String REGISTRATION_SUCCESSFUL = "Registration successful";
    public static final String REGISTRATION_FAILED = "We could not complete your registration. Please try again later";
    public static final String EMAIL_ALREADY_REGISTERED = "An account already exists for %s";
    public static final String PHONE_NUMBER_ALREADY_REGISTERED = "An account already exists for %s";
    public static final String NO_EMAIL_FOUND = "An email address is required to register";
    public static final String NO_PASSWORD_FOUND = "A password is required to register";
    public static final String NO_FIRST_NAME_FOUND = "A first name is required to register";
    public static final String NO_LAST_NAME_FOUND = "A last name is required to register";
    public static final String NO_USER_TYPE_FOUND = "A user type is required to register";

    public static final String LOGIN_SUCCESSFUL = "Login successful";
    public static final String LOGIN_FAILED = "We could not sign you in. Please try again later";
    // one message for an unknown email and for a wrong password, so the api does not disclose which emails exist
    public static final String INVALID_CREDENTIALS = "The email or password is incorrect";
    public static final String ACCOUNT_DEACTIVATED = "This account has been deactivated";

    public static final String PASSWORD_CHANGE_SUCCESSFUL = "Your password has been changed";
    public static final String PASSWORD_CHANGE_FAILED = "We could not change your password. Please try again later";
    public static final String INVALID_CURRENT_PASSWORD = "The current password is incorrect";
    public static final String NO_CURRENT_PASSWORD_FOUND = "Your current password is required";
    public static final String NO_NEW_PASSWORD_FOUND = "A new password is required";
    public static final String PASSWORD_TOO_SHORT = "A password must be at least %s characters long";
    public static final String PASSWORD_UNCHANGED = "The new password must be different from the current one";
    public static final String ACCOUNT_NOT_FOUND = "We could not find that account";

    // the same answer whether or not the email is held, so the api cannot be used to discover
    // which emails have accounts - the code itself only ever reaches a real account
    public static final String PASSWORD_RESET_REQUESTED = "If that email has an account, a reset code has been sent to it";
    public static final String PASSWORD_RESET_REQUEST_FAILED = "We could not start a password reset. Please try again later";

    public static final String PASSWORD_RESET_SUCCESSFUL = "Your password has been reset. Please sign in with your new password";
    public static final String PASSWORD_RESET_FAILED = "We could not reset your password. Please try again later";
    public static final String NO_RESET_CODE_FOUND = "A reset code is required";
    // one message for an unknown email, a wrong code, a used code and an expired one, for the
    // same reason INVALID_CREDENTIALS is one message
    public static final String INVALID_RESET_CODE = "The reset code is invalid or has expired";
    public static final String TOO_MANY_RESET_ATTEMPTS = "Too many incorrect attempts. Please request a new reset code";

    public static final String LOGOUT_SUCCESSFUL = "Logout successful";
    public static final String LOGOUT_FAILED = "We could not sign you out. Please try again later";
    public static final String NO_ACCESS_TOKEN_FOUND = "An access token is required to access this resource";
    public static final String INVALID_ACCESS_TOKEN = "The access token is invalid or has expired";

    public static final String PROFILE_CREATED = "Your profile has been created";
    public static final String PROFILE_CREATION_FAILED = "We could not create your profile. Please try again later";
    public static final String PROFILE_UPDATED = "Your profile has been updated";
    public static final String PROFILE_UPDATE_FAILED = "We could not update your profile. Please try again later";
    public static final String PROFILE_RETRIEVED = "Profile retrieved";
    public static final String PROFILE_RETRIEVAL_FAILED = "We could not load your profile. Please try again later";
    public static final String PROFILE_ALREADY_EXISTS = "You already have a profile. Update it instead";
    // every employee route but the create reads this, because a worker with no profile has no
    // skills to match on and no rate to be hired at
    public static final String NO_PROFILE_FOUND = "Create your profile before using this service";
    public static final String NO_YEARS_OF_EXPERIENCE_FOUND = "Your years of experience are required";
    public static final String INVALID_YEARS_OF_EXPERIENCE = "Years of experience must be between 0 and 70";
    public static final String NO_RATE_FOUND = "An hourly rate or a daily rate is required so employers know what you charge";
    public static final String INVALID_RATE = "A rate must be greater than zero";
    public static final String INVALID_SERVICE_RADIUS = "A service radius must be between 1 and 500 kilometres";
    public static final String INCOMPLETE_BASE_LOCATION = "A base location needs both a latitude and a longitude";
    public static final String INVALID_COORDINATES = "A latitude must be between -90 and 90 and a longitude between -180 and 180";

    public static final String AVAILABILITY_UPDATED = "Your availability has been updated";
    public static final String AVAILABILITY_UPDATE_FAILED = "We could not update your availability. Please try again later";
    public static final String NO_AVAILABILITY_STATUS_FOUND = "An availability status is required";
    // a worker suspends nobody, least of all themselves - that is the platform's to set
    public static final String AVAILABILITY_NOT_SELF_ASSIGNABLE = "That availability status can only be set by the platform";

    public static final String SKILL_ADDED = "The skill has been added to your profile";
    public static final String SKILL_ADD_FAILED = "We could not add that skill. Please try again later";
    public static final String SKILL_UPDATED = "The skill has been updated";
    public static final String SKILL_UPDATE_FAILED = "We could not update that skill. Please try again later";
    public static final String SKILL_REMOVED = "The skill has been removed from your profile";
    public static final String SKILL_REMOVE_FAILED = "We could not remove that skill. Please try again later";
    public static final String SKILLS_RETRIEVED = "Skills retrieved";
    public static final String SKILLS_RETRIEVAL_FAILED = "We could not load your skills. Please try again later";
    public static final String NO_SKILL_FOUND = "A skill is required";
    public static final String SKILL_NOT_RECOGNISED = "We do not recognise that skill";
    public static final String SKILL_NOT_ON_PROFILE = "That skill is not on your profile";
    public static final String SKILL_ALREADY_ON_PROFILE = "That skill is already on your profile. Update it instead";
    public static final String NO_PROFICIENCY_LEVEL_FOUND = "A proficiency level is required";
    // a trade that is licensed is licensed - claiming it without the certificate is the one
    // thing an employer cannot check for themselves on site
    public static final String CERTIFICATION_REQUIRED = "%s requires a certification. Add the certification name and a link to it";
    public static final String VERIFIED_SKILL_NOT_EDITABLE = "A verified skill cannot be changed. Contact support";

    public static final String JOBS_RETRIEVED = "Jobs retrieved";
    public static final String JOBS_RETRIEVAL_FAILED = "We could not load jobs. Please try again later";
    public static final String JOB_RETRIEVED = "Job retrieved";
    public static final String JOB_RETRIEVAL_FAILED = "We could not load that job. Please try again later";
    public static final String NO_JOB_FOUND = "We could not find that job";
    // an unapproved job is answered as though it did not exist, so browsing cannot be used to
    // read jobs an employer has not had approved yet
    public static final String JOB_NOT_OPEN = "That job is no longer open";
    public static final String JOB_FULLY_STAFFED = "That job already has all the workers it needs";

    public static final String APPLICATION_SUBMITTED = "Your application has been submitted";
    public static final String APPLICATION_FAILED = "We could not submit your application. Please try again later";
    public static final String APPLICATIONS_RETRIEVED = "Applications retrieved";
    public static final String APPLICATIONS_RETRIEVAL_FAILED = "We could not load your applications. Please try again later";
    public static final String ALREADY_APPLIED = "You have already applied for that job";
    public static final String NO_APPLICATION_FOUND = "We could not find that application";
    public static final String APPLICATION_WITHDRAWN = "Your application has been withdrawn";
    public static final String APPLICATION_WITHDRAWAL_FAILED = "We could not withdraw your application. Please try again later";
    public static final String APPLICATION_NOT_WITHDRAWABLE = "An application that has already been answered cannot be withdrawn";
    public static final String NO_SKILLS_ON_PROFILE = "Add at least one skill before applying for work";
    public static final String MANDATORY_SKILL_MISSING = "You do not hold every skill this job requires";
    public static final String MATCH_SCORE_TOO_LOW = "Your profile scores %s for this job and %s is the minimum";
    public static final String INVALID_PROPOSED_RATE = "A proposed rate must be greater than zero";

    public static final String NO_COMPANY_NAME_FOUND = "A company name is required";
    // every employer route but the create reads this. a job has to be posted by somebody, and
    // a worker deciding whether to travel to a site is deciding partly on who is behind it
    public static final String NO_EMPLOYER_PROFILE_FOUND = "Create your company profile before posting work";
    public static final String INCOMPLETE_COMPANY_LOCATION = "A company location needs both a latitude and a longitude";

    public static final String JOB_CREATED = "Your job has been created as a draft. Submit it for approval when it is ready";
    public static final String JOB_CREATION_FAILED = "We could not create that job. Please try again later";
    public static final String JOB_UPDATED = "The job has been updated";
    public static final String JOB_UPDATE_FAILED = "We could not update that job. Please try again later";
    public static final String JOB_DELETED = "The job has been deleted";
    public static final String JOB_DELETE_FAILED = "We could not delete that job. Please try again later";
    // the whole point of the approval step: once a job is in front of workers, the terms they
    // read are the terms they applied on and cannot be rewritten underneath them
    public static final String JOB_NOT_EDITABLE = "A job can only be changed while it is a draft or has had changes requested. This one is %s";
    public static final String JOB_NOT_DELETABLE = "Only a draft job can be deleted. Close it instead";

    public static final String NO_JOB_TITLE_FOUND = "A job title is required";
    public static final String NO_JOB_DESCRIPTION_FOUND = "A description is required so workers know what the work involves";
    public static final String NO_JOB_TYPE_FOUND = "A job type is required";
    public static final String NO_SITE_ADDRESS_FOUND = "A site address is required so workers know where the work is";
    public static final String INVALID_SITE_COORDINATES = "A site latitude must be between -90 and 90 and a longitude between -180 and 180";
    public static final String INCOMPLETE_SITE_LOCATION = "A site location needs both a latitude and a longitude";
    public static final String NO_JOB_RATE_FOUND = "An hourly rate, a daily rate or a total budget is required so workers know what the job pays";
    public static final String INVALID_JOB_RATE = "A rate must be greater than zero";
    public static final String INVALID_WORKERS_NEEDED = "A job needs between 1 and %s workers";
    public static final String INVALID_SCHEDULE = "A job cannot end before it starts";
    public static final String INVALID_DURATION = "An estimated duration must be greater than zero";
    public static final String INVALID_DAILY_HOURS = "A working day must be between 1 and 24 hours";
    public static final String INVALID_EXTENSIONS_ALLOWED = "Extensions allowed cannot be negative";
    public static final String NO_REQUIREMENTS_FOUND = "A job needs at least one skill requirement before it can be submitted";
    public static final String REQUIREMENT_SKILL_NOT_RECOGNISED = "We do not recognise the skill %s";
    public static final String DUPLICATE_REQUIREMENT = "The skill %s is listed more than once on this job";

    public static final String JOB_SUBMITTED = "Your job has been submitted for approval";
    public static final String JOB_AUTO_APPROVED = "Your job has been approved and is now open to workers";
    public static final String JOB_SUBMISSION_FAILED = "We could not submit that job. Please try again later";
    public static final String JOB_NOT_SUBMITTABLE = "Only a draft job or one with changes requested can be submitted. This one is %s";

    public static final String JOB_CLOSED = "The job has been closed";
    public static final String JOB_CLOSE_FAILED = "We could not close that job. Please try again later";
    public static final String JOB_ALREADY_CLOSED = "That job is already closed";

    public static final String APPROVALS_RETRIEVED = "Approval history retrieved";
    public static final String APPROVALS_RETRIEVAL_FAILED = "We could not load the approval history. Please try again later";

    public static final String APPLICANTS_RETRIEVED = "Applicants retrieved";
    public static final String APPLICANTS_RETRIEVAL_FAILED = "We could not load the applicants. Please try again later";
    public static final String APPLICANT_RETRIEVED = "Applicant retrieved";
    public static final String APPLICANT_RETRIEVAL_FAILED = "We could not load that applicant. Please try again later";

    public static final String APPLICANT_SHORTLISTED = "The applicant has been shortlisted";
    public static final String APPLICANT_SHORTLIST_FAILED = "We could not shortlist that applicant. Please try again later";
    public static final String APPLICANT_ACCEPTED = "The applicant has been accepted and can now see your contact details";
    public static final String APPLICANT_ACCEPT_FAILED = "We could not accept that applicant. Please try again later";
    public static final String APPLICANT_REJECTED = "The applicant has been turned down";
    public static final String APPLICANT_REJECT_FAILED = "We could not turn down that applicant. Please try again later";
    // a worker who withdrew is gone, and one already answered has been told where they stand -
    // moving either would change an outcome they have already been given
    public static final String APPLICATION_ALREADY_ANSWERED = "That application has already been answered";
    public static final String APPLICATION_WAS_WITHDRAWN = "That worker has withdrawn their application";
    public static final String APPLICANT_NOT_SHORTLISTABLE = "Only a new application can be shortlisted. This one is %s";
    public static final String JOB_NOT_HIRING = "Workers can only be accepted onto an approved job. This one is %s";
    public static final String NO_REJECTION_REASON_FOUND = "A reason is required so the worker knows where they stand";

    public static final String APPROVAL_QUEUE_RETRIEVED = "Jobs awaiting approval retrieved";
    public static final String APPROVAL_QUEUE_RETRIEVAL_FAILED = "We could not load the approval queue. Please try again later";
    public static final String JOB_APPROVED = "The job has been approved and is now open to workers";
    public static final String JOB_APPROVAL_FAILED = "We could not approve that job. Please try again later";
    public static final String JOB_REJECTED = "The job has been turned down";
    public static final String JOB_REJECTION_FAILED = "We could not turn down that job. Please try again later";
    public static final String JOB_CHANGES_REQUESTED = "The job has been sent back to the company with your changes";
    public static final String JOB_CHANGES_REQUEST_FAILED = "We could not send that job back. Please try again later";
    // the whole queue exists to answer jobs sitting in it. anything else has either been answered
    // already or has never been submitted, and answering it twice would move a job the employer
    // has already been told about
    public static final String JOB_NOT_AWAITING_APPROVAL = "Only a job waiting for approval can be reviewed. This one is %s";
    public static final String NO_REVIEW_REASON_FOUND = "A reason is required so the company knows what to fix";
    public static final String REVIEWS_RETRIEVED = "Reviews retrieved";
    public static final String REVIEWS_RETRIEVAL_FAILED = "We could not load your reviews. Please try again later";

    public static final String SKILL_CREATED = "The skill has been added to the catalogue";
    public static final String SKILL_CREATION_FAILED = "We could not add that skill. Please try again later";
    public static final String CATALOGUE_SKILL_UPDATED = "The skill has been updated";
    public static final String CATALOGUE_SKILL_UPDATE_FAILED = "We could not update that skill. Please try again later";
    public static final String CATALOGUE_SKILLS_RETRIEVED = "Catalogue retrieved";
    public static final String CATALOGUE_SKILLS_RETRIEVAL_FAILED = "We could not load the catalogue. Please try again later";
    public static final String CATALOGUE_SKILL_RETRIEVED = "Skill retrieved";
    public static final String CATALOGUE_SKILL_RETRIEVAL_FAILED = "We could not load that skill. Please try again later";
    public static final String SKILL_RETIRED = "The skill has been retired. It stays on the profiles and jobs that already carry it";
    public static final String SKILL_RESTORED = "The skill is back in the catalogue";
    public static final String SKILL_RETIRE_FAILED = "We could not retire that skill. Please try again later";
    public static final String SKILL_RESTORE_FAILED = "We could not restore that skill. Please try again later";
    public static final String NO_SKILL_NAME_FOUND = "A skill name is required";
    public static final String NO_SKILL_CATEGORY_FOUND = "A category is required so the skill can be found";
    public static final String SKILL_NAME_ALREADY_IN_CATALOGUE = "A skill named %s is already in the catalogue";
    public static final String SKILL_SLUG_ALREADY_IN_CATALOGUE = "The slug %s is already in the catalogue";
    public static final String PARENT_SKILL_NOT_RECOGNISED = "We do not recognise the parent skill %s";
    // a trade that is its own parent, or a pair that each name the other, reads as a tree with no
    // top and would walk forever on anything that follows it
    public static final String SKILL_CANNOT_PARENT_ITSELF = "A skill cannot be its own parent";
    public static final String SKILL_ALREADY_RETIRED = "That skill is already retired";
    public static final String SKILL_ALREADY_ACTIVE = "That skill is already in the catalogue";

    public static final String USERS_RETRIEVED = "Accounts retrieved";
    public static final String USERS_RETRIEVAL_FAILED = "We could not load the accounts. Please try again later";
    public static final String USER_RETRIEVED = "Account retrieved";
    public static final String USER_RETRIEVAL_FAILED = "We could not load that account. Please try again later";
    public static final String USER_SUSPENDED = "The account has been suspended and can no longer sign in";
    public static final String USER_SUSPENSION_FAILED = "We could not suspend that account. Please try again later";
    public static final String USER_RESTORED = "The account has been restored and can sign in again";
    public static final String USER_RESTORE_FAILED = "We could not restore that account. Please try again later";
    public static final String USER_ALREADY_SUSPENDED = "That account is already suspended";
    public static final String USER_ALREADY_ACTIVE = "That account is already active";
    public static final String NO_SUSPENSION_REASON_FOUND = "A reason is required so the account holder knows why";
    // an admin who could suspend themselves would lock the last approver out of the queue, and
    // there is no route back in from outside
    public static final String ADMIN_CANNOT_SUSPEND_SELF = "You cannot suspend your own account";

    public static final String EMPLOYER_VERIFIED = "The company has been verified";
    public static final String EMPLOYER_UNVERIFIED = "The company's verification has been withdrawn";
    public static final String EMPLOYER_VERIFICATION_FAILED = "We could not change that company's verification. Please try again later";
    public static final String EMPLOYER_AUTO_APPROVAL_GRANTED = "The company's jobs will now skip the approval queue";
    public static final String EMPLOYER_AUTO_APPROVAL_WITHDRAWN = "The company's jobs will go through the approval queue again";
    public static final String EMPLOYER_AUTO_APPROVAL_FAILED = "We could not change that company's approval setting. Please try again later";
    // the switch is the strongest thing the platform grants a company, so it is only ever given
    // to one it has already checked
    public static final String EMPLOYER_NOT_VERIFIED = "Verify the company before letting its jobs skip the approval queue";

    public static final String WORKER_ID_VERIFIED = "The worker's identity has been verified";
    public static final String WORKER_ID_UNVERIFIED = "The worker's identity verification has been withdrawn";
    public static final String WORKER_BACKGROUND_CHECKED = "The worker's background check has been recorded";
    public static final String WORKER_BACKGROUND_CHECK_WITHDRAWN = "The worker's background check has been withdrawn";
    public static final String WORKER_VERIFICATION_FAILED = "We could not change that worker's verification. Please try again later";
    public static final String WORKER_SKILL_VERIFIED = "The worker's certification has been verified";
    public static final String WORKER_SKILL_UNVERIFIED = "The worker's certification verification has been withdrawn";
    public static final String WORKER_SKILL_VERIFICATION_FAILED = "We could not change that certification. Please try again later";
    public static final String NO_WORKER_PROFILE_FOUND = "We could not find that worker";
    public static final String NO_COMPANY_FOUND = "We could not find that company";

    public static final String OVERVIEW_RETRIEVED = "Overview retrieved";
    public static final String OVERVIEW_RETRIEVAL_FAILED = "We could not load the overview. Please try again later";

    // the three "we looked and it is not there" messages the admin side needs. they are worded
    // apart from the worker's NO_SKILL_FOUND, which asks for a skill rather than reports a
    // missing one - an approver reading "a skill is required" after sending a bad id learns
    // nothing about what went wrong
    public static final String NO_CATALOGUE_SKILL_FOUND = "We could not find that skill in the catalogue";
    public static final String NO_USER_ACCOUNT_FOUND = "We could not find that account";
    public static final String NO_WORKER_SKILL_FOUND = "We could not find that certification";

    public static final String EXECUTIONS_RETRIEVED = "Jobs retrieved";
    public static final String EXECUTIONS_RETRIEVAL_FAILED = "We could not load your jobs. Please try again later";
    public static final String EXECUTION_RETRIEVED = "Job retrieved";
    public static final String EXECUTION_RETRIEVAL_FAILED = "We could not load that job. Please try again later";
    // a worker is only ever shown their own assignments and a company only the ones on its own
    // jobs, so an execution belonging to anybody else is answered as missing rather than refused
    public static final String NO_EXECUTION_FOUND = "We could not find that job";

    public static final String TRAVEL_STARTED = "You are on your way. Your location is now being shared with the company until you finish";
    public static final String TRAVEL_START_FAILED = "We could not start your journey. Please try again later";
    public static final String ARRIVAL_RECORDED = "You have been marked as on site";
    // arriving outside the site's own radius is still recorded, because a large site, a yard
    // entrance round the back and plain gps drift all read the same way from here. what it does
    // not get is the verified flag, so the company can see the difference
    public static final String ARRIVAL_RECORDED_OFF_SITE = "You have been marked as on site. Your position is away from the site, so the company will see the arrival as unconfirmed";
    public static final String ARRIVAL_FAILED = "We could not record your arrival. Please try again later";
    public static final String WORK_STARTED = "Work has started and the clock is running";
    public static final String WORK_START_FAILED = "We could not start the work. Please try again later";
    public static final String WORK_PAUSED = "Work is paused. The break is not billed";
    public static final String WORK_PAUSE_FAILED = "We could not pause the work. Please try again later";
    public static final String WORK_RESUMED = "Work has resumed";
    public static final String WORK_RESUME_FAILED = "We could not resume the work. Please try again later";
    public static final String WORK_COMPLETED = "The job has been marked complete";
    public static final String WORK_COMPLETED_OFF_SITE = "The job has been marked complete. Your position is away from the site, so the company will see the completion as unconfirmed";
    public static final String WORK_COMPLETE_FAILED = "We could not complete the job. Please try again later";

    // the step the worker is on decides which step is allowed next, so every refusal names
    // where they actually are. "that is not allowed" on a phone in somebody's pocket halfway
    // up a ladder is not something they can act on
    public static final String TRAVEL_NOT_STARTABLE = "You can only set off on a job you have not started. This one is %s";
    public static final String ARRIVAL_NOT_RECORDABLE = "You can only arrive on a job you have not started or are travelling to. This one is %s";
    public static final String WORK_NOT_STARTABLE = "You can only start work once you are on site. This one is %s";
    public static final String WORK_NOT_PAUSABLE = "You can only pause work that is running. This one is %s";
    public static final String WORK_NOT_RESUMABLE = "You can only resume work that is paused. This one is %s";
    public static final String WORK_NOT_COMPLETABLE = "You can only complete a job you are on site for. This one is %s";

    public static final String LOCATION_RECORDED = "Location recorded";
    public static final String LOCATION_RECORD_FAILED = "We could not record your location. Please try again later";
    public static final String LOCATION_HISTORY_RETRIEVED = "Location history retrieved";
    public static final String LOCATION_HISTORY_RETRIEVAL_FAILED = "We could not load the location history. Please try again later";
    public static final String NO_LOCATION_FOUND = "A latitude and a longitude are required";
    // a phone still reporting its position after the job has ended is following a person around
    // rather than tracking a job. the stream is open for exactly as long as the work is
    public static final String TRACKING_CLOSED = "This job has ended, so your location is no longer being recorded";
    public static final String TRACKING_NOT_OPEN = "Set off on the job before sharing your location";

    public static final String SESSIONS_RETRIEVED = "Work sessions retrieved";
    public static final String SESSIONS_RETRIEVAL_FAILED = "We could not load the work sessions. Please try again later";

    public static final String EXTENSION_REQUESTED = "Your request for more time has been sent to the company";
    public static final String EXTENSION_REQUEST_FAILED = "We could not send your request. Please try again later";
    public static final String EXTENSIONS_RETRIEVED = "Extension requests retrieved";
    public static final String EXTENSIONS_RETRIEVAL_FAILED = "We could not load the extension requests. Please try again later";
    public static final String EXTENSION_APPROVED = "The extra time has been granted and the worker has been told";
    public static final String EXTENSION_APPROVAL_FAILED = "We could not grant the extra time. Please try again later";
    public static final String EXTENSION_REJECTED = "The request for more time has been turned down and the worker has been told";
    public static final String EXTENSION_REJECTION_FAILED = "We could not turn down the request. Please try again later";
    public static final String NO_EXTENSION_FOUND = "We could not find that request";
    public static final String NO_EXTENSION_HOURS_FOUND = "The number of extra hours is required";
    public static final String INVALID_EXTENSION_HOURS = "Extra hours must be between 1 and %s";
    public static final String NO_EXTENSION_REASON_FOUND = "A reason is required so the company knows why the job is running over";
    public static final String INVALID_WORK_COMPLETED_PERCENTAGE = "How far through the work you are must be between 0 and 100";
    public static final String EXTENSION_NOT_REQUESTABLE = "You can only ask for more time on a job that is under way. This one is %s";
    // asking twice while the first ask is still outstanding would leave the company two requests
    // to answer for the same overrun, and whichever they answered second would move the finish
    // time a second time
    public static final String EXTENSION_ALREADY_PENDING = "You have already asked for more time on this job and the company has not answered yet";
    public static final String EXTENSIONS_EXHAUSTED = "This job allows %s extensions and you have used them all";
    public static final String EXTENSION_ALREADY_ANSWERED = "That request has already been answered";
    public static final String EXTENSION_EXPIRED = "That request has expired. The worker will need to ask again";
    public static final String INVALID_APPROVED_HOURS = "The hours granted must be between 1 and the %s asked for";

    public static final String MILESTONE_ADDED = "The step has been added to the job";
    public static final String MILESTONE_ADD_FAILED = "We could not add that step. Please try again later";
    public static final String MILESTONES_RETRIEVED = "Steps retrieved";
    public static final String MILESTONES_RETRIEVAL_FAILED = "We could not load the steps. Please try again later";
    public static final String MILESTONE_STARTED = "The step has been marked as under way";
    public static final String MILESTONE_START_FAILED = "We could not start that step. Please try again later";
    public static final String MILESTONE_COMPLETED = "The step has been marked complete";
    public static final String MILESTONE_COMPLETE_FAILED = "We could not complete that step. Please try again later";
    public static final String MILESTONE_BLOCKED = "The step has been marked as blocked and the company has been told";
    public static final String MILESTONE_BLOCK_FAILED = "We could not mark that step as blocked. Please try again later";
    public static final String MILESTONE_APPROVED = "The step has been signed off";
    public static final String MILESTONE_APPROVAL_FAILED = "We could not sign off that step. Please try again later";
    public static final String NO_MILESTONE_FOUND = "We could not find that step";
    public static final String NO_MILESTONE_TITLE_FOUND = "A title is required so the step can be told apart from the others";
    public static final String INVALID_MILESTONE_WEIGHT = "A step's share of the job must be between 1 and 100";
    public static final String MILESTONE_ALREADY_COMPLETED = "That step is already complete";
    public static final String MILESTONE_NOT_APPROVABLE = "Only a completed step can be signed off. This one is %s";
    public static final String NO_BLOCKED_REASON_FOUND = "A reason is required so the company knows what is holding the step up";

    public static final String MONITOR_RETRIEVED = "Live monitor retrieved";
    public static final String MONITOR_RETRIEVAL_FAILED = "We could not load the live monitor. Please try again later";

    public static final String NOTIFICATIONS_RETRIEVED = "Notifications retrieved";
    public static final String NOTIFICATIONS_RETRIEVAL_FAILED = "We could not load your notifications. Please try again later";
    public static final String NOTIFICATION_READ = "The notification has been marked as read";
    public static final String NOTIFICATION_READ_FAILED = "We could not mark that notification as read. Please try again later";
    public static final String NO_NOTIFICATION_FOUND = "We could not find that notification";
}
