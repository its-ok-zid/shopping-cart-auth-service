package com.zidtech.auth.service;

import com.zidtech.auth.dto.AuthResult;
import com.zidtech.auth.dto.LoginRequest;
import com.zidtech.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResult register(RegisterRequest request);
    AuthResult login(LoginRequest request);
    AuthResult refreshToken(String rawRefreshToken);
    void logout(String rawRefreshToken);
}