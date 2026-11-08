package com.finpay.exception;

/** The same Idempotency-Key was reused with a different request payload. */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
