package com.zidtech.auth.dto;

import java.util.UUID;

/**
 * Notice that we DO NOT send the Refresh Token in this JSON payload.
 * Refresh Tokens must be sent in a secure, HttpOnly cookie to prevent XSS attacks.
 */
public record AuthResponse(
        String accessToken,
        UUID userId, // The safe publicId, NOT the internal database Long ID
        String firstName,
        String lastName,
        String role
) {}