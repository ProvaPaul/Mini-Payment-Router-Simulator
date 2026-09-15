package com.paymentrouter.dfspb.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.paymentrouter.dfspb.dto.PaymentRequest;
import com.paymentrouter.dfspb.dto.PaymentResponse;
import com.paymentrouter.dfspb.dto.PaymentResult;

/**
 * Simulates DFSP-B's decision for an incoming payment.
 * <p>
 * Stateless: nothing is stored. A payment is accepted unless the amount is missing,
 * not positive, or above DFSP-B's per-payment limit.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final long maxPaymentAmountInPaisa;

    public PaymentService(@Value("${dfsp-b.max-payment-amount-in-paisa}") long maxPaymentAmountInPaisa) {
        this.maxPaymentAmountInPaisa = maxPaymentAmountInPaisa;
    }

    public PaymentResponse receive(PaymentRequest request) {
        log.info("DFSP-B received payment: {}", request);

        String paymentRef = newPaymentRef();
        PaymentResponse response;

        if (request.amountInPaisa() == null || request.amountInPaisa() <= 0) {
            response = new PaymentResponse(PaymentResult.REJECTED, paymentRef, "Invalid amount");
        } else if (request.amountInPaisa() > maxPaymentAmountInPaisa) {
            response = new PaymentResponse(PaymentResult.REJECTED, paymentRef,
                    "Amount exceeds DFSP-B limit of " + maxPaymentAmountInPaisa + " paisa");
        } else {
            response = new PaymentResponse(PaymentResult.ACCEPTED, paymentRef, null);
        }

        log.info("DFSP-B decision for clientRef {}: {}", request.clientRef(), response);
        return response;
    }

    /** DFSP-B's own reference, e.g. B-PAY-3F6E2C1A. */
    private static String newPaymentRef() {
        return "B-PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
