package com.zidtech.auth.controller;

import com.zidtech.auth.dto.AuthResponse;
import com.zidtech.auth.dto.AuthResult;
import com.zidtech.auth.dto.LoginRequest;
import com.zidtech.auth.dto.RegisterRequest;
import com.zidtech.auth.service.AuthService;
import com.zidtech.security.RefreshTokenCookieFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Auto-wired from your common-security GitHub Package!
    private final RefreshTokenCookieFactory cookieFactory;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST request to register new user: {}", request.email());
        AuthResult result = authService.register(request);

        // 1. Pack the raw refresh token into a highly secure, XSS-proof HttpOnly cookie
        ResponseCookie cookie = cookieFactory.create(result.rawRefreshToken());

        // 2. Return 201 CREATED with the cookie header and the JSON Access Token payload
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.response());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST request to login user: {}", request.email());
        AuthResult result = authService.login(request);

        // 1. Pack the raw refresh token into an HttpOnly cookie
        ResponseCookie cookie = cookieFactory.create(result.rawRefreshToken());

        // 2. Return 200 OK
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @org.springframework.web.bind.annotation.CookieValue(name = "shopping_refresh", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthResult result = authService.refreshToken(refreshToken);

        // Rotate the cookie with the new refresh token
        ResponseCookie cookie = cookieFactory.create(result.rawRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @org.springframework.web.bind.annotation.CookieValue(name = "shopping_refresh", required = false) String refreshToken) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }

        // Send a blank, instantly-expiring cookie to delete it from the user's browser
        ResponseCookie clearedCookie = cookieFactory.clear();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .build();
    }
}