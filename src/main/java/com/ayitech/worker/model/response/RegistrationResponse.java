package com.ayitech.worker.model.response;

import com.ayitech.worker.model.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationResponse {

    private String responseCode;
    private String responseMessage;
    private String userId;
    private String email;
    private String phoneNumber;
    private String fullName;
    private UserType userType;
}
