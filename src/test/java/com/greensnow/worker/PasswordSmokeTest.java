package com.greensnow.worker;

import com.greensnow.worker.model.entity.PasswordResetToken;
import com.greensnow.worker.model.entity.User;
import com.greensnow.worker.model.enums.UserType;
import com.greensnow.worker.model.request.ChangePasswordRequest;
import com.greensnow.worker.model.request.ForgotPasswordRequest;
import com.greensnow.worker.model.request.LoginRequest;
import com.greensnow.worker.model.request.RegistrationRequest;
import com.greensnow.worker.model.request.ResetPasswordRequest;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.RegistrationResponse;
import com.greensnow.worker.repository.PasswordResetTokenRepository;
import com.greensnow.worker.repository.UserRepository;
import com.greensnow.worker.service.auth.AuthRequestBuilder;
import com.greensnow.worker.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PasswordSmokeTest {

    private static final String KNOWN_CODE = "246813";

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthRequestBuilder requestBuilder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private String registerWorker(String email, String phoneNumber) {
        RegistrationResponse registered = authService.registerUser(RegistrationRequest.builder()
                .userType(UserType.EMPLOYEE)
                .email(email)
                .phoneNumber(phoneNumber)
                .firstName("Ama")
                .lastName("Owusu")
                .password("first-password-1")
                .build());
        assertThat(registered.getResponseCode()).isEqualTo("01");
        return registered.getUserId();
    }

    // the service only ever logs the code it generates, so a test cannot read one back. it
    // plants a token carrying a code it already knows, which is the same row forgotPassword
    // writes - buildPasswordResetToken is the one that writes it either way
    private void plantResetCode(String userId) {
        User user = userRepository.findById(userId).orElseThrow();
        passwordResetTokenRepository.save(requestBuilder.buildPasswordResetToken(user, KNOWN_CODE));
    }

    @Test
    void aWorkerWhoKnowsTheirPasswordCanChangeItAndTheOldOneStopsWorking() {
        String userId = registerWorker("ama@worker.test", "233200000011");

        // the wrong current password is refused
        assertThat(authService.changePassword(userId, ChangePasswordRequest.builder()
                .currentPassword("not-the-password").newPassword("second-password-2").build())
                .getResponseCode()).isEqualTo("105");

        // a new password that is really the current one is refused rather than quietly accepted
        assertThat(authService.changePassword(userId, ChangePasswordRequest.builder()
                .currentPassword("first-password-1").newPassword("first-password-1").build())
                .getResponseCode()).isEqualTo("110");

        // too short is refused before anything is written
        assertThat(authService.changePassword(userId, ChangePasswordRequest.builder()
                .currentPassword("first-password-1").newPassword("short").build())
                .getResponseCode()).isEqualTo("110");

        assertThat(authService.changePassword(userId, ChangePasswordRequest.builder()
                .currentPassword("first-password-1").newPassword("second-password-2").build())
                .getResponseCode()).isEqualTo("01");

        // the new password signs in and the old one no longer does
        assertThat(authService.login(LoginRequest.builder()
                .email("ama@worker.test").password("second-password-2").build())
                .getResponseCode()).isEqualTo("01");
        assertThat(authService.login(LoginRequest.builder()
                .email("ama@worker.test").password("first-password-1").build())
                .getResponseCode()).isEqualTo("105");
    }

    @Test
    void anEmailNobodyHoldsIsAnsweredExactlyAsAHeldOneIs() {
        registerWorker("kwabena@worker.test", "233200000012");

        GeneralResponse unknown = authService.forgotPassword(ForgotPasswordRequest.builder()
                .email("stranger@worker.test").build());
        GeneralResponse known = authService.forgotPassword(ForgotPasswordRequest.builder()
                .email("kwabena@worker.test").build());

        // identical code and identical wording, so the api cannot be used to discover which
        // emails have accounts
        assertThat(unknown.getResponseCode()).isEqualTo(known.getResponseCode()).isEqualTo("01");
        assertThat(unknown.getResponseMessage()).isEqualTo(known.getResponseMessage());
    }

    @Test
    void aRequestedResetCodeIsStoredHashedAndExpiring() {
        String userId = registerWorker("adwoa@worker.test", "233200000013");

        authService.forgotPassword(ForgotPasswordRequest.builder().email("adwoa@worker.test").build());

        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId).orElseThrow();

        assertThat(token.getTokenHash()).startsWith("$2");
        assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(token.getAttempts()).isZero();
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void aResetCodeWorksOnceAndTheWrongOneIsCounted() {
        String userId = registerWorker("kofi@worker.test", "233200000014");
        plantResetCode(userId);

        // a wrong code is refused and counted against the cap
        assertThat(authService.resetPassword(ResetPasswordRequest.builder()
                .email("kofi@worker.test").resetCode("000000").newPassword("third-password-3").build())
                .getResponseCode()).isEqualTo("110");
        assertThat(passwordResetTokenRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId).orElseThrow()
                .getAttempts()).isEqualTo(1);

        assertThat(authService.resetPassword(ResetPasswordRequest.builder()
                .email("kofi@worker.test").resetCode(KNOWN_CODE).newPassword("third-password-3").build())
                .getResponseCode()).isEqualTo("01");

        assertThat(authService.login(LoginRequest.builder()
                .email("kofi@worker.test").password("third-password-3").build())
                .getResponseCode()).isEqualTo("01");

        // the same code cannot reset a second time
        assertThat(authService.resetPassword(ResetPasswordRequest.builder()
                .email("kofi@worker.test").resetCode(KNOWN_CODE).newPassword("fourth-password-4").build())
                .getResponseCode()).isEqualTo("110");
        assertThat(authService.login(LoginRequest.builder()
                .email("kofi@worker.test").password("fourth-password-4").build())
                .getResponseCode()).isEqualTo("105");
    }

    @Test
    void askingForANewCodeSpendsTheOldOne() {
        String userId = registerWorker("yaa@worker.test", "233200000015");
        plantResetCode(userId);

        PasswordResetToken first = passwordResetTokenRepository
                .findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(userId).orElseThrow();

        authService.forgotPassword(ForgotPasswordRequest.builder().email("yaa@worker.test").build());

        // the planted code is spent, so only the newest one can reset
        assertThat(passwordResetTokenRepository.findById(first.getId()).orElseThrow().getUsedAt()).isNotNull();
        assertThat(authService.resetPassword(ResetPasswordRequest.builder()
                .email("yaa@worker.test").resetCode(KNOWN_CODE).newPassword("fifth-password-5").build())
                .getResponseCode()).isEqualTo("110");
    }

    @Test
    void changingThePasswordSpendsAnyResetInFlight() {
        String userId = registerWorker("esi@worker.test", "233200000016");
        plantResetCode(userId);

        assertThat(authService.changePassword(userId, ChangePasswordRequest.builder()
                .currentPassword("first-password-1").newPassword("second-password-2").build())
                .getResponseCode()).isEqualTo("01");

        // a code requested before the change cannot be used afterwards to set the password back
        assertThat(authService.resetPassword(ResetPasswordRequest.builder()
                .email("esi@worker.test").resetCode(KNOWN_CODE).newPassword("first-password-1").build())
                .getResponseCode()).isEqualTo("110");
    }
}
