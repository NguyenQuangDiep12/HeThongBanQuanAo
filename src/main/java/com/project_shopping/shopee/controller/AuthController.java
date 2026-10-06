package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.AuthResponse;
import com.project_shopping.shopee.dto.ApiDtos.LoginRequest;
import com.project_shopping.shopee.dto.ApiDtos.RegisterRequest;
import com.project_shopping.shopee.dto.ApiDtos.UserResponse;
import com.project_shopping.shopee.dto.ApiDtos.ProfileUpdateRequest;
import com.project_shopping.shopee.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

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

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails principal) {
        return authService.profile(principal.getUsername());
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal UserDetails principal,
                                      @Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(principal.getUsername(), request);
    }
}
