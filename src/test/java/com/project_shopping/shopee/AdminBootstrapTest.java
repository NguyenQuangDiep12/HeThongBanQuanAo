package com.project_shopping.shopee;

import com.project_shopping.shopee.config.AdminBootstrap;
import com.project_shopping.shopee.model.User;
import com.project_shopping.shopee.model.enums.Role;
import com.project_shopping.shopee.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    @Test
    void doesNotReactivateAnAdministratorThatWasDisabled() throws Exception {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User admin = new User("Admin", "admin@example.test", "encoded", Role.ADMIN, null);
        admin.setActive(false);
        when(users.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.of(admin));

        AdminBootstrap bootstrap = new AdminBootstrap(
                users, passwordEncoder, "admin@example.test", "secure-password-1");
        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        assertThat(admin.isActive()).isFalse();
        verify(users, never()).save(admin);
        verify(passwordEncoder, never()).encode("secure-password-1");
    }
}
