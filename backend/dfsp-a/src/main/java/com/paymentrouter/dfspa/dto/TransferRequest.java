package com.paymentrouter.dfspa.dto;

import java.math.BigDecimal;

/**
 * Request body of POST /api/dfsp-a/transfers, as sent by the Payment Router.
 *
 * @param transactionId  router's transaction identifier
 * @param sourceProvider provider the money comes from, e.g. DFSP_B
 * @param amount         amount in taka (decimal)
 * @param fee            fee in taka (decimal)
 */
public record TransferRequest(
        String transactionId,
        String sourceProvider,
        BigDecimal amount,
        BigDecimal fee) {
}
