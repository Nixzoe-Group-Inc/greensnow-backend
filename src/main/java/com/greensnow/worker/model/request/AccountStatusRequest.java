package com.greensnow.worker.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// the reason an account is being suspended. whether it is a suspension or a restoration is the
// route, never a field - as with turning an applicant down, a reason is required because the
// person on the other side has to be able to read what they did
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountStatusRequest {

    private String reason;
}
