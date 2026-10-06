package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.AdminUserResponse;
import com.project_shopping.shopee.service.AdminUserService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService users;
    public AdminUserController(AdminUserService users) { this.users = users; }
    @GetMapping public List<AdminUserResponse> list() { return users.list(); }
    @PutMapping("/{id}/active")
    public AdminUserResponse setActive(@AuthenticationPrincipal UserDetails admin, @PathVariable Long id,
                                       @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid ActiveRequest request) {
        return users.setActive(id, request.active(), admin.getUsername());
    }
    public record ActiveRequest(@NotNull Boolean active) { }
}
