package com.ayitech.worker.repository;

import com.ayitech.worker.model.entity.EmployerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, String> {

    Optional<EmployerProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    long countByVerifiedTrue();
}
