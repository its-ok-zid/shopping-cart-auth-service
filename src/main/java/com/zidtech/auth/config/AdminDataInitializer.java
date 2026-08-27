package com.zidtech.auth.config;

import com.zidtech.auth.domain.entity.AppUser;
import com.zidtech.auth.domain.enums.UserRole;
import com.zidtech.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminDataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initAdminUser() {
        return args -> {
            String adminEmail = "admin@zidtech.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                AppUser admin = AppUser.builder()
                        .firstName("System")
                        .lastName("Admin")
                        .email(adminEmail)
                        .passwordHash(passwordEncoder.encode("AdminSecurePassword123!"))
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(admin);
                log.info("Default system administrator seeded successfully: {}", adminEmail);
            }
        };
    }
}