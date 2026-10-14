package com.project_shopping.shopee.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_shopping.shopee.controller.ApiExceptionHandler;
import com.project_shopping.shopee.dto.ApiDtos.ErrorResponse;
import com.project_shopping.shopee.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper) throws Exception {

        configureSecurity(http);
        configureAuthorization(http, objectMapper);
        configureJwtFilter(http, jwtAuthenticationFilter);

        return http.build();
    }

    private void configureSecurity(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
    }

    private void configureAuthorization(
            HttpSecurity http,
            ObjectMapper objectMapper) throws Exception {
        http.exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, exception) -> writeError(
                response,
                objectMapper,
                HttpStatus.UNAUTHORIZED,
                "Bạn cần đăng nhập để sử dụng chức năng này."
            ))
            .accessDeniedHandler((request, response, exception) -> {
                String message = request.getRequestURI().startsWith("/api/admin/")
                    ? "Bạn không có quyền truy cập chức năng quản trị."
                    : "Bạn không có quyền thực hiện thao tác này.";
                writeError(response, objectMapper, HttpStatus.FORBIDDEN, message);
            })
        );

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
            .requestMatchers("/api/auth/me").hasAnyRole("USER", "ADMIN")

            .requestMatchers(
                HttpMethod.GET,
                "/api/products/**",
                "/api/categories/**",
                "/api/bank-accounts/active"
            ).permitAll()

            .requestMatchers("/api/admin/**")
            .hasRole("ADMIN")

            .requestMatchers(
                "/api/cart/**",
                "/api/orders/**",
                "/api/notifications/**",
                "/api/wishlist/**"
            ).hasRole("USER")

            .anyRequest()
            .authenticated()
        );
    }

    private void writeError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        objectMapper.writeValue(
            response.getWriter(),
            new ErrorResponse(
                Instant.now(),
                status.value(),
                ApiExceptionHandler.localizedStatus(status),
                message
            )
        );
    }

    private void configureJwtFilter(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        http.addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );
    }
}
