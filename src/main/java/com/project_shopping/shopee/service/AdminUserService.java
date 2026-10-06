package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.ApiDtos.AdminUserResponse;
import com.project_shopping.shopee.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminUserService {
    private final UserRepository users;
    public AdminUserService(UserRepository users) { this.users = users; }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return users.findAllByOrderByCreatedAtDesc().stream().map(user -> new AdminUserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), user.isActive(), user.getCreatedAt()
        )).toList();
    }

    @Transactional
    public AdminUserResponse setActive(Long id, boolean active, String adminEmail) {
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (user.getRole() != com.project_shopping.shopee.model.enums.Role.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ có thể thay đổi trạng thái tài khoản khách hàng.");
        }
        if (!active && user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn không thể vô hiệu hóa tài khoản đang đăng nhập.");
        }
        user.setActive(active);
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), user.isActive(), user.getCreatedAt());
    }

    public record ActiveRequest(boolean active) { }
}
