package com.greensnow.worker.model.response;

import com.greensnow.worker.model.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String responseCode;
    private String responseMessage;
    private String userId;
    private String email;
    private String phoneNumber;
    private String fullName;
    private UserType userType;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
}
