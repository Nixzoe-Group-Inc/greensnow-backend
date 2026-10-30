package com.ayitech.worker.repository;

import com.ayitech.worker.model.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(String userId);

    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(String userId);
}
