package com.paymentrouter.router.service;

import java.util.List;

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
    private final List<DfspStrategy> dfspStrategies;

    /** Spring injects every {@link DfspStrategy} bean (currently DFSP-A and DFSP-B) as a list. */
    public QuoteService(PaymentRequestValidator paymentRequestValidator, List<DfspStrategy> dfspStrategies) {
        this.paymentRequestValidator = paymentRequestValidator;
        this.dfspStrategies = dfspStrategies;
    }

    public QuoteResponse calculateQuote(QuoteRequest request) {
        log.info("Quote request received: {}", request);

        PaymentProviders providers = paymentRequestValidator.validate(
                request.sourceProviderCode(), request.destinationProviderCode());
        Provider destination = providers.destination();

        DfspStrategy strategy = strategyFor(destination.getCode());
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
}
