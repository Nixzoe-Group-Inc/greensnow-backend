package com.greensnow.worker.repository;

import com.greensnow.worker.model.entity.JobMilestone;
import com.greensnow.worker.model.enums.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobMilestoneRepository extends JpaRepository<JobMilestone, String> {

    // the order the work is meant to be done in, which is the employer's ordering rather
    // than the order the rows happened to be written
    List<JobMilestone> findByJobExecutionIdOrderByDisplayOrderAsc(String jobExecutionId);

    Optional<JobMilestone> findByIdAndJobExecutionId(String id, String jobExecutionId);

    List<JobMilestone> findByJobExecutionIdIn(List<String> jobExecutionIds);

    long countByJobExecutionId(String jobExecutionId);

    long countByJobExecutionIdAndStatus(String jobExecutionId, MilestoneStatus status);
}
