package com.greensnow.worker.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // forgot and reset password are public by necessity - someone who cannot sign in is exactly
    // who needs them. change-password is deliberately NOT here: it is for a signed in user and
    // the current password is only half of what authorises it
    private static final String[] PUBLIC_ROUTES = {
            "/auth/register",
            "/auth/login",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        // the whole employee surface answers off the signed in worker's own id,
                        // so an employer's token reaching it would read a profile that is not
                        // theirs. gated at the route rather than checked in every method
                        .requestMatchers("/employee/**").hasRole("EMPLOYEE")
                        // and the same the other way. every employer route resolves the company
                        // from the signed in user, so a worker's token here would be answered
                        // with another party's jobs and the people who applied for them
                        .requestMatchers("/employer/**").hasRole("EMPLOYER")
                        // the admin surface is the one place a record belonging to somebody else
                        // is meant to be reachable - approving another company's job, suspending
                        // another person's account. nothing inside it checks ownership, because
                        // ownership is not what authorises it, so this line is the whole gate
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
