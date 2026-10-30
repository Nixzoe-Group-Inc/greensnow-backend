package com.ayitech.worker.security;

import com.ayitech.worker.model.constant.ResponseMessage;
import com.ayitech.worker.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String USER_TYPE_CLAIM = "userType";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String accessToken = bearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));

        // no token is not a rejection here - a public route still has to be reachable,
        // so the entry point is left to answer the ones that do require authentication
        if (!StringUtils.hasText(accessToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication(claims, request));
        } catch (Exception e) {
            log.info("Refused a request to {} because the access token is invalid . Exception: ", request.getRequestURI(), e);
            SecurityContextHolder.clearContext();
            JwtAuthenticationEntryPoint.writeUnauthorised(response, ResponseMessage.INVALID_ACCESS_TOKEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken authentication(Claims claims, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                claims.getSubject(), null, authorities(claims));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }

    private List<SimpleGrantedAuthority> authorities(Claims claims) {
        String userType = claims.get(USER_TYPE_CLAIM, String.class);
        return StringUtils.hasText(userType)
                ? List.of(new SimpleGrantedAuthority(ROLE_PREFIX + userType))
                : List.of();
    }

    private String bearerToken(String authorization) {
        return StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length()).trim()
                : null;
    }
}
