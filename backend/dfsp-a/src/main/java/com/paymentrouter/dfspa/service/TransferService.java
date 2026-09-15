package com.paymentrouter.dfspa.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.paymentrouter.dfspa.dto.TransferRequest;
import com.paymentrouter.dfspa.dto.TransferResponse;
import com.paymentrouter.dfspa.dto.TransferStatus;

/**
 * Simulates DFSP-A's decision for an incoming transfer.
 * <p>
 * Stateless: nothing is stored. A transfer is accepted unless the amount is missing,
 * not positive, or above DFSP-A's per-transfer limit.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final BigDecimal maxTransferAmount;

    public TransferService(@Value("${dfsp-a.max-transfer-amount}") BigDecimal maxTransferAmount) {
        this.maxTransferAmount = maxTransferAmount;
    }

    public TransferResponse process(TransferRequest request) {
        log.info("DFSP-A received transfer: {}", request);

        String referenceId = newReferenceId();
        TransferResponse response;

        if (request.amount() == null || request.amount().signum() <= 0) {
            response = new TransferResponse(TransferStatus.FAILED, referenceId, "Invalid amount");
        } else if (request.amount().compareTo(maxTransferAmount) > 0) {
            response = new TransferResponse(TransferStatus.FAILED, referenceId,
                    "Amount exceeds DFSP-A limit of " + maxTransferAmount);
        } else {
            response = new TransferResponse(TransferStatus.SUCCESS, referenceId, "Transfer completed");
        }

        log.info("DFSP-A decision for transaction {}: {}", request.transactionId(), response);
        return response;
    }

    /** DFSP-A's own reference, e.g. A-TXN-3F6E2C1A. */
    private static String newReferenceId() {
        return "A-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
