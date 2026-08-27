package com.zidtech.auth.config;

import com.zidtech.auth.domain.entity.AppUser;
import com.zidtech.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminDataInitializer adminDataInitializer;

    @Test
    @DisplayName("Should create default admin user when none exists")
    void initAdminUser_NotExists_SavesAdmin() throws Exception {
        when(userRepository.existsByEmail("admin@zidtech.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_admin_password");

        CommandLineRunner runner = adminDataInitializer.initAdminUser();
        runner.run();

        verify(userRepository, times(1)).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Should skip creating admin user when one already exists")
    void initAdminUser_AlreadyExists_DoesNotSave() throws Exception {
        when(userRepository.existsByEmail("admin@zidtech.com")).thenReturn(true);

        CommandLineRunner runner = adminDataInitializer.initAdminUser();
        runner.run();

        verify(userRepository, never()).save(any(AppUser.class));
    }
}