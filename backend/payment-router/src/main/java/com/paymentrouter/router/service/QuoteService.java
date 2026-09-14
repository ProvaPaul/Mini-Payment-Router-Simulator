package com.paymentrouter.router.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.paymentrouter.router.dto.QuoteRequest;
import com.paymentrouter.router.dto.QuoteResponse;
import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.strategy.DfspStrategy;
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
    private final Map<String, DfspStrategy> strategiesByProviderCode;

    /**
     * Spring injects every {@link DfspStrategy} bean (DfspAStrategy, DfspBStrategy, ...) as a list.
     * They are indexed by provider code once, at startup. Two strategies with the same code
     * fail application startup.
     */
    public QuoteService(PaymentRequestValidator paymentRequestValidator, List<DfspStrategy> strategies) {
        this.paymentRequestValidator = paymentRequestValidator;
        this.strategiesByProviderCode = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(DfspStrategy::getProviderCode, Function.identity()));
    }

    public QuoteResponse calculateQuote(QuoteRequest request) {
        log.info("Quote request received: {}", request);

        PaymentProviders providers = paymentRequestValidator.validate(
                request.sourceProviderCode(), request.destinationProviderCode());
        Provider destination = providers.destination();

        DfspStrategy strategy = strategyFor(destination);
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

    /** Runtime strategy selection: the destination provider's code picks the implementation. */
    private DfspStrategy strategyFor(Provider provider) {
        DfspStrategy strategy = strategiesByProviderCode.get(provider.getCode());
        if (strategy == null) {
            throw new IllegalStateException("No DFSP strategy registered for provider " + provider.getCode());
        }
        return strategy;
    }
}
