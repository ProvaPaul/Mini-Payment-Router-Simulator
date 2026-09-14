package com.paymentrouter.router.strategy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Returns the {@link DfspStrategy} for a provider code.
 * <p>
 * Spring creates the strategy beans and injects them here as a list; this factory only
 * SELECTS the right one at runtime. Every service that needs a DFSP strategy (quotes now,
 * transfers next) uses this single lookup instead of repeating it.
 */
@Component
public class DfspStrategyFactory {

    private final Map<String, DfspStrategy> strategiesByProviderCode;

    /**
     * Indexes all strategies by provider code once, at startup.
     *
     * @throws IllegalStateException if two strategies report the same provider code
     */
    public DfspStrategyFactory(List<DfspStrategy> strategies) {
        this.strategiesByProviderCode = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(DfspStrategy::getProviderCode, Function.identity()));
    }

    /**
     * @param providerCode provider code from the providers table, e.g. DFSP_B
     * @return the strategy registered for that code
     * @throws IllegalStateException if no strategy is registered for the code (server misconfiguration)
     */
    public DfspStrategy getStrategy(String providerCode) {
        DfspStrategy strategy = strategiesByProviderCode.get(providerCode);
        if (strategy == null) {
            throw new IllegalStateException("No DFSP strategy registered for provider " + providerCode);
        }
        return strategy;
    }
}
