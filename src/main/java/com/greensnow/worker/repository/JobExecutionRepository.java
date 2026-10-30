package com.greensnow.worker.repository;

import com.greensnow.worker.model.entity.JobExecution;
import com.greensnow.worker.model.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobExecutionRepository extends JpaRepository<JobExecution, String> {

    // a worker's own board. newest first, because the job they are on today is the one they
    // open the app for and yesterday's finished work is history
    List<JobExecution> findByEmployeeProfileIdOrderByAcceptedAtDesc(String employeeProfileId);

    List<JobExecution> findByEmployeeProfileIdAndStatusInOrderByAcceptedAtDesc(String employeeProfileId,
                                                                               List<ExecutionStatus> statuses);

    // every read on the worker's side goes through this rather than findById, so a worker
    // cannot ping, pause or complete a job that belongs to somebody else
    Optional<JobExecution> findByIdAndEmployeeProfileId(String id, String employeeProfileId);

    boolean existsByJobIdAndEmployeeProfileId(String jobId, String employeeProfileId);

    List<JobExecution> findByJobIdOrderByAcceptedAtDesc(String jobId);

    // the monitor. the company's jobs are resolved first and the live executions read off
    // them in one go, because a dashboard that queried per job would open one round trip for
    // every job the company has ever posted
    List<JobExecution> findByJobIdInAndStatusInOrderByAcceptedAtDesc(List<String> jobIds,
                                                                     List<ExecutionStatus> statuses);

    List<JobExecution> findByJobIdIn(List<String> jobIds);

    long countByEmployeeProfileIdAndStatusIn(String employeeProfileId, List<ExecutionStatus> statuses);
}
