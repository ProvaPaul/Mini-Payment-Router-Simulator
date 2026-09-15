package com.paymentrouter.dfspb.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentrouter.dfspb.dto.PaymentRequest;
import com.paymentrouter.dfspb.dto.PaymentResponse;
import com.paymentrouter.dfspb.service.PaymentService;

/**
 * DFSP-B's only API, called by the Payment Router.
 * <p>
 * A processed payment always returns HTTP 200; the decision (ACCEPTED or REJECTED) is in the body.
 */
@RestController
@RequestMapping("/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/receive")
    public PaymentResponse receivePayment(@RequestBody PaymentRequest request) {
        return paymentService.receive(request);
    }
}
