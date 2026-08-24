package com.zidtech.auth.dto;

public record AuthResult(
        AuthResponse response,
        String rawRefreshToken
) {}