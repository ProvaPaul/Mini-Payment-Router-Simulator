package com.paymentrouter.router.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body of POST /api/quotes.
 * <p>
 * The annotations describe request (field) validation. They are checked by
 * {@code @Valid} in the controller before any business logic runs.
 *
 * @param sourceProviderCode      provider the money is sent from, e.g. DFSP_A
 * @param destinationProviderCode provider the money is sent to, e.g. DFSP_B
 * @param amount                  amount to send
 */
public record QuoteRequest(
        @NotBlank(message = "sourceProviderCode is required")
        String sourceProviderCode,

        @NotBlank(message = "destinationProviderCode is required")
        String destinationProviderCode,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "amount must have at most 10 digits and 2 decimal places")
        BigDecimal amount) {
}
