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
 * API foundation only: the request reaches this service, but transfer execution
 * is not implemented yet, so the caller receives 501 Not Implemented.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    public TransferResponse executeTransfer(TransferRequest request) {
        log.info("Transfer request received: {}", request);
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Transfer execution is not implemented yet");
    }
}
