package com.ayitech.worker.service.auth;

import com.ayitech.worker.model.constant.ResponseCode;
import com.ayitech.worker.model.constant.ResponseMessage;
import com.ayitech.worker.model.entity.User;
import com.ayitech.worker.model.response.GeneralResponse;
import com.ayitech.worker.model.response.LoginResponse;
import com.ayitech.worker.model.response.RegistrationResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthResponseProcessor {

    public RegistrationResponse processRegistrationResponse(User user) {
        if (Objects.isNull(user)) {
            return failedRegistration(ResponseCode.CODE_GENERAL_FAILURE, ResponseMessage.REGISTRATION_FAILED);
        }
        return RegistrationResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.REGISTRATION_SUCCESSFUL)
                .userId(String.valueOf(user.getId()))
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(composeFullName(user))
                .userType(user.getUserType())
                .build();
    }

    public RegistrationResponse failedRegistration(String responseCode, String responseMessage) {
        return RegistrationResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public LoginResponse processLoginResponse(User user, String accessToken, long expiresIn) {
        return LoginResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.LOGIN_SUCCESSFUL)
                .userId(String.valueOf(user.getId()))
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(composeFullName(user))
                .userType(user.getUserType())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }

    public LoginResponse failedLogin(String responseCode, String responseMessage) {
        return LoginResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public GeneralResponse processLogoutResponse() {
        return GeneralResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.LOGOUT_SUCCESSFUL)
                .build();
    }

    public GeneralResponse failedLogout(String responseCode, String responseMessage) {
        return GeneralResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public GeneralResponse processPasswordChangeResponse() {
        return GeneralResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.PASSWORD_CHANGE_SUCCESSFUL)
                .build();
    }

    public GeneralResponse failedPasswordChange(String responseCode, String responseMessage) {
        return GeneralResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    // the one success shape the forgot password route ever answers with, whether or not an
    // account was found - see ResponseMessage.PASSWORD_RESET_REQUESTED
    public GeneralResponse processForgotPasswordResponse() {
        return GeneralResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.PASSWORD_RESET_REQUESTED)
                .build();
    }

    public GeneralResponse failedForgotPassword(String responseCode, String responseMessage) {
        return GeneralResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    public GeneralResponse processPasswordResetResponse() {
        return GeneralResponse.builder()
                .responseCode(ResponseCode.CODE_01)
                .responseMessage(ResponseMessage.PASSWORD_RESET_SUCCESSFUL)
                .build();
    }

    public GeneralResponse failedPasswordReset(String responseCode, String responseMessage) {
        return GeneralResponse.builder()
                .responseCode(responseCode)
                .responseMessage(responseMessage)
                .build();
    }

    private String composeFullName(User user) {
        return Stream.of(user.getFirstName(), user.getLastName())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }
}
