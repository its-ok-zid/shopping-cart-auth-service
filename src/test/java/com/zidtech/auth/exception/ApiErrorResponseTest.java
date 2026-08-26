package com.zidtech.auth.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiErrorResponseTest {

    @Test
    @DisplayName("Force 100% Coverage for ApiErrorResponse and its Lombok Builder")
    void fullCoverageTest() {
        Instant now = Instant.now();
        Map<String, String> errors = Map.of("field", "error");

        // 1. Invoke builder() and all builder setters
        ApiErrorResponse.ApiErrorResponseBuilder builder = ApiErrorResponse.builder()
                .timestamp(now)
                .status(404)
                .error("Not Found")
                .message("Missing")
                .path("/api/test")
                .validationErrors(errors);

        // 2. Invoke the Builder's toString() method
        String builderStr = builder.toString();
        assertNotNull(builderStr);
        assertTrue(builderStr.contains("Not Found"));

        // 3. Invoke build() which triggers the all-args constructor
        ApiErrorResponse response = builder.build();

        // 4. Invoke all 6 getters
        assertEquals(now, response.getTimestamp());
        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getError());
        assertEquals("Missing", response.getMessage());
        assertEquals("/api/test", response.getPath());
        assertEquals(errors, response.getValidationErrors());
    }
}