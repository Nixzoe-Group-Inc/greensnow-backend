package com.ayitech.worker.repository;

import com.ayitech.worker.model.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, String> {

    Optional<EmployeeProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
