package com.greensnow.worker.repository;

import com.greensnow.worker.model.entity.JobApplication;
import com.greensnow.worker.model.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, String> {

    List<JobApplication> findByEmployeeProfileIdOrderByAppliedAtDesc(String employeeProfileId);

    List<JobApplication> findByEmployeeProfileIdAndStatusOrderByAppliedAtDesc(String employeeProfileId,
                                                                              ApplicationStatus status);

    Optional<JobApplication> findByIdAndEmployeeProfileId(String id, String employeeProfileId);

    boolean existsByJobIdAndEmployeeProfileId(String jobId, String employeeProfileId);

    // what the browse list reads to mark the jobs a worker has already gone for, so the same
    // job is not offered back to them as though it were new
    List<JobApplication> findByEmployeeProfileId(String employeeProfileId);

    // the employer's side of the same table. best match first, because the score is what the
    // employer is choosing on and a list in the order people happened to apply buries it
    List<JobApplication> findByJobIdOrderByMatchScoreDesc(String jobId);

    List<JobApplication> findByJobIdAndStatusOrderByMatchScoreDesc(String jobId, ApplicationStatus status);

    Optional<JobApplication> findByIdAndJobId(String id, String jobId);
}
