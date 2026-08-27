package com.zidtech.auth.domain;

import com.zidtech.auth.domain.entity.AppUser;
import com.zidtech.auth.domain.entity.RefreshToken;
import com.zidtech.auth.domain.enums.UserRole;
import com.zidtech.auth.exception.ApiErrorResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainCoverageTest {

    @Test
    void coverAppUserLombok() {
        AppUser user = AppUser.builder().email("test@test.com").firstName("A").lastName("B")
                .passwordHash("hash").role(UserRole.CUSTOMER).isActive(true).build();
        
        user.setId(1L);
        user.setPublicId(UUID.randomUUID());
        user.setEmail("new@test.com");
        user.setPasswordHash("newHash");
        user.setFirstName("Zidan");
        user.setLastName("Ali");
        user.setRole(UserRole.ADMIN);
        user.setActive(false);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        assertNotNull(user.getId());
        assertNotNull(user.getPublicId());
        assertNotNull(user.getEmail());
        assertNotNull(user.getPasswordHash());
        assertNotNull(user.getFirstName());
        assertNotNull(user.getLastName());
        assertNotNull(user.getRole());
        assertFalse(user.isActive());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void coverRefreshTokenLombok() {
        RefreshToken token = RefreshToken.builder().tokenId(UUID.randomUUID()).familyId(UUID.randomUUID())
                .hashedValue("hash").expiresAt(Instant.now()).isRevoked(false).build();
        
        token.setId(1L);
        token.setTokenId(UUID.randomUUID());
        token.setFamilyId(UUID.randomUUID());
        token.setHashedValue("newHash");
        token.setExpiresAt(Instant.now());
        token.setRevoked(true);
        token.setUser(AppUser.builder().build());

        assertNotNull(token.getId());
        assertNotNull(token.getTokenId());
        assertNotNull(token.getFamilyId());
        assertNotNull(token.getHashedValue());
        assertNotNull(token.getExpiresAt());
        assertTrue(token.isRevoked());
        assertNotNull(token.getUser());
    }

    @Test
    void coverApiErrorResponseLombok() {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(500)
                .error("Error")
                .message("Msg")
                .path("/path")
                .validationErrors(Map.of("field", "error"))
                .build();

        assertNotNull(response.getTimestamp());
        assertEquals(500, response.getStatus());
        assertEquals("Error", response.getError());
        assertEquals("Msg", response.getMessage());
        assertEquals("/path", response.getPath());
        assertNotNull(response.getValidationErrors());
    }
}