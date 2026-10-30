package com.ayitech.worker.repository;

import com.ayitech.worker.model.entity.JobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface JobRequirementRepository extends JpaRepository<JobRequirement, String> {

    List<JobRequirement> findByJobId(String jobId);

    // read for the whole page of jobs at once - scoring a list one job at a time is a query
    // per row, which is the browse screen every worker opens the app on
    List<JobRequirement> findByJobIdIn(Collection<String> jobIds);
}
