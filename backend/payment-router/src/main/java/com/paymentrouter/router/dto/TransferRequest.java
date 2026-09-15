package com.paymentrouter.router.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body of POST /api/transfers.
 * <p>
 * The annotations describe request (field) validation. They are checked by
 * {@code @Valid} in the controller before any business logic runs.
 *
 * @param sourceProviderCode      provider the money is sent from, e.g. DFSP_A
 * @param destinationProviderCode provider the money is sent to, e.g. DFSP_B
 * @param amount                  amount to send
 */
public record TransferRequest(
        @NotBlank(message = "sourceProviderCode is required")
        String sourceProviderCode,

        @NotBlank(message = "destinationProviderCode is required")
        String destinationProviderCode,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be greater than zero")
        // 9 integer digits, so amount + fee still fits the NUMERIC(12,2) columns of the transactions table.
        @Digits(integer = 9, fraction = 2, message = "amount must have at most 9 digits and 2 decimal places")
        BigDecimal amount) {
}
