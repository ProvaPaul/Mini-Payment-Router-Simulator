package com.paymentrouter.router.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.paymentrouter.router.entity.TransactionStatus;

/**
 * Response body of POST /api/transfers: the saved transaction and its pricing snapshot.
 *
 * @param transactionId           public transaction identifier
 * @param sourceProviderCode      provider the money is sent from
 * @param destinationProviderCode provider the money is sent to
 * @param amount                  amount sent
 * @param feePercentage           fee percentage used for this transfer (snapshot)
 * @param feeAmount               fee charged (snapshot)
 * @param totalAmount             amount + feeAmount (snapshot)
 * @param status                  SUCCESS or FAILED
 * @param message                 human-readable outcome from the DFSP
 * @param createdAt               when the transaction was recorded
 */
public record TransferResponse(
        UUID transactionId,
        String sourceProviderCode,
        String destinationProviderCode,
        BigDecimal amount,
        BigDecimal feePercentage,
        BigDecimal feeAmount,
        BigDecimal totalAmount,
        TransactionStatus status,
        String message,
        Instant createdAt) {
}
