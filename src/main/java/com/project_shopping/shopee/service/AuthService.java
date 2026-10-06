package com.project_shopping.shopee.service;

import com.project_shopping.shopee.dto.ApiDtos.*;
import com.project_shopping.shopee.model.User;
import com.project_shopping.shopee.model.enums.Role;
import com.project_shopping.shopee.repository.UserRepository;
import com.project_shopping.shopee.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwords,
            JwtService jwt
    ) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase();

        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email này đã được đăng ký."
            );
        }

        User user = users.save(
                new User(
                        request.fullName().trim(),
                        email,
                        passwords.encode(request.password()),
                        Role.USER,
                        request.phone()
                )
        );

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        User user = users
                .findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Email hoặc mật khẩu không chính xác."
                        )
                );

        if (!passwords.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Email hoặc mật khẩu không chính xác."
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ cửa hàng."
            );
        }

        return new AuthResponse(
                jwt.createToken(user),
                user.getId(),
                user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse profile(String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản người dùng."));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, ProfileUpdateRequest request) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản người dùng."));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone() == null ? null : request.phone().trim());
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getPhone());
    }
}
