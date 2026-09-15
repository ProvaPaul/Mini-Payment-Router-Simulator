package com.paymentrouter.router.service;

import java.net.SocketTimeoutException;
import java.util.List;
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
import com.paymentrouter.router.strategy.QuoteCalculation;

/**
 * Executes transfers:
 * validate → destination strategy → price → call destination DFSP → save transaction → respond.
 * <p>
 * Every attempt that passes validation is saved, whether the DFSP accepts it, rejects it,
 * times out, or cannot be reached.
 * <p>
 * The database write happens after the DFSP call and is not wrapped around it, so no
 * database transaction is held open while waiting for a remote service.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    static final String DFSP_UNAVAILABLE_MESSAGE = "Destination DFSP is unavailable";
    static final String DFSP_TIMEOUT_MESSAGE = "Destination DFSP did not respond in time";

    private final PaymentRequestValidator paymentRequestValidator;
    private final List<DfspStrategy> dfspStrategies;
    private final TransactionRepository transactionRepository;

    /** Spring injects every {@link DfspStrategy} bean (currently DFSP-A and DFSP-B) as a list. */
    public TransferService(
            PaymentRequestValidator paymentRequestValidator,
            List<DfspStrategy> dfspStrategies,
            TransactionRepository transactionRepository) {
        this.paymentRequestValidator = paymentRequestValidator;
        this.dfspStrategies = dfspStrategies;
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
        DfspStrategy strategy = strategyFor(destination.getCode());

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
            message = isTimeout(exception) ? DFSP_TIMEOUT_MESSAGE : DFSP_UNAVAILABLE_MESSAGE;
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

    /**
     * Finds the strategy whose {@link DfspStrategy#getProviderCode()} matches the destination
     * provider. With only two DFSPs, searching the short injected list directly is simpler than
     * a dedicated lookup class, and adding a DFSP-C strategy bean still needs no change here.
     *
     * @throws IllegalStateException if no strategy is registered for the code (server misconfiguration)
     */
    private DfspStrategy strategyFor(String providerCode) {
        return dfspStrategies.stream()
                .filter(strategy -> strategy.getProviderCode().equals(providerCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No DFSP strategy registered for provider " + providerCode));
    }

    /** DFSP message plus its reference, e.g. "Transfer completed (ref A-TXN-4854902E)". */
    private static String describe(DfspTransferResult result) {
        if (result.providerReference() == null) {
            return result.message();
        }
        return result.message() + " (ref " + result.providerReference() + ")";
    }

    /** True when the DFSP call failed because the connect or read timeout expired. */
    private static boolean isTimeout(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
