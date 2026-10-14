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

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwords,
            JwtService jwt
    ) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.dummyPasswordHash = passwords.encode("non-existent-account-dummy-password");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        validatePassword(request.password());

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

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            passwords.matches("non-existent-account-dummy-password", dummyPasswordHash);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác.");
        }

        var userRecord = users.findByEmailIgnoreCase(request.email().trim());
        if (userRecord.isEmpty()) {
            passwords.matches(request.password(), dummyPasswordHash);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác.");
        }
        User user = userRecord.get();

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

    private void validatePassword(String password) {
        int utf8Length = password.getBytes(StandardCharsets.UTF_8).length;
        if (password.length() < 8 || utf8Length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mật khẩu phải có ít nhất 8 ký tự và không vượt quá 72 byte UTF-8."
            );
        }
    }
}
