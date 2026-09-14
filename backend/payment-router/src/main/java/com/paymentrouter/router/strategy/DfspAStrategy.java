package com.paymentrouter.router.strategy;

import org.springframework.stereotype.Component;

/**
 * Behaviour for payments whose destination is DFSP-A.
 * <p>
 * Pricing uses the default rule from {@link DfspStrategy} with DFSP-A's fee from the database.
 */
@Component
public class DfspAStrategy implements DfspStrategy {

    @Override
    public String getProviderCode() {
        return "DFSP_A";
    }
}
