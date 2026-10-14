package com.project_shopping.shopee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Fails fast outside local/test profiles when production-only credentials are unsafe or absent. */
@Component
@Profile("!dev & !test")
public class ProductionConfigurationGuard implements ApplicationRunner {

    private final String databaseUrl;
    private final String databaseUsername;
    private final String databasePassword;
    private final String jwtSecret;
    private final String adminEmail;
    private final String adminPassword;

    public ProductionConfigurationGuard(
            @Value("${spring.datasource.url}") String databaseUrl,
            @Value("${spring.datasource.username}") String databaseUsername,
            @Value("${spring.datasource.password}") String databasePassword,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.bootstrap-admin.email:}") String adminEmail,
            @Value("${app.bootstrap-admin.password:}") String adminPassword) {
        this.databaseUrl = databaseUrl;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.jwtSecret = jwtSecret;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    void validate() {
        if (databaseUsername == null || databaseUsername.isBlank()
                || "root".equalsIgnoreCase(databaseUsername.trim())) {
            throw new IllegalStateException("Production must use a dedicated, non-root database account.");
        }
        if (databasePassword == null || databasePassword.isBlank()) {
            throw new IllegalStateException("DB_PASSWORD must be set outside dev/test profiles.");
        }
        if (databaseUrl == null || !databaseUrl.toLowerCase(Locale.ROOT)
                .matches("(?s).*([?&])sslmode=verify_identity([&;].*)?")) {
            throw new IllegalStateException(
                    "Production DB_URL must enable MySQL sslMode=VERIFY_IDENTITY with a trusted server certificate.");
        }
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Production JWT_SECRET must contain at least 32 bytes.");
        }
        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.length() < 8
                || adminPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Production must provide ADMIN_EMAIL and an ADMIN_PASSWORD of 8–72 UTF-8 bytes.");
        }
    }
}
