package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.ApiDtos.NotificationResponse;
import com.project_shopping.shopee.repository.NotificationRepository;
import com.project_shopping.shopee.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(
            NotificationRepository notifications,
            UserRepository users
    ) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email) {

        Long userId = userId(email);

        return notifications
                .findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n ->
                        new NotificationResponse(
                                n.getId(),
                                n.getOrder().getId(),
                                n.getTitle(),
                                n.getMessage(),
                                n.getType(),
                                n.isRead(),
                                n.getCreatedAt()
                        )
                )
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(
            String email,
            Long id
    ) {

        var n = notifications
                .findByIdAndUserId(
                        id,
                        userId(email)
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy thông báo."
                        )
                );

        n.setRead(true);

        return new NotificationResponse(
                n.getId(),
                n.getOrder().getId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.isRead(),
                n.getCreatedAt()
        );
    }

    private Long userId(String email) {

        return users
                .findByEmailIgnoreCase(email)
                .map(u -> u.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Không tìm thấy tài khoản người dùng."
                        )
                );
    }
}
