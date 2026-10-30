package com.ayitech.worker.model.response;

import com.ayitech.worker.model.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// one account as the admin reads it. the password hash is not here and never should be - the
// admin administers accounts, which is not the same as being able to use them
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetail {

    private String userId;
    private String email;
    private String phoneNumber;
    private String fullName;
    private UserType userType;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private Boolean active;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    // whether the account has got as far as a profile, and whether the platform has verified it.
    // an account with no profile has never posted work or been matched to any
    private Boolean hasProfile;
    private Boolean verified;
}
