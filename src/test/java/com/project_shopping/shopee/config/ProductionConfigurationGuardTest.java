package com.project_shopping.shopee.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationGuardTest {

    private static final Map<String, Object> PRODUCTION_SETTINGS = Map.of(
            "spring.datasource.url", "jdbc:mysql://db.example.test/store?sslMode=VERIFY_IDENTITY",
            "spring.datasource.username", "shopee_app",
            "spring.datasource.password", "db-password",
            "app.jwt.secret", "a-production-jwt-secret-with-at-least-32-bytes");

    @Test
    void acceptsSecureProductionConfigurationWithoutPermanentAdminBootstrapCredentials() {
        MockEnvironment environment = productionEnvironment(Map.of());

        assertThatCode(() -> new ProductionConfigurationGuard().postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsSecureProductionConfigurationWithOneTimeAdminBootstrapCredentials() {
        MockEnvironment environment = productionEnvironment(Map.of(
                "app.bootstrap-admin.email", "admin@example.test",
                "app.bootstrap-admin.password", "strong-admin-password"));

        assertThatCode(() -> new ProductionConfigurationGuard().postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsRootDatabaseCredentialsBeforeContextStartup() {
        MockEnvironment environment = productionEnvironment(Map.of("spring.datasource.username", "root"));

        assertThatThrownBy(() -> new ProductionConfigurationGuard().postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-root");
    }

    @Test
    void rejectsDatabaseConnectionsWithoutServerIdentityVerification() {
        MockEnvironment environment = productionEnvironment(Map.of(
                "spring.datasource.url", "jdbc:mysql://db.example.test/store?sslMode=REQUIRED"));

        assertThatThrownBy(() -> new ProductionConfigurationGuard().postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VERIFY_IDENTITY");
    }

    @Test
    void requiresAdminBootstrapCredentialsToBePaired() {
        MockEnvironment environment = productionEnvironment(Map.of(
                "app.bootstrap-admin.email", "admin@example.test"));

        assertThatThrownBy(() -> new ProductionConfigurationGuard().postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configured together");
    }

    private MockEnvironment productionEnvironment(Map<String, Object> overrides) {
        Map<String, Object> settings = new java.util.HashMap<>(PRODUCTION_SETTINGS);
        settings.putAll(overrides);
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", settings));
        return environment;
    }
}
