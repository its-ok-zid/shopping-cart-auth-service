package com.zidtech.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zidtech.auth.config.SecurityConfig;
import com.zidtech.auth.dto.AuthResponse;
import com.zidtech.auth.dto.AuthResult;
import com.zidtech.auth.dto.LoginRequest;
import com.zidtech.auth.dto.RegisterRequest;
import com.zidtech.auth.service.AuthService;
import com.zidtech.security.JwtTokenService;
import com.zidtech.security.RefreshTokenCookieFactory;
import jakarta.servlet.http.Cookie;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private RefreshTokenCookieFactory cookieFactory;
    @MockBean private JwtTokenService jwtTokenService;

    @Test
    void register_ReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest("Z", "A", "z@z.com", "Password123!");
        AuthResult result = new AuthResult(new AuthResponse("token", UUID.randomUUID(), "Z", "A", "CUSTOMER"), "refresh");
        when(authService.register(any())).thenReturn(result);
        when(cookieFactory.create(any())).thenReturn(ResponseCookie.from("shopping_refresh", "refresh").build());

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());
    }

    @Test
    void login_ReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest("z@z.com", "Password123!");
        AuthResult result = new AuthResult(new AuthResponse("token", UUID.randomUUID(), "Z", "A", "CUSTOMER"), "refresh");
        when(authService.login(any())).thenReturn(result);
        when(cookieFactory.create(any())).thenReturn(ResponseCookie.from("shopping_refresh", "refresh").build());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());
    }

    @Test
    void refreshToken_ValidCookie_ReturnsOk() throws Exception {
        AuthResult result = new AuthResult(new AuthResponse("new", UUID.randomUUID(), "Z", "A", "C"), "newRef");
        when(authService.refreshToken("old")).thenReturn(result);
        when(cookieFactory.create("newRef")).thenReturn(ResponseCookie.from("shopping_refresh", "newRef").build());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("shopping_refresh", "old")))
                .andExpect(status().isOk());
    }

    @Test
    void refreshToken_MissingCookie_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_BlankCookie_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("shopping_refresh", "")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_ClearsCookie() throws Exception {
        when(cookieFactory.clear()).thenReturn(ResponseCookie.from("shopping_refresh", "").maxAge(0).build());
        mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie("shopping_refresh", "old")))
                .andExpect(status().isOk()).andExpect(header().exists(HttpHeaders.SET_COOKIE));
        verify(authService).logout("old");
    }

    @Test
    void logout_MissingCookie_ReturnsOk() throws Exception {
        when(cookieFactory.clear()).thenReturn(ResponseCookie.from("shopping_refresh", "").maxAge(0).build());
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isOk());
    }
}