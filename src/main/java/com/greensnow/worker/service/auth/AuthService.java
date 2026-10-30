package com.greensnow.worker.service.auth;

import com.greensnow.worker.model.constant.ResponseCode;
import com.greensnow.worker.model.constant.ResponseMessage;
import com.greensnow.worker.model.entity.PasswordResetToken;
import com.greensnow.worker.model.entity.User;
import com.greensnow.worker.model.request.ChangePasswordRequest;
import com.greensnow.worker.model.request.ForgotPasswordRequest;
import com.greensnow.worker.model.request.LoginRequest;
import com.greensnow.worker.model.request.RegistrationRequest;
import com.greensnow.worker.model.request.ResetPasswordRequest;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.LoginResponse;
import com.greensnow.worker.model.response.RegistrationResponse;
import com.greensnow.worker.repository.PasswordResetTokenRepository;
import com.greensnow.worker.repository.UserRepository;
import com.greensnow.worker.util.JsonUtility;
import com.greensnow.worker.util.MaskUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthRequestBuilder requestBuilder;
    private final AuthResponseProcessor responseProcessor;
    private final JwtService jwtService;

    @Value("${app.auth.min-password-length}")
    private int minPasswordLength;

    @Value("${app.auth.max-reset-attempts}")
    private int maxResetAttempts;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public RegistrationResponse registerUser(RegistrationRequest request) {
        log.info("Going to process registration for {}", MaskUtility.toMaskedJson(request, "password"));
        try {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                log.info("Registration refused for {} because an account already exists for that email", request.getEmail());
                return responseProcessor.failedRegistration(ResponseCode.CODE_DUPLICATE_RECORD,
                        String.format(ResponseMessage.EMAIL_ALREADY_REGISTERED, request.getEmail()));
            }

            if (StringUtils.hasText(request.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                log.info("Registration refused for {} because an account already exists for that phone number", request.getEmail());
                return responseProcessor.failedRegistration(ResponseCode.CODE_DUPLICATE_RECORD,
                        String.format(ResponseMessage.PHONE_NUMBER_ALREADY_REGISTERED, request.getPhoneNumber()));
            }

            log.info("Going to persist a new {} account for {}", request.getUserType(), request.getEmail());
            User user = userRepository.save(requestBuilder.buildUser(request));
            log.info("Response after persisting registration for email : {} {}", request.getEmail(), JsonUtility.toJson(user.getId()));
            return responseProcessor.processRegistrationResponse(user);

        } catch (Exception e) {
            log.info("An exception occurred when attempting to register {} . Exception: ", request.getEmail(), e);
            return responseProcessor.failedRegistration(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.REGISTRATION_FAILED);
        }
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Going to process a login for {}", MaskUtility.toMaskedJson(request, "password"));
        try {
            Optional<User> account = userRepository.findByEmailIgnoreCase(request.getEmail());
            if (account.isEmpty()) {
                log.info("Login refused for {} because no account was found for that email", request.getEmail());
                return responseProcessor.failedLogin(ResponseCode.CODE_UNAUTHORISED, ResponseMessage.INVALID_CREDENTIALS);
            }

            User user = account.get();
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                log.info("Login refused for {} because the password did not match", request.getEmail());
                return responseProcessor.failedLogin(ResponseCode.CODE_UNAUTHORISED, ResponseMessage.INVALID_CREDENTIALS);
            }

            if (!user.isActive()) {
                log.info("Login refused for {} because the account is deactivated", request.getEmail());
                return responseProcessor.failedLogin(ResponseCode.CODE_UNAUTHORISED, ResponseMessage.ACCOUNT_DEACTIVATED);
            }

            log.info("Going to issue an access token for {}", request.getEmail());
            String accessToken = jwtService.generateToken(user);

            // saved rather than left to hibernate to notice - nothing holds a transaction open
            // around this method any more, so a change that is not written here is not written
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Response after authenticating email : {} {}", request.getEmail(), JsonUtility.toJson(user.getId()));
            return responseProcessor.processLoginResponse(user, accessToken, jwtService.expiresIn());

        } catch (Exception e) {
            log.info("An exception occurred when attempting to sign in {} . Exception: ", request.getEmail(), e);
            return responseProcessor.failedLogin(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.LOGIN_FAILED);
        }
    }

    public GeneralResponse logout(String userId) {
        log.info("Going to process a logout for {}", userId);
        try {
            log.info("Response after signing out user : {}", userId);
            return responseProcessor.processLogoutResponse();

        } catch (Exception e) {
            log.info("An exception occurred when attempting to sign out . Exception: ", e);
            return responseProcessor.failedLogout(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.LOGOUT_FAILED);
        }
    }

    public GeneralResponse changePassword(String userId, ChangePasswordRequest request) {
        log.info("Going to process a password change for {}", userId);
        try {
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                log.info("Password change refused for {} because no current password was supplied", userId);
                return responseProcessor.failedPasswordChange(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.NO_CURRENT_PASSWORD_FOUND);
            }

            String rejection = rejectUnusablePassword(request.getNewPassword());
            if (StringUtils.hasText(rejection)) {
                log.info("Password change refused for {} because the new password was not usable", userId);
                return responseProcessor.failedPasswordChange(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            Optional<User> account = userRepository.findById(userId);
            if (account.isEmpty()) {
                log.info("Password change refused for {} because no account was found", userId);
                return responseProcessor.failedPasswordChange(ResponseCode.CODE_RECORD_NOT_FOUND, ResponseMessage.ACCOUNT_NOT_FOUND);
            }

            User user = account.get();
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                log.info("Password change refused for {} because the current password did not match", userId);
                return responseProcessor.failedPasswordChange(ResponseCode.CODE_UNAUTHORISED, ResponseMessage.INVALID_CURRENT_PASSWORD);
            }

            if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
                log.info("Password change refused for {} because the new password is the current one", userId);
                return responseProcessor.failedPasswordChange(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.PASSWORD_UNCHANGED);
            }

            // a password change invalidates any reset in flight, so a code requested before it
            // cannot be used afterwards to set the password back. done before the password is
            // written for the same reason the reset route spends its code first - these are two
            // separate writes now, and the one that must not be skipped is this one
            discardOutstandingResetTokens(user);

            log.info("Going to persist a new password for {}", userId);
            user.setPasswordHash(requestBuilder.encodePassword(request.getNewPassword()));
            userRepository.save(user);

            log.info("Response after changing the password for user : {} {}", userId, JsonUtility.toJson(ResponseCode.CODE_01));
            return responseProcessor.processPasswordChangeResponse();

        } catch (Exception e) {
            log.info("An exception occurred when attempting to change the password for {} . Exception: ", userId, e);
            return responseProcessor.failedPasswordChange(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.PASSWORD_CHANGE_FAILED);
        }
    }

    public GeneralResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Going to process a password reset request for {}", request.getEmail());
        try {
            if (!StringUtils.hasText(request.getEmail())) {
                log.info("Password reset request refused because no email was supplied");
                return responseProcessor.failedForgotPassword(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.NO_EMAIL_FOUND);
            }

            Optional<User> account = userRepository.findByEmailIgnoreCase(request.getEmail());
            if (account.isEmpty()) {
                // answered exactly as a found account is - see ResponseMessage.PASSWORD_RESET_REQUESTED
                log.info("No account was found for {} , answering as though there were", request.getEmail());
                return responseProcessor.processForgotPasswordResponse();
            }

            User user = account.get();
            if (!user.isActive()) {
                log.info("No reset code issued for {} because the account is deactivated", request.getEmail());
                return responseProcessor.processForgotPasswordResponse();
            }

            // only one code is live at a time, so an older one cannot be used after a newer is asked for
            discardOutstandingResetTokens(user);

            String resetCode = requestBuilder.buildResetCode();
            PasswordResetToken token = passwordResetTokenRepository.save(requestBuilder.buildPasswordResetToken(user, resetCode));

            // nothing sends this yet - there is no mail or sms service in the project. this log
            // line IS the delivery channel for now, and is the seam a notification service drops
            // into. it must come out the moment one exists: a reset code in a log file is a
            // credential anyone with log access can use
            log.info("Reset code for {} is {} and expires at {}", request.getEmail(), resetCode, token.getExpiresAt());

            log.info("Response after requesting a password reset for email : {} {}", request.getEmail(), JsonUtility.toJson(token.getId()));
            return responseProcessor.processForgotPasswordResponse();

        } catch (Exception e) {
            log.info("An exception occurred when attempting to start a password reset for {} . Exception: ", request.getEmail(), e);
            return responseProcessor.failedForgotPassword(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.PASSWORD_RESET_REQUEST_FAILED);
        }
    }

    public GeneralResponse resetPassword(ResetPasswordRequest request) {
        log.info("Going to process a password reset for {}", MaskUtility.toMaskedJson(request, "resetCode", "newPassword"));
        try {
            if (!StringUtils.hasText(request.getEmail())) {
                log.info("Password reset refused because no email was supplied");
                return responseProcessor.failedPasswordReset(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.NO_EMAIL_FOUND);
            }

            if (!StringUtils.hasText(request.getResetCode())) {
                log.info("Password reset refused for {} because no reset code was supplied", request.getEmail());
                return responseProcessor.failedPasswordReset(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.NO_RESET_CODE_FOUND);
            }

            String rejection = rejectUnusablePassword(request.getNewPassword());
            if (StringUtils.hasText(rejection)) {
                log.info("Password reset refused for {} because the new password was not usable", request.getEmail());
                return responseProcessor.failedPasswordReset(ResponseCode.CODE_INVALID_REQUEST, rejection);
            }

            Optional<User> account = userRepository.findByEmailIgnoreCase(request.getEmail());
            if (account.isEmpty()) {
                log.info("Password reset refused for {} because no account was found for that email", request.getEmail());
                return responseProcessor.failedPasswordReset(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.INVALID_RESET_CODE);
            }

            User user = account.get();
            Optional<PasswordResetToken> outstanding =
                    passwordResetTokenRepository.findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(user.getId());
            if (outstanding.isEmpty()) {
                log.info("Password reset refused for {} because no reset code is outstanding", request.getEmail());
                return responseProcessor.failedPasswordReset(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.INVALID_RESET_CODE);
            }

            PasswordResetToken token = outstanding.get();
            if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.info("Password reset refused for {} because the reset code expired at {}", request.getEmail(), token.getExpiresAt());
                return responseProcessor.failedPasswordReset(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.INVALID_RESET_CODE);
            }

            // the cap is what makes a six digit code safe at all - without it a million guesses
            // is minutes of scripted requests
            if (token.getAttempts() >= maxResetAttempts) {
                log.info("Password reset refused for {} after {} incorrect attempts", request.getEmail(), token.getAttempts());
                return responseProcessor.failedPasswordReset(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.TOO_MANY_RESET_ATTEMPTS);
            }

            if (!passwordEncoder.matches(request.getResetCode(), token.getTokenHash())) {
                token.setAttempts(token.getAttempts() + 1);
                passwordResetTokenRepository.save(token);
                log.info("Password reset refused for {} because the reset code did not match, attempt {}", request.getEmail(), token.getAttempts());
                return responseProcessor.failedPasswordReset(ResponseCode.CODE_INVALID_REQUEST, ResponseMessage.INVALID_RESET_CODE);
            }

            // single use, and spent BEFORE the password is written. the two writes are no longer
            // one unit, so one of them has to be able to fail first: a spent code with the old
            // password still in place costs the user a new code, where a changed password with
            // the code still live leaves a credential anyone holding it can reuse
            log.info("Going to spend the reset code for {}", request.getEmail());
            token.setUsedAt(LocalDateTime.now());
            passwordResetTokenRepository.save(token);

            log.info("Going to persist a reset password for {}", request.getEmail());
            user.setPasswordHash(requestBuilder.encodePassword(request.getNewPassword()));
            userRepository.save(user);

            log.info("Response after resetting the password for email : {} {}", request.getEmail(), JsonUtility.toJson(user.getId()));
            return responseProcessor.processPasswordResetResponse();

        } catch (Exception e) {
            log.info("An exception occurred when attempting to reset the password for {} . Exception: ", request.getEmail(), e);
            return responseProcessor.failedPasswordReset(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.PASSWORD_RESET_FAILED);
        }
    }

    private String rejectUnusablePassword(String password) {
        if (!StringUtils.hasText(password)) {
            return ResponseMessage.NO_NEW_PASSWORD_FOUND;
        }
        if (password.length() < minPasswordLength) {
            return String.format(ResponseMessage.PASSWORD_TOO_SHORT, minPasswordLength);
        }
        return null;
    }

    private void discardOutstandingResetTokens(User user) {
        List<PasswordResetToken> outstanding = passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(user.getId());
        if (outstanding.isEmpty()) {
            return;
        }
        LocalDateTime spentAt = LocalDateTime.now();
        outstanding.forEach(token -> token.setUsedAt(spentAt));
        passwordResetTokenRepository.saveAll(outstanding);
        log.info("Discarded {} outstanding reset code(s) for {}", outstanding.size(), user.getId());
    }
}
