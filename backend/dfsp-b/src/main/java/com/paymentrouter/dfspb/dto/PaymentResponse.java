package com.paymentrouter.dfspb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response body of POST /v1/payments/receive.
 * <p>
 * {@code reason} is only present when the payment is rejected.
 *
 * @param result     ACCEPTED or REJECTED
 * @param paymentRef DFSP-B's own reference for this payment
 * @param reason     why the payment was rejected; omitted from the JSON when accepted
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        PaymentResult result,
        String paymentRef,
        String reason) {
}
