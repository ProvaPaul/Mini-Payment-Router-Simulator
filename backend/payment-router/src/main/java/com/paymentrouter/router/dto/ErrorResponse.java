package com.paymentrouter.router.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Error body returned for every failed request (400, 404, 405, 415 and 500).
 *
 * @param timestamp   when the error happened
 * @param status      HTTP status code, e.g. 400
 * @param error       HTTP reason phrase, e.g. "Bad Request"
 * @param message     short description of what is wrong
 * @param fieldErrors field name → validation message; empty for business rule errors
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {
}
