package com.project_shopping.shopee.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationGuardTest {

    @Test
    void acceptsProductionDatabaseAndSecretConfigurationWhenTlsIdentityIsVerified() {
        ProductionConfigurationGuard guard = new ProductionConfigurationGuard(
                "jdbc:mysql://db.example.test/store?sslMode=VERIFY_IDENTITY",
                "shopee_app",
                "db-password",
                "a-production-jwt-secret-with-at-least-32-bytes",
                "admin@example.test",
                "strong-admin-password");

        assertThatCode(guard::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsRootDatabaseCredentialsEvenWhenOtherSettingsAreValid() {
        ProductionConfigurationGuard guard = new ProductionConfigurationGuard(
                "jdbc:mysql://db.example.test/store?sslMode=VERIFY_IDENTITY",
                "root",
                "db-password",
                "a-production-jwt-secret-with-at-least-32-bytes",
                "admin@example.test",
                "strong-admin-password");

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-root");
    }

    @Test
    void rejectsDatabaseConnectionsWithoutServerIdentityVerification() {
        ProductionConfigurationGuard guard = new ProductionConfigurationGuard(
                "jdbc:mysql://db.example.test/store?sslMode=REQUIRED",
                "shopee_app",
                "db-password",
                "a-production-jwt-secret-with-at-least-32-bytes",
                "admin@example.test",
                "strong-admin-password");

        assertThatThrownBy(guard::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VERIFY_IDENTITY");
    }
}
