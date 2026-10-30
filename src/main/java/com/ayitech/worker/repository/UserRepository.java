package com.ayitech.worker.repository;

import com.ayitech.worker.model.entity.User;
import com.ayitech.worker.model.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByUserTypeOrderByCreatedAtDesc(UserType userType);

    long countByUserType(UserType userType);
}
