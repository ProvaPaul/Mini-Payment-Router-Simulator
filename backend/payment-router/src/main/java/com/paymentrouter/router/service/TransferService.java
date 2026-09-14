package com.paymentrouter.router.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.paymentrouter.router.dto.TransferRequest;
import com.paymentrouter.router.dto.TransferResponse;

/**
 * Business logic for transfers.
 * <p>
 * Business validation is in place. Transfer execution is not implemented yet,
 * so a valid request currently receives 501 Not Implemented.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final PaymentRequestValidator paymentRequestValidator;

    public TransferService(PaymentRequestValidator paymentRequestValidator) {
        this.paymentRequestValidator = paymentRequestValidator;
    }

    public TransferResponse executeTransfer(TransferRequest request) {
        log.info("Transfer request received: {}", request);
        paymentRequestValidator.validate(request.sourceProviderCode(), request.destinationProviderCode());

        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Transfer execution is not implemented yet");
    }
}
