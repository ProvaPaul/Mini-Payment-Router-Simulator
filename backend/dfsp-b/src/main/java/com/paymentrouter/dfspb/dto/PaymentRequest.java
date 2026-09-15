package com.paymentrouter.dfspb.dto;

/**
 * Request body of POST /v1/payments/receive, as sent by the Payment Router.
 * <p>
 * DFSP-B works in whole paisa (1 taka = 100 paisa), not decimal taka.
 *
 * @param clientRef     caller's reference for this payment (the router's transaction id)
 * @param senderDfsp    provider the money comes from, e.g. DFSP_A
 * @param amountInPaisa amount in paisa, e.g. 100000 for 1000.00 taka
 * @param feeInPaisa    fee in paisa, e.g. 1500 for 15.00 taka
 */
public record PaymentRequest(
        String clientRef,
        String senderDfsp,
        Long amountInPaisa,
        Long feeInPaisa) {
}
