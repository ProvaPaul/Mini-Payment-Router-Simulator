package com.paymentrouter.router.client;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Provider-neutral transfer data the router gives to any {@link DfspClient}.
 *
 * @param transactionId      router's transaction identifier, sent to the DFSP as a reference
 * @param sourceProviderCode provider the money comes from, e.g. DFSP_A
 * @param amount             amount in taka, 2 decimal places
 * @param feeAmount          fee in taka, 2 decimal places
 */
public record DfspTransferRequest(
        UUID transactionId,
        String sourceProviderCode,
        BigDecimal amount,
        BigDecimal feeAmount) {
}
