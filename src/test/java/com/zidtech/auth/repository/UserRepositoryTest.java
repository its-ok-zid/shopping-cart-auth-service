package com.zidtech.auth.repository;

import com.zidtech.auth.config.JpaConfig;
import com.zidtech.auth.domain.entity.AppUser;
import com.zidtech.auth.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Ensure a clean slate
        AppUser user = AppUser.builder()
                .firstName("Zidan")
                .lastName("Ali")
                .email("zidan@zidtech.com")
                .passwordHash("hashed_password_123")
                .role(UserRole.CUSTOMER)
                .build();
        userRepository.save(user);
    }

    @Test
    @DisplayName("Should successfully find an active user by exact email")
    void shouldFindByEmail() {
        Optional<AppUser> found = userRepository.findByEmail("zidan@zidtech.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Zidan");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return true if email exists to prevent duplicates")
    void shouldReturnTrueIfEmailExists() {
        boolean exists = userRepository.existsByEmail("zidan@zidtech.com");
        assertThat(exists).isTrue();
    }
}