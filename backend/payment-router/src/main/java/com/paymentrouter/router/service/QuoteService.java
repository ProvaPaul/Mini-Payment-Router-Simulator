package com.paymentrouter.router.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.paymentrouter.router.dto.QuoteRequest;
import com.paymentrouter.router.dto.QuoteResponse;
import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.strategy.DfspStrategy;
import com.paymentrouter.router.strategy.DfspStrategyFactory;
import com.paymentrouter.router.strategy.QuoteCalculation;

/**
 * Calculates quotes. A quote is only a price calculation: nothing is saved and no DFSP is called.
 * <p>
 * The DFSP-specific part is delegated to the {@link DfspStrategy} of the destination provider.
 */
@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    private final PaymentRequestValidator paymentRequestValidator;
    private final DfspStrategyFactory dfspStrategyFactory;

    public QuoteService(PaymentRequestValidator paymentRequestValidator, DfspStrategyFactory dfspStrategyFactory) {
        this.paymentRequestValidator = paymentRequestValidator;
        this.dfspStrategyFactory = dfspStrategyFactory;
    }

    public QuoteResponse calculateQuote(QuoteRequest request) {
        log.info("Quote request received: {}", request);

        PaymentProviders providers = paymentRequestValidator.validate(
                request.sourceProviderCode(), request.destinationProviderCode());
        Provider destination = providers.destination();

        DfspStrategy strategy = dfspStrategyFactory.getStrategy(destination.getCode());
        QuoteCalculation calculation = strategy.calculateQuote(request.amount(), destination);

        QuoteResponse response = new QuoteResponse(
                providers.source().getCode(),
                destination.getCode(),
                calculation.amount(),
                calculation.feePercentage(),
                calculation.feeAmount(),
                calculation.totalAmount());

        log.info("Quote calculated with {}: {}", strategy.getClass().getSimpleName(), response);
        return response;
    }
}
