package com.paymentrouter.router.dto;

import java.math.BigDecimal;

/**
 * Request body of POST /api/quotes.
 *
 * @param sourceProviderCode      provider the money is sent from, e.g. DFSP_A
 * @param destinationProviderCode provider the money is sent to, e.g. DFSP_B
 * @param amount                  amount to send
 */
public record QuoteRequest(
        String sourceProviderCode,
        String destinationProviderCode,
        BigDecimal amount) {
}
