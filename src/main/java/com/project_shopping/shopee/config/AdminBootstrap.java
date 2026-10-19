package com.project_shopping.shopee.config;

import com.project_shopping.shopee.model.User;
import com.project_shopping.shopee.model.enums.Role;
import com.project_shopping.shopee.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String adminEmail,
            @Value("${app.bootstrap-admin.password:}") String adminPassword) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }

        validateAdminConfiguration();

        String normalizedEmail = normalizeEmail(adminEmail);

        var existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail);

        if (existingUser.isPresent()) {
            validateExistingUser(existingUser.get());
            return;
        }

        createAdmin(normalizedEmail);
    }

    private void validateAdminConfiguration() {

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Cần cấu hình đồng thời ADMIN_EMAIL và ADMIN_PASSWORD để khởi tạo tài khoản quản trị."
            );
        }
        int passwordBytes = adminPassword.getBytes(StandardCharsets.UTF_8).length;
        if (adminPassword.length() < 8 || passwordBytes > 72) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD phải có ít nhất 8 ký tự và không vượt quá 72 byte UTF-8."
            );
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateExistingUser(User user) {

        if (user.getRole() != Role.ADMIN) {
            throw new IllegalStateException(
                    "Email ADMIN_EMAIL đã được một tài khoản không có quyền quản trị sử dụng."
            );
        }
    }

    private void createAdmin(String email) {

        User admin = new User(
                "Quản trị viên",
                email,
                passwordEncoder.encode(adminPassword),
                Role.ADMIN,
                null
        );

        userRepository.save(admin);
    }
}
