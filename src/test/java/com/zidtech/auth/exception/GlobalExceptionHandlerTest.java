package com.zidtech.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void testHandleValidationExceptions() {
        when(request.getRequestURI()).thenReturn("/api/test");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new FieldError("objectName", "email", "invalid format")));

        ResponseEntity<ApiErrorResponse> response = handler.handleValidationExceptions(ex, request);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("invalid format", response.getBody().getValidationErrors().get("email"));
    }

    @Test
    void testHandleDuplicateResource() {
        when(request.getRequestURI()).thenReturn("/api/test");
        ResponseEntity<ApiErrorResponse> response = handler.handleDuplicateResource(new DuplicateResourceException("Duplicate!"), request);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void testHandleInvalidCredentials() {
        when(request.getRequestURI()).thenReturn("/api/test");
        ResponseEntity<ApiErrorResponse> response = handler.handleInvalidCredentials(new InvalidCredentialsException("Invalid!"), request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testHandleAllOtherExceptions() {
        when(request.getRequestURI()).thenReturn("/api/test");
        ResponseEntity<ApiErrorResponse> response = handler.handleAllOtherExceptions(new RuntimeException("Unknown Error"), request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}