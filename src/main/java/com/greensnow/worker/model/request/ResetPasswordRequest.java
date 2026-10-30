package com.greensnow.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"resetCode", "newPassword"})
public class ResetPasswordRequest {

    private String email;

    private String resetCode;

    private String newPassword;
}
