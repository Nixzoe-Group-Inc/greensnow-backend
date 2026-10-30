package com.ayitech.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"currentPassword", "newPassword"})
public class ChangePasswordRequest {

    private String currentPassword;

    private String newPassword;
}
