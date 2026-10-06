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
            // The configured bootstrap admin must remain able to administer accounts.
            // Existing rows may have received the new active column as FALSE during schema update.
            if (!existingUser.get().isActive()) {
                existingUser.get().setActive(true);
            }
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
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
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
