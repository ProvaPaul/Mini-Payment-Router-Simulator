package com.paymentrouter.dfspa.dto;

/**
 * Response body of POST /api/dfsp-a/transfers.
 *
 * @param status      SUCCESS or FAILED
 * @param referenceId DFSP-A's own reference for this transfer
 * @param message     human-readable outcome
 */
public record TransferResponse(
        TransferStatus status,
        String referenceId,
        String message) {
}
