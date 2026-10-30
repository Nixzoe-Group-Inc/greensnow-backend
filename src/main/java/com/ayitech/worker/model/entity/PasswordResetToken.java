package com.ayitech.worker.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseEntity {

    private String userId;

    // the bcrypt hash of the six digit code, never the code itself - a reset code is a
    // credential for the window it lives, so a database read must not hand anyone a live one
    private String tokenHash;

    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    private int attempts;

}
