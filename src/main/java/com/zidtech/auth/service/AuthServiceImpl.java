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
import com.zidtech.security.IssuedToken;
import com.zidtech.security.JwtTokenService;
import com.zidtech.security.SecurityPrincipal;
import com.zidtech.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        // SECURITY RULE: Public users can register as CUSTOMER or SELLER only.
        // Preventing unauthorized escalation to ADMIN role.
        UserRole assignedRole = UserRole.CUSTOMER;
        if (request.role() != null) {
            if (request.role() == UserRole.ADMIN) {
                throw new InvalidCredentialsException("Unauthorized role assignment");
            }
            assignedRole = request.role();
        }

        AppUser newUser = AppUser.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(assignedRole)
                .build();

        AppUser savedUser = userRepository.save(newUser);
        log.info("Successfully registered user ID: {} with role: {}", savedUser.getPublicId(), assignedRole);
        return generateTokens(savedUser);
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        AppUser user = userRepository.findByEmail(request.email()).orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

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
        SecurityPrincipal principal = new SecurityPrincipal(user.getPublicId().toString(), user.getEmail(), Set.of(user.getRole().name()));

        // 2. Issue the Access Token (Stateless)
        IssuedToken accessToken = jwtTokenService.issueAccessToken(principal);

        // 3. Issue the Refresh Token (Stateful)
        IssuedToken refreshToken = jwtTokenService.issueRefreshToken(principal, null);

        // 4. Hash the refresh token using your custom library's TokenHasher
        String hashedRefreshToken = TokenHasher.sha256(refreshToken.value());

        // 5. Persist the hashed refresh token
        RefreshToken refreshTokenEntity = RefreshToken.builder().tokenId(refreshToken.tokenId()).familyId(refreshToken.familyId()).hashedValue(hashedRefreshToken).expiresAt(refreshToken.expiresAt()).user(user).build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 6. Build the API Response DTO
        AuthResponse response = new AuthResponse(accessToken.value(), user.getPublicId(), user.getFirstName(), user.getLastName(), user.getRole().name());

        return new AuthResult(response, refreshToken.value());
    }

    @Override
    @Transactional
    public AuthResult refreshToken(String rawRefreshToken) {
        log.info("Attempting to refresh session token");

        // 1. Parse the token using your custom security library (this validates signature & expiration)
        var parsedToken = jwtTokenService.parse(rawRefreshToken, com.zidtech.security.TokenType.REFRESH);

        // 2. Hash the token to look it up securely in the DB
        String hashedToken = TokenHasher.sha256(rawRefreshToken);
        RefreshToken tokenEntity = refreshTokenRepository.findByHashedValue(hashedToken).orElseThrow(() -> new InvalidCredentialsException("Refresh token not found"));

        // 3. SECURITY BREACH DETECTION: If the token is already revoked, someone is trying to replay a stolen token!
        if (tokenEntity.isRevoked()) {
            log.warn("SECURITY ALERT: Attempted reuse of revoked token family: {}", parsedToken.familyId());
            List<RefreshToken> familyTokens = refreshTokenRepository.findAllByFamilyId(parsedToken.familyId());
            familyTokens.forEach(t -> t.setRevoked(true));
            refreshTokenRepository.saveAll(familyTokens);
            throw new InvalidCredentialsException("Compromised token family. All sessions have been revoked. Please log in again.");
        }

        // 4. Token Rotation: Revoke the current token, so it can never be used again
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        // 5. Verify the user is still active
        AppUser user = tokenEntity.getUser();
        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        log.info("Successfully refreshed session for user: {}", user.getPublicId());

        // 6. Generate a NEW Access Token and a NEW Refresh Token (passing the SAME family ID)
        SecurityPrincipal principal = new SecurityPrincipal(user.getPublicId().toString(), user.getEmail(), Set.of(user.getRole().name()));

        IssuedToken newAccessToken = jwtTokenService.issueAccessToken(principal);
        IssuedToken newRefreshToken = jwtTokenService.issueRefreshToken(principal, parsedToken.familyId());

        // 7. Persist the new Refresh Token Hash
        RefreshToken newRefreshTokenEntity = RefreshToken.builder().tokenId(newRefreshToken.tokenId()).familyId(newRefreshToken.familyId()).hashedValue(TokenHasher.sha256(newRefreshToken.value())).expiresAt(newRefreshToken.expiresAt()).user(user).build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        // 8. Return the Result
        AuthResponse response = new AuthResponse(newAccessToken.value(), user.getPublicId(), user.getFirstName(), user.getLastName(), user.getRole().name());

        return new AuthResult(response, newRefreshToken.value());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String hashedToken = TokenHasher.sha256(rawRefreshToken);
        refreshTokenRepository.findByHashedValue(hashedToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Successfully revoked refresh token for user: {}", token.getUser().getPublicId());
        });
    }
}