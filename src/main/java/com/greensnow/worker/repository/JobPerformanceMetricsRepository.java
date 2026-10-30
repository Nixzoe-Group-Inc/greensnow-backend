package com.greensnow.worker.repository;

import com.greensnow.worker.model.entity.JobPerformanceMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPerformanceMetricsRepository extends JpaRepository<JobPerformanceMetrics, String> {

    Optional<JobPerformanceMetrics> findByJobExecutionId(String jobExecutionId);
}
