package com.finpay.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Client mistakes must map to 4xx; they used to surface as HTTP 500. No Spring context needed. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final WebRequest request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/test"));

    @Test
    void disabledAccountIs403NotAServerError() {
        ResponseEntity<?> response = handler.handleAccountStatusException(new DisabledException("disabled"), request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void lockedAccountIs403NotAServerError() {
        ResponseEntity<?> response = handler.handleAccountStatusException(new LockedException("locked"), request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void uniqueConstraintRaceIs409AndDoesNotLeakSql() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key value violates unique constraint \"users_email_key\""),
                request);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().message().contains("users_email_key"));
    }

    @Test
    void malformedJsonIs400() {
        HttpInputMessage input = new MockHttpInputMessage(new byte[0]);
        ResponseEntity<?> response = handler.handleMalformedRequest(
                new HttpMessageNotReadableException("bad json", input), request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void wrongHttpVerbIs405() {
        ResponseEntity<?> response = handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("GET"), request);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }
}
