package com.greensnow.worker.service.admin;

import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.entity.JobApproval;
import com.greensnow.worker.model.entity.Skill;
import com.greensnow.worker.model.enums.ApprovalAction;
import com.greensnow.worker.model.enums.ApprovalStatus;
import com.greensnow.worker.model.request.JobReviewRequest;
import com.greensnow.worker.model.request.SkillRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
public class AdminRequestBuilder {

    // a trade is born usable. there is no route that creates a retired one, because a trade
    // nobody can hold and no job can ask for is not a catalogue entry, it is a note to self
    public Skill buildSkill(SkillRequest request) {
        return Skill.builder()
                .name(trimmed(request.getName()))
                .slug(resolvedSlug(request))
                .category(trimmed(request.getCategory()))
                .description(trimmed(request.getDescription()))
                .parentSkillId(trimmed(request.getParentSkillId()))
                .requiresCertification(Boolean.TRUE.equals(request.getRequiresCertification()))
                .active(true)
                .build();
    }

    // the same whole-record rule the employer side uses: what the request carries is what the
    // trade reads as afterwards, and a field left out is cleared. active is deliberately not on
    // the request and so is never touched here - retiring a trade takes a plumber off every
    // profile that holds it and out of every match, which is far too large a thing to happen as
    // a side effect of someone correcting a spelling
    public Skill buildSkillUpdate(Skill skill, SkillRequest request) {
        skill.setName(trimmed(request.getName()));
        skill.setSlug(resolvedSlug(request));
        skill.setCategory(trimmed(request.getCategory()));
        skill.setDescription(trimmed(request.getDescription()));
        skill.setParentSkillId(trimmed(request.getParentSkillId()));
        skill.setRequiresCertification(Boolean.TRUE.equals(request.getRequiresCertification()));
        return skill;
    }

    // the three answers an approver can give. each one names who gave it and how long the
    // company waited for it, because the queue is the one part of this platform that stands
    // between a company posting work and a worker earning from it - an approver nobody can name
    // and a wait nobody measured is a queue nobody can hold to account
    public JobApproval buildApproval(Job job, String reviewerId, JobReviewRequest request) {
        return JobApproval.builder()
                .jobId(job.getId())
                .reviewerId(reviewerId)
                .status(ApprovalStatus.APPROVED)
                .action(ApprovalAction.APPROVED)
                .comments(trimmed(request.getComments()))
                .reviewedAt(LocalDateTime.now())
                .reviewDurationSeconds(waitedSeconds(job))
                .build();
    }

    // the reason is kept apart from the comments on purpose. the reason is what the company is
    // owed and what it has to answer, where a comment is whatever the approver wanted to add
    public JobApproval buildRejection(Job job, String reviewerId, JobReviewRequest request) {
        return JobApproval.builder()
                .jobId(job.getId())
                .reviewerId(reviewerId)
                .status(ApprovalStatus.REJECTED)
                .action(ApprovalAction.REJECTED)
                .comments(trimmed(request.getComments()))
                .rejectionReason(trimmed(request.getReason()))
                .reviewedAt(LocalDateTime.now())
                .reviewDurationSeconds(waitedSeconds(job))
                .build();
    }

    // not a refusal: the company is being asked to fix something and send it back. it keeps the
    // same reason field, because what has to change is the same thing a refusal would have named
    public JobApproval buildChangesRequested(Job job, String reviewerId, JobReviewRequest request) {
        return JobApproval.builder()
                .jobId(job.getId())
                .reviewerId(reviewerId)
                .status(ApprovalStatus.CHANGES_REQUESTED)
                .action(ApprovalAction.CHANGES_REQUESTED)
                .comments(trimmed(request.getComments()))
                .rejectionReason(trimmed(request.getReason()))
                .reviewedAt(LocalDateTime.now())
                .reviewDurationSeconds(waitedSeconds(job))
                .build();
    }

    // how long the job sat in the queue. a job with no submission time recorded is measured as
    // nothing rather than as a negative or a throw - the figure is there to be reported on, and
    // losing one review's duration is not worth losing the review itself
    private Long waitedSeconds(Job job) {
        if (Objects.isNull(job.getSubmittedForApprovalAt())) {
            return 0L;
        }
        long seconds = Duration.between(job.getSubmittedForApprovalAt(), LocalDateTime.now()).getSeconds();
        return Math.max(seconds, 0L);
    }

    // the slug is what a url and an import file name a trade by, so it is derived from the name
    // when nobody supplied one rather than left null. a trade with no slug is one that can only
    // ever be referred to by its generated id, which is of no use to anyone writing an integration
    private String resolvedSlug(SkillRequest request) {
        if (StringUtils.hasText(request.getSlug())) {
            return request.getSlug().trim().toLowerCase(Locale.ROOT);
        }
        if (!StringUtils.hasText(request.getName())) {
            return null;
        }
        return request.getName().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private String trimmed(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
