package com.greensnow.worker.repository;

import com.greensnow.worker.model.entity.Job;
import com.greensnow.worker.model.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, String> {

    Optional<Job> findByIdAndStatus(String id, JobStatus status);

    // the one query the worker side browses on. APPROVED is the only status a job can be picked
    // in, and a job already carrying every worker it asked for is not offered - it is still
    // APPROVED until the employer closes it, so the staffing check has to be here rather than
    // left to the status
    @Query("""
            SELECT j FROM Job j
            WHERE j.status = :status
              AND j.workersAssigned < j.workersNeeded
            ORDER BY j.urgent DESC, j.publishedAt DESC
            """)
    List<Job> findPickable(@Param("status") JobStatus status);

    @Query("""
            SELECT j FROM Job j
            WHERE j.status = :status
              AND j.workersAssigned < j.workersNeeded
              AND (LOWER(j.title) LIKE LOWER(CONCAT('%', :term, '%'))
                   OR LOWER(j.description) LIKE LOWER(CONCAT('%', :term, '%')))
            ORDER BY j.urgent DESC, j.publishedAt DESC
            """)
    List<Job> searchPickable(@Param("status") JobStatus status, @Param("term") String term);

    List<Job> findByEmployerProfileIdOrderByCreatedAtDesc(String employerProfileId);

    List<Job> findByEmployerProfileIdAndStatusOrderByCreatedAtDesc(String employerProfileId, JobStatus status);

    // every employer read goes through this rather than findById, so another employer's job id
    // answers exactly as a job id that was never issued does. phrased as not-found on purpose:
    // telling them the job exists but is not theirs is itself a disclosure
    Optional<Job> findByIdAndEmployerProfileId(String id, String employerProfileId);

    // the approval queue, oldest first. a newest-first queue would leave the job that has been
    // waiting longest waiting longer still every time another company posts one, and the company
    // whose work never gets reviewed is the company that stops posting
    List<Job> findByStatusOrderByCreatedAtAsc(JobStatus status);

    // the approver's browse, which is the opposite order: what was posted most recently is what
    // is most likely being asked about
    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);

    List<Job> findAllByOrderByCreatedAtDesc();

    long countByStatus(JobStatus status);
}
