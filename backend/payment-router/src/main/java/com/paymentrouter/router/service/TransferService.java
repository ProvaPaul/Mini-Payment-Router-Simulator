package com.paymentrouter.router.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.paymentrouter.router.client.DfspTransferRequest;
import com.paymentrouter.router.client.DfspTransferResult;
import com.paymentrouter.router.dto.TransferRequest;
import com.paymentrouter.router.dto.TransferResponse;
import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.Transaction;
import com.paymentrouter.router.entity.TransactionStatus;
import com.paymentrouter.router.exception.DfspCommunicationException;
import com.paymentrouter.router.repository.TransactionRepository;
import com.paymentrouter.router.strategy.DfspStrategy;
import com.paymentrouter.router.strategy.DfspStrategyFactory;
import com.paymentrouter.router.strategy.QuoteCalculation;

/**
 * Executes transfers:
 * validate → destination strategy → price → call destination DFSP → save transaction → respond.
 * <p>
 * Every attempt that passes validation is saved, whether the DFSP accepts it, rejects it,
 * or cannot be reached.
 * <p>
 * The database write happens after the DFSP call and is not wrapped around it, so no
 * database transaction is held open while waiting for a remote service.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    static final String DFSP_UNAVAILABLE_MESSAGE = "Destination DFSP is unavailable";

    private final PaymentRequestValidator paymentRequestValidator;
    private final DfspStrategyFactory dfspStrategyFactory;
    private final TransactionRepository transactionRepository;

    public TransferService(
            PaymentRequestValidator paymentRequestValidator,
            DfspStrategyFactory dfspStrategyFactory,
            TransactionRepository transactionRepository) {
        this.paymentRequestValidator = paymentRequestValidator;
        this.dfspStrategyFactory = dfspStrategyFactory;
        this.transactionRepository = transactionRepository;
    }

    public TransferResponse executeTransfer(TransferRequest request) {
        log.info("Transfer request received: {}", request);

        // 1. Validate: providers differ, exist and are active.
        PaymentProviders providers = paymentRequestValidator.validate(
                request.sourceProviderCode(), request.destinationProviderCode());
        Provider source = providers.source();
        Provider destination = providers.destination();

        // 2. Determine the destination DFSP's strategy.
        DfspStrategy strategy = dfspStrategyFactory.getStrategy(destination.getCode());

        // 3. Price with the destination provider's CURRENT fee (same rule as quotes).
        QuoteCalculation pricing = strategy.calculateQuote(request.amount(), destination);

        // 4. Create the transaction id first, so the DFSP receives it as a reference.
        UUID transactionId = UUID.randomUUID();
        DfspTransferRequest dfspRequest = new DfspTransferRequest(
                transactionId, source.getCode(), pricing.amount(), pricing.feeAmount());

        // 5. Route to the destination DFSP (strategy → adapter → HTTP) and read its decision.
        TransactionStatus status;
        String message;
        log.info("Sending transfer {} to {} at {}", transactionId, destination.getCode(), destination.getBaseUrl());
        try {
            DfspTransferResult result = strategy.executeTransfer(dfspRequest, destination);
            status = result.successful() ? TransactionStatus.SUCCESS : TransactionStatus.FAILED;
            message = describe(result);
            log.info("{} responded for transfer {}: successful={}, reference={}, message={}",
                    destination.getCode(), transactionId, result.successful(),
                    result.providerReference(), result.message());
        } catch (DfspCommunicationException exception) {
            status = TransactionStatus.FAILED;
            message = DFSP_UNAVAILABLE_MESSAGE;
            log.error("Could not complete transfer {} with {}: {}",
                    transactionId, destination.getCode(), exception.getMessage(), exception);
        }

        // 6. Save the attempt with its pricing snapshot.
        Transaction transaction = transactionRepository.save(new Transaction(
                transactionId,
                source,
                destination,
                pricing.amount(),
                pricing.feePercentage(),
                pricing.feeAmount(),
                pricing.totalAmount(),
                status));

        if (status == TransactionStatus.SUCCESS) {
            log.info("Transfer {} succeeded: {}", transactionId, message);
        } else {
            log.warn("Transfer {} failed: {}", transactionId, message);
        }

        // 7. Respond.
        return new TransferResponse(
                transaction.getTransactionId(),
                source.getCode(),
                destination.getCode(),
                transaction.getAmount(),
                transaction.getFeePercentage(),
                transaction.getFeeAmount(),
                transaction.getTotalAmount(),
                transaction.getStatus(),
                message,
                transaction.getCreatedAt());
    }

    /** DFSP message plus its reference, e.g. "Transfer completed (ref A-TXN-4854902E)". */
    private static String describe(DfspTransferResult result) {
        if (result.providerReference() == null) {
            return result.message();
        }
        return result.message() + " (ref " + result.providerReference() + ")";
    }
}
