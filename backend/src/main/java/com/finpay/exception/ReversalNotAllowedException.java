package com.finpay.exception;

/**
 * Thrown when a reversal is rejected by a business rule (reversal window expired,
 * recipient no longer holds the funds, transaction type not reversible, ...).
 */
public class ReversalNotAllowedException extends RuntimeException {
    public ReversalNotAllowedException(String message) {
        super(message);
    }
}
