package com.project_shopping.shopee.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Validates production settings before Spring creates the datasource or Hibernate schema. */
public final class ProductionConfigurationGuard implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        // Run after ConfigDataEnvironmentPostProcessor has loaded application.properties and profiles.
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            return;
        }
        validate(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"),
                environment.getProperty("app.jwt.secret"),
                environment.getProperty("app.bootstrap-admin.email", ""),
                environment.getProperty("app.bootstrap-admin.password", ""));
    }

    static void validate(String databaseUrl, String databaseUsername, String databasePassword,
                         String jwtSecret, String adminEmail, String adminPassword) {
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

        // Bootstrap credentials are a one-time provisioning option, not a permanent runtime secret.
        boolean emailConfigured = adminEmail != null && !adminEmail.isBlank();
        boolean passwordConfigured = adminPassword != null && !adminPassword.isBlank();
        if (emailConfigured != passwordConfigured) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL and ADMIN_PASSWORD must be configured together when bootstrapping an admin.");
        }
        if (passwordConfigured && (adminPassword.length() < 8
                || adminPassword.getBytes(StandardCharsets.UTF_8).length > 72)) {
            throw new IllegalStateException("ADMIN_PASSWORD must contain 8–72 UTF-8 bytes when configured.");
        }
    }
}
