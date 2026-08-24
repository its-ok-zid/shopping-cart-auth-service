package com.zidtech.auth.service;

import com.zidtech.auth.domain.entity.AppUser;
import com.zidtech.auth.domain.entity.RefreshToken;
import com.zidtech.auth.domain.enums.UserRole;
import com.zidtech.auth.dto.AuthResponse;
import com.zidtech.auth.dto.AuthResult;
import com.zidtech.auth.dto.LoginRequest;
import com.zidtech.auth.dto.RegisterRequest;
import com.zidtech.auth.exception.DuplicateResourceException;
import com.zidtech.auth.exception.InvalidCredentialsException;
import com.zidtech.auth.repository.RefreshTokenRepository;
import com.zidtech.auth.repository.UserRepository;
import com.zidtech.auth.service.AuthService;
import com.zidtech.security.IssuedToken;
import com.zidtech.security.JwtTokenService;
import com.zidtech.security.SecurityPrincipal;
import com.zidtech.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    
    // These beans are auto-magically provided by your custom common-security GitHub Package!
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    @Transactional // ACID guarantee: If token save fails, user creation rolls back!
    public AuthResult register(RegisterRequest request) {
        log.info("Attempting to register new user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        AppUser newUser = AppUser.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                // Never store plain text passwords!
                .passwordHash(passwordEncoder.encode(request.password())) 
                .role(UserRole.CUSTOMER) // Default role for new signups
                .build();

        AppUser savedUser = userRepository.save(newUser);
        log.info("Successfully registered user ID: {}", savedUser.getPublicId());

        return generateTokens(savedUser);
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password"); // Vague message prevents user enumeration
        }

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        log.info("Successful login for user ID: {}", user.getPublicId());
        return generateTokens(user);
    }

    /**
     * Helper method to generate the JWTs and persist the refresh token hash.
     */
    private AuthResult generateTokens(AppUser user) {
        // 1. Create the application-neutral principal expected by your security library
        SecurityPrincipal principal = new SecurityPrincipal(
                user.getPublicId().toString(),
                user.getEmail(),
                Set.of(user.getRole().name())
        );

        // 2. Issue the Access Token (Stateless)
        IssuedToken accessToken = jwtTokenService.issueAccessToken(principal);

        // 3. Issue the Refresh Token (Stateful)
        IssuedToken refreshToken = jwtTokenService.issueRefreshToken(principal, null);

        // 4. Hash the refresh token using your custom library's TokenHasher
        String hashedRefreshToken = TokenHasher.sha256(refreshToken.value());

        // 5. Persist the hashed refresh token
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .tokenId(refreshToken.tokenId())
                .familyId(refreshToken.familyId())
                .hashedValue(hashedRefreshToken)
                .expiresAt(refreshToken.expiresAt())
                .user(user)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 6. Build the API Response DTO
        AuthResponse response = new AuthResponse(
                accessToken.value(),
                user.getPublicId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );

        return new AuthResult(response, refreshToken.value());
    }
}