package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.NotificationResponse;
import com.project_shopping.shopee.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications(
            @AuthenticationPrincipal UserDetails user) {

        return notificationService.list(user.getUsername());
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {

        return notificationService.markRead(
                user.getUsername(),
                id
        );
    }
}