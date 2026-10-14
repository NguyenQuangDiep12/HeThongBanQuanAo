package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.AuthResponse;
import com.project_shopping.shopee.dto.ApiDtos.LoginRequest;
import com.project_shopping.shopee.dto.ApiDtos.RegisterRequest;
import com.project_shopping.shopee.dto.ApiDtos.UserResponse;
import com.project_shopping.shopee.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request) {

        return authService.login(request);
    }
}