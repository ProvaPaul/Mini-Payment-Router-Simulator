package com.paymentrouter.router.client;

/**
 * Provider-neutral outcome of a DFSP transfer call.
 *
 * @param successful        true when the DFSP accepted the transfer, false when it rejected it
 * @param providerReference DFSP's own reference for the transfer, may be null
 * @param message           human-readable outcome reported by the DFSP
 */
public record DfspTransferResult(
        boolean successful,
        String providerReference,
        String message) {
}
