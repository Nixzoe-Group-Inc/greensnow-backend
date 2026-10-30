package com.ayitech.worker.service.auth;

import com.ayitech.worker.model.entity.PasswordResetToken;
import com.ayitech.worker.model.entity.User;
import com.ayitech.worker.model.request.RegistrationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthRequestBuilder {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RESET_CODE_BOUND = 1_000_000;

    @Value("${app.auth.password-reset-expiry-minutes}")
    private long passwordResetExpiryMinutes;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User buildUser(RegistrationRequest request) {
        return User.builder()
                .userType(request.getUserType())
                .email(trimmed(request.getEmail()))
                .phoneNumber(trimmed(request.getPhoneNumber()))
                .firstName(trimmed(request.getFirstName()))
                .lastName(trimmed(request.getLastName()))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .phoneVerified(false)
                .active(true)
                .build();
    }

    // six digits because the audience is on a phone keypad, which is also why the code is
    // attempt limited - a million combinations is nothing to a script, so the cap is what
    // actually makes a short code safe, not its length
    public String buildResetCode() {
        return String.format("%06d", RANDOM.nextInt(RESET_CODE_BOUND));
    }

    public PasswordResetToken buildPasswordResetToken(User user, String resetCode) {
        return PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(passwordEncoder.encode(resetCode))
                .expiresAt(LocalDateTime.now().plusMinutes(passwordResetExpiryMinutes))
                .attempts(0)
                .build();
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String trimmed(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
