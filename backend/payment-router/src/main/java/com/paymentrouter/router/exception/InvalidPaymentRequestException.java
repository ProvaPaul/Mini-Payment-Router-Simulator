package com.paymentrouter.router.exception;

/**
 * Thrown when a quote or transfer request is well-formed but breaks a business rule,
 * for example the same source and destination provider, or an unknown provider.
 * Returned to the client as HTTP 400.
 */
public class InvalidPaymentRequestException extends RuntimeException {

    public InvalidPaymentRequestException(String message) {
        super(message);
    }
}
