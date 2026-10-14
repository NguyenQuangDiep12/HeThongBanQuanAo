package com.project_shopping.shopee.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_shopping.shopee.dto.ApiDtos.ErrorResponse;
import com.project_shopping.shopee.model.User;
import com.project_shopping.shopee.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private static final System.Logger LOGGER = System.getLogger(JwtAuthenticationFilter.class.getName());

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            ObjectMapper objectMapper) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return (path.equals("/api/auth/register") || path.equals("/api/auth/login"))
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || ("GET".equalsIgnoreCase(request.getMethod())
                    && (path.startsWith("/api/products") || path.startsWith("/api/categories")))
                || path.startsWith("/api/bank-accounts/active");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                authenticateUser(token, request);
            } catch (DataAccessException exception) {
                SecurityContextHolder.clearContext();
                LOGGER.log(System.Logger.Level.WARNING,
                        "JWT user lookup failed because the database is unavailable.", exception);
                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                        Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "Dịch vụ tạm thời không khả dụng",
                        "Không thể xác thực phiên đăng nhập lúc này. Vui lòng thử lại sau."));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {

        String authorizationHeader =
                request.getHeader("Authorization");

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            return null;
        }

        return authorizationHeader.substring(7);
    }

    private void authenticateUser(
            String token,
            HttpServletRequest request) {

        try {
            String email = jwtService.extractSubject(token);

            userRepository.findByEmailIgnoreCase(email)
                    .filter(User::isActive)
                    .ifPresent(user -> setAuthentication(user, request));

        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(
            User user,
            HttpServletRequest request) {

        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }
}
