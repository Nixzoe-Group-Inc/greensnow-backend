package com.ayitech.worker.repository;

import com.ayitech.worker.model.entity.JobApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApprovalRepository extends JpaRepository<JobApproval, String> {

    // newest first, because a job can go round the approval loop more than once and what the
    // employer needs to read is the last thing that was said about it
    List<JobApproval> findByJobIdOrderByCreatedAtDesc(String jobId);

    // what one approver has answered. the queue itself is read off the job's status rather than
    // off this table - a job sits in the queue because of what it is, not because of a row saying
    // it was submitted, and the two could otherwise disagree
    List<JobApproval> findByReviewerIdOrderByCreatedAtDesc(String reviewerId);
}
