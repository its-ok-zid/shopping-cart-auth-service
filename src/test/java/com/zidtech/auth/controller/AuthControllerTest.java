package com.zidtech.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zidtech.auth.config.SecurityConfig;
import com.zidtech.auth.dto.AuthResponse;
import com.zidtech.auth.dto.AuthResult;
import com.zidtech.auth.dto.RegisterRequest;
import com.zidtech.auth.service.AuthService;
import com.zidtech.security.JwtTokenService;
import com.zidtech.security.RefreshTokenCookieFactory;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class) // Load our security rules to ensure endpoints are accessible
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // We mock the service layer because we only want to test the HTTP Edge Layer!
    @MockBean
    private AuthService authService;
    @MockBean
    private RefreshTokenCookieFactory cookieFactory;

    // Required because SecurityConfig needs it to build the JwtAuthenticationFilter
    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    @DisplayName("Should return 201 Created and Set-Cookie header on successful registration")
    void register_ValidPayload_ReturnsCreated() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("Zidan", "Ali", "zidan@zidtech.com", "Password123!");
        AuthResponse authResponse = new AuthResponse("mock.access.token", UUID.randomUUID(), "Zidan", "Ali", "CUSTOMER");
        AuthResult authResult = new AuthResult(authResponse, "mock.refresh.token");

        ResponseCookie mockCookie = ResponseCookie.from("shopping_refresh", "mock.refresh.token")
                .httpOnly(true)
                .path("/")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResult);
        when(cookieFactory.create("mock.refresh.token")).thenReturn(mockCookie);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("mock.access.token"))
                .andExpect(jsonPath("$.firstName").value("Zidan"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, "shopping_refresh=mock.refresh.token; Path=/; HttpOnly"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when JSON payload is invalid")
    void register_InvalidPayload_ReturnsBadRequest() throws Exception {
        // Arrange: Missing email and password too short
        RegisterRequest request = new RegisterRequest("Zidan", "Ali", "invalid-email", "short");

        // Act & Assert: Verifies our @Valid annotations and GlobalExceptionHandler!
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payload validation failed"))
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("Should return 200 OK and clear cookie on logout")
    void logout_ClearsCookie() throws Exception {
        // Arrange
        ResponseCookie clearedCookie = ResponseCookie.from("shopping_refresh", "")
                .maxAge(0)
                .path("/")
                .build();

        when(cookieFactory.clear()).thenReturn(clearedCookie);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("shopping_refresh", "old.refresh.token")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, "shopping_refresh=; Path=/; Max-Age=0; Expires=Thu, 1 Jan 1970 00:00:00 GMT"));

        // Verify the service was actually commanded to revoke the token
        verify(authService).logout("old.refresh.token");
    }
}