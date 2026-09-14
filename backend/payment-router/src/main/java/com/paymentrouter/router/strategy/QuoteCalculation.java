package com.paymentrouter.router.strategy;

import java.math.BigDecimal;

/**
 * Result of pricing a payment for one destination provider.
 *
 * @param amount        amount to send, with 2 decimal places
 * @param feePercentage destination provider's fee percentage that was applied
 * @param feeAmount     calculated fee
 * @param totalAmount   amount + feeAmount
 */
public record QuoteCalculation(
        BigDecimal amount,
        BigDecimal feePercentage,
        BigDecimal feeAmount,
        BigDecimal totalAmount) {
}
