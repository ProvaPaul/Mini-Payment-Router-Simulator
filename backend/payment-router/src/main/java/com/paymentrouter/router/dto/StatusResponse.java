package com.paymentrouter.router.dto;

import java.time.Instant;

/**
 * Response body of the status endpoint.
 *
 * @param service   application name, read from configuration
 * @param status    "UP" when the application is able to answer requests
 * @param timestamp server time when the response was created
 */
public record StatusResponse(String service, String status, Instant timestamp) {
}
