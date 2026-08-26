package com.zidtech.auth.service;

import com.zidtech.auth.domain.entity.AppUser;
import com.zidtech.auth.domain.entity.RefreshToken;
import com.zidtech.auth.domain.enums.UserRole;
import com.zidtech.auth.dto.AuthResult;
import com.zidtech.auth.dto.LoginRequest;
import com.zidtech.auth.dto.RegisterRequest;
import com.zidtech.auth.exception.DuplicateResourceException;
import com.zidtech.auth.exception.InvalidCredentialsException;
import com.zidtech.auth.repository.RefreshTokenRepository;
import com.zidtech.auth.repository.UserRepository;
import com.zidtech.auth.service.AuthServiceImpl;
import com.zidtech.security.IssuedToken;
import com.zidtech.security.JwtTokenService;
import com.zidtech.security.ParsedToken;
import com.zidtech.security.SecurityPrincipal;
import com.zidtech.security.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private AppUser mockUser;
    private IssuedToken mockAccessToken;
    private IssuedToken mockRefreshToken;

    @BeforeEach
    void setUp() {
        mockUser = AppUser.builder()
                .firstName("Zidan")
                .lastName("Ali")
                .email("zidan@zidtech.com")
                .passwordHash("hashed_password")
                .role(UserRole.CUSTOMER)
                .build();

        mockAccessToken = new IssuedToken("access.jwt.string", UUID.randomUUID(), null, Instant.now().plusSeconds(900));
        mockRefreshToken = new IssuedToken("refresh.jwt.string", UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(86400));
    }

    // --- REGISTRATION TESTS ---

    @Test
    @DisplayName("Register: Should successfully create user and return token pair")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("Zidan", "Ali", "zidan@zidtech.com", "Password123!");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed_password");
        when(userRepository.save(any(AppUser.class))).thenReturn(mockUser);
        when(jwtTokenService.issueAccessToken(any(SecurityPrincipal.class))).thenReturn(mockAccessToken);
        when(jwtTokenService.issueRefreshToken(any(SecurityPrincipal.class), isNull())).thenReturn(mockRefreshToken);

        AuthResult result = authService.register(request);

        assertThat(result.response().accessToken()).isEqualTo("access.jwt.string");
        assertThat(result.rawRefreshToken()).isEqualTo("refresh.jwt.string");
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Register: Should block registration if email already exists")
    void register_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest("Zidan", "Ali", "zidan@zidtech.com", "Password123!");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email is already in use");

        // Verify we never attempted to save a user or generate tokens
        verify(userRepository, never()).save(any());
        verify(jwtTokenService, never()).issueAccessToken(any());
    }

    // --- LOGIN TESTS ---

    @Test
    @DisplayName("Login: Should fail when email is not found")
    void login_EmailNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest("ghost@zidtech.com", "Password123!");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Login: Should fail when password does not match")
    void login_InvalidPassword_ThrowsException() {
        LoginRequest request = new LoginRequest("zidan@zidtech.com", "WrongPassword!");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.password(), mockUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("Login: Should fail when user account is deactivated")
    void login_DeactivatedAccount_ThrowsException() {
        mockUser.setActive(false); // DEACTIVATE USER
        LoginRequest request = new LoginRequest("zidan@zidtech.com", "Password123!");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.password(), mockUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Account is deactivated");
    }

    @Test
    @DisplayName("Login: Should successfully authenticate user and return token pair")
    void login_Success() {
        LoginRequest request = new LoginRequest("zidan@zidtech.com", "Password123!");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(request.password(), mockUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenService.issueAccessToken(any(SecurityPrincipal.class))).thenReturn(mockAccessToken);
        when(jwtTokenService.issueRefreshToken(any(SecurityPrincipal.class), any())).thenReturn(mockRefreshToken);

        AuthResult result = authService.login(request);

        assertThat(result.response().accessToken()).isEqualTo("access.jwt.string");
        assertThat(result.rawRefreshToken()).isEqualTo("refresh.jwt.string");
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    // --- REFRESH TOKEN TESTS (THE MOST CRITICAL EDGE CASES) ---

    @Test
    @DisplayName("Refresh: SECURITY DETECTED - Reusing a revoked token should revoke entire family")
    void refreshToken_ReusingRevokedToken_RevokesFamilyAndThrows() {
        String rawToken = "stolen.refresh.token";
        ParsedToken parsedToken = new ParsedToken("1", "zidan", Set.of("CUSTOMER"), TokenType.REFRESH, UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        RefreshToken compromisedToken = RefreshToken.builder().isRevoked(true).build(); // ALREADY REVOKED!
        RefreshToken sisterToken = RefreshToken.builder().isRevoked(false).build(); // Active sibling token

        when(jwtTokenService.parse(rawToken, TokenType.REFRESH)).thenReturn(parsedToken);
        when(refreshTokenRepository.findByHashedValue(anyString())).thenReturn(Optional.of(compromisedToken));
        when(refreshTokenRepository.findAllByFamilyId(parsedToken.familyId())).thenReturn(List.of(compromisedToken, sisterToken));

        assertThatThrownBy(() -> authService.refreshToken(rawToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Compromised token family");

        // VERIFY: The system actively revoked the sister token to protect the user
        assertThat(sisterToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(anyList());
    }

    // --- ADDITIONAL SERVICE COVERAGE TESTS ---

    @Test
    @DisplayName("Refresh: Should successfully rotate token pair when token is valid")
    void refreshToken_Success() {
        String rawToken = "valid.refresh.token";
        ParsedToken parsedToken = new ParsedToken("1", "zidan@zidtech.com", Set.of("CUSTOMER"), TokenType.REFRESH, UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(86400));

        RefreshToken tokenEntity = RefreshToken.builder()
                .isRevoked(false)
                .user(mockUser)
                .familyId(parsedToken.familyId())
                .build();

        when(jwtTokenService.parse(rawToken, TokenType.REFRESH)).thenReturn(parsedToken);
        when(refreshTokenRepository.findByHashedValue(anyString())).thenReturn(Optional.of(tokenEntity));
        when(jwtTokenService.issueAccessToken(any(SecurityPrincipal.class))).thenReturn(mockAccessToken);
        when(jwtTokenService.issueRefreshToken(any(SecurityPrincipal.class), any())).thenReturn(mockRefreshToken);

        AuthResult result = authService.refreshToken(rawToken);

        assertThat(result.response().accessToken()).isEqualTo("access.jwt.string");
        assertThat(result.rawRefreshToken()).isEqualTo("refresh.jwt.string");
        assertThat(tokenEntity.isRevoked()).isTrue(); // Verifies rotation/revocation of old token
        verify(refreshTokenRepository, times(1)).save(tokenEntity);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // Saves new token
    }

    @Test
    @DisplayName("Refresh: Should fail when refresh token is not found in database")
    void refreshToken_NotFound_ThrowsException() {
        String rawToken = "unknown.refresh.token";
        ParsedToken parsedToken = new ParsedToken("1", "zidan@zidtech.com", Set.of("CUSTOMER"), TokenType.REFRESH, UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(86400));

        when(jwtTokenService.parse(rawToken, TokenType.REFRESH)).thenReturn(parsedToken);
        when(refreshTokenRepository.findByHashedValue(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(rawToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Refresh token not found");
    }

    @Test
    @DisplayName("Refresh: Should fail when user account is deactivated during token refresh")
    void refreshToken_DeactivatedAccount_ThrowsException() {
        String rawToken = "valid.refresh.token";
        mockUser.setActive(false); // DEACTIVATE USER
        ParsedToken parsedToken = new ParsedToken("1", "zidan@zidtech.com", Set.of("CUSTOMER"), TokenType.REFRESH, UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(86400));

        RefreshToken tokenEntity = RefreshToken.builder()
                .isRevoked(false)
                .user(mockUser)
                .familyId(parsedToken.familyId())
                .build();

        when(jwtTokenService.parse(rawToken, TokenType.REFRESH)).thenReturn(parsedToken);
        when(refreshTokenRepository.findByHashedValue(anyString())).thenReturn(Optional.of(tokenEntity));

        assertThatThrownBy(() -> authService.refreshToken(rawToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Account is deactivated");
    }

    @Test
    @DisplayName("Logout: Should successfully revoke refresh token if it exists")
    void logout_Success() {
        String rawToken = "valid.refresh.token";
        RefreshToken tokenEntity = RefreshToken.builder()
                .isRevoked(false)
                .user(mockUser)
                .build();

        when(refreshTokenRepository.findByHashedValue(anyString())).thenReturn(Optional.of(tokenEntity));

        authService.logout(rawToken);

        assertThat(tokenEntity.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(1)).save(tokenEntity);
    }
}