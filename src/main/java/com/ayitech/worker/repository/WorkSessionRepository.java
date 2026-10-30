package com.ayitech.worker.repository;

import com.ayitech.worker.model.entity.WorkSession;
import com.ayitech.worker.model.enums.WorkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkSessionRepository extends JpaRepository<WorkSession, String> {

    List<WorkSession> findByJobExecutionIdOrderByStartedAtAsc(String jobExecutionId);

    // the session the worker is inside right now - the one with no end on it. everything
    // that closes a stretch of time goes looking for this first, because a second open
    // session on the same execution would have the worker on the clock twice over
    Optional<WorkSession> findFirstByJobExecutionIdAndEndedAtIsNullOrderByStartedAtDesc(String jobExecutionId);

    Optional<WorkSession> findFirstByJobExecutionIdAndWorkTypeAndEndedAtIsNullOrderByStartedAtDesc(
            String jobExecutionId, WorkType workType);

    List<WorkSession> findByJobExecutionIdInOrderByStartedAtAsc(List<String> jobExecutionIds);
}
