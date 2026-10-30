package com.greensnow.worker.model.request;

import com.greensnow.worker.model.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password")
public class RegistrationRequest {

    private UserType userType;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String password;
}
