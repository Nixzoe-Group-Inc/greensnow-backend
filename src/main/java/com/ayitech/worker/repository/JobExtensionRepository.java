package com.ayitech.worker.repository;

import com.ayitech.worker.model.entity.JobExtension;
import com.ayitech.worker.model.enums.ExtensionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobExtensionRepository extends JpaRepository<JobExtension, String> {

    List<JobExtension> findByJobExecutionIdOrderBySequenceNumberAsc(String jobExecutionId);

    // one outstanding ask at a time. a worker with two live requests against the same job
    // would have the employer answering a question that a later request has already moved on
    Optional<JobExtension> findFirstByJobExecutionIdAndStatusOrderByRequestedAtDesc(String jobExecutionId,
                                                                                    ExtensionStatus status);

    List<JobExtension> findByJobExecutionIdInAndStatusOrderByRequestedAtAsc(List<String> jobExecutionIds,
                                                                            ExtensionStatus status);

    List<JobExtension> findByJobExecutionIdIn(List<String> jobExecutionIds);

    long countByJobExecutionId(String jobExecutionId);

    long countByJobExecutionIdAndStatus(String jobExecutionId, ExtensionStatus status);
}
