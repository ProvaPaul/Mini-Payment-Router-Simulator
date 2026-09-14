package com.paymentrouter.router.dto;

import java.math.BigDecimal;

/**
 * Response body of POST /api/quotes. A quote is calculated, never stored.
 *
 * @param sourceProviderCode      provider the money is sent from
 * @param destinationProviderCode provider the money is sent to
 * @param amount                  amount to send
 * @param feePercentage           destination provider's current fee percentage
 * @param feeAmount               fee for this amount
 * @param totalAmount             amount + feeAmount
 */
public record QuoteResponse(
        String sourceProviderCode,
        String destinationProviderCode,
        BigDecimal amount,
        BigDecimal feePercentage,
        BigDecimal feeAmount,
        BigDecimal totalAmount) {
}
