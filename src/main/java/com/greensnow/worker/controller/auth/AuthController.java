package com.greensnow.worker.controller.auth;

import com.greensnow.worker.model.request.ChangePasswordRequest;
import com.greensnow.worker.model.request.ForgotPasswordRequest;
import com.greensnow.worker.model.request.LoginRequest;
import com.greensnow.worker.model.request.RegistrationRequest;
import com.greensnow.worker.model.request.ResetPasswordRequest;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.model.response.LoginResponse;
import com.greensnow.worker.model.response.RegistrationResponse;
import com.greensnow.worker.service.auth.AuthService;
import com.greensnow.worker.util.JsonUtility;
import com.greensnow.worker.util.MaskUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("register")
    public RegistrationResponse register(@RequestBody RegistrationRequest request) {
        RegistrationResponse registrationResponse = authService.registerUser(request);
        log.info("Registration request for {} response {}", request.getEmail(), JsonUtility.toJson(registrationResponse));
        return registrationResponse;
    }

    @PostMapping("login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.login(request);
        log.info("Login request for {} response {}", request.getEmail(),
                MaskUtility.toMaskedJson(loginResponse, "accessToken"));
        return loginResponse;
    }

    @PostMapping("logout")
    public GeneralResponse logout(@AuthenticationPrincipal String userId) {
        GeneralResponse logoutResponse = authService.logout(userId);
        log.info("Logout request for {} response {}", userId, JsonUtility.toJson(logoutResponse));
        return logoutResponse;
    }

    @PostMapping("change-password")
    public GeneralResponse changePassword(@AuthenticationPrincipal String userId,
                                          @RequestBody ChangePasswordRequest request) {
        GeneralResponse changePasswordResponse = authService.changePassword(userId, request);
        log.info("Change password request for {} response {}", userId, JsonUtility.toJson(changePasswordResponse));
        return changePasswordResponse;
    }

    @PostMapping("forgot-password")
    public GeneralResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        GeneralResponse forgotPasswordResponse = authService.forgotPassword(request);
        log.info("Forgot password request for {} response {}", request.getEmail(), JsonUtility.toJson(forgotPasswordResponse));
        return forgotPasswordResponse;
    }

    @PostMapping("reset-password")
    public GeneralResponse resetPassword(@RequestBody ResetPasswordRequest request) {
        GeneralResponse resetPasswordResponse = authService.resetPassword(request);
        log.info("Reset password request for {} response {}", request.getEmail(), JsonUtility.toJson(resetPasswordResponse));
        return resetPasswordResponse;
    }
}
