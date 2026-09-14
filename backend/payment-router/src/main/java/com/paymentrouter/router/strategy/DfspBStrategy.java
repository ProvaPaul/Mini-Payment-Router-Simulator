package com.paymentrouter.router.strategy;

import org.springframework.stereotype.Component;

/**
 * Behaviour for payments whose destination is DFSP-B.
 * <p>
 * Pricing uses the default rule from {@link DfspStrategy} with DFSP-B's fee from the database.
 */
@Component
public class DfspBStrategy implements DfspStrategy {

    @Override
    public String getProviderCode() {
        return "DFSP_B";
    }
}
