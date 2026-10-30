package com.greensnow.worker.repository;

import com.greensnow.worker.model.entity.LocationTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LocationTrackingRepository extends JpaRepository<LocationTracking, String> {

    // replaying one worker's route. oldest first, because a route read backwards is a route
    // drawn backwards on whatever map the employer is looking at
    List<LocationTracking> findByJobExecutionIdOrderByRecordedAtAsc(String jobExecutionId);

    List<LocationTracking> findByJobExecutionIdAndRecordedAtBetweenOrderByRecordedAtAsc(String jobExecutionId,
                                                                                        LocalDateTime from,
                                                                                        LocalDateTime to);

    // where the worker is now, as opposed to where they have been
    Optional<LocationTracking> findFirstByJobExecutionIdOrderByRecordedAtDesc(String jobExecutionId);

    long countByJobExecutionId(String jobExecutionId);
}
