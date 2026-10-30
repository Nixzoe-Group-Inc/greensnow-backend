package com.greensnow.worker;

import com.greensnow.worker.model.enums.UserType;
import com.greensnow.worker.model.request.LoginRequest;
import com.greensnow.worker.model.request.RegistrationRequest;
import com.greensnow.worker.model.response.LoginResponse;
import com.greensnow.worker.model.response.RegistrationResponse;
import com.greensnow.worker.service.auth.AuthService;
import com.greensnow.worker.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthSmokeTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Test
    void aRegisteredWorkerCanSignInAndTheTokenNamesThem() {
        RegistrationRequest registration = RegistrationRequest.builder()
                .userType(UserType.EMPLOYEE)
                .email("kwame@worker.test")
                .phoneNumber("233200000001")
                .firstName("Kwame")
                .lastName("Mensah")
                .password("plumber-pass-1")
                .build();

        RegistrationResponse registered = authService.registerUser(registration);
        assertThat(registered.getResponseCode()).isEqualTo("01");
        assertThat(registered.getUserId()).isNotBlank();
        assertThat(registered.getFullName()).isEqualTo("Kwame Mensah");

        // the same email a second time is refused rather than creating a duplicate worker
        assertThat(authService.registerUser(registration).getResponseCode()).isEqualTo("109");

        LoginResponse login = authService.login(LoginRequest.builder()
                .email("KWAME@worker.test").password("plumber-pass-1").build());
        assertThat(login.getResponseCode()).isEqualTo("01");
        assertThat(login.getTokenType()).isEqualTo("Bearer");
        assertThat(login.getUserType()).isEqualTo(UserType.EMPLOYEE);
        assertThat(login.getAccessToken()).isNotBlank();

        // the token the filter will parse names the user it was issued for
        Claims claims = jwtService.parseClaims(login.getAccessToken());
        assertThat(claims.getSubject()).isEqualTo(registered.getUserId());
        assertThat(claims.get("userType", String.class)).isEqualTo("EMPLOYEE");

        // a wrong password says exactly what an unknown email says
        assertThat(authService.login(LoginRequest.builder()
                .email("kwame@worker.test").password("not-the-password").build())
                .getResponseCode()).isEqualTo("105");
        assertThat(authService.login(LoginRequest.builder()
                .email("nobody@worker.test").password("plumber-pass-1").build())
                .getResponseCode()).isEqualTo("105");

        assertThat(authService.logout(registered.getUserId()).getResponseCode()).isEqualTo("01");
    }
}
