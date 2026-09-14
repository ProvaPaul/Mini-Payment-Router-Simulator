package com.paymentrouter.router.exception;

/**
 * Thrown by a DFSP adapter when the DFSP cannot be reached (connection refused, timeout),
 * answers with an HTTP error, or returns a response the adapter cannot understand.
 * <p>
 * A DFSP that answers normally but REJECTS a transfer is not an exception; that is a
 * regular unsuccessful result.
 */
public class DfspCommunicationException extends RuntimeException {

    public DfspCommunicationException(String message) {
        super(message);
    }

    public DfspCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
