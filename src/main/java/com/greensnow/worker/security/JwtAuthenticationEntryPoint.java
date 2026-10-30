package com.greensnow.worker.security;

import com.greensnow.worker.model.constant.ResponseCode;
import com.greensnow.worker.model.constant.ResponseMessage;
import com.greensnow.worker.model.response.GeneralResponse;
import com.greensnow.worker.util.JsonUtility;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        writeUnauthorised(response, ResponseMessage.NO_ACCESS_TOKEN_FOUND);
    }

    // the one place an unauthorised body is written, so the filter and spring security cannot drift
    public static void writeUnauthorised(HttpServletResponse response, String responseMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonUtility.toJson(GeneralResponse.builder()
                .responseCode(ResponseCode.CODE_UNAUTHORISED)
                .responseMessage(responseMessage)
                .build()));
    }
}
