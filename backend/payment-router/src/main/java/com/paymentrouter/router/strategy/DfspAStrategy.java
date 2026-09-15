package com.paymentrouter.router.strategy;

import org.springframework.stereotype.Component;

import com.paymentrouter.router.client.DfspAAdapter;
import com.paymentrouter.router.client.DfspClient;
import com.paymentrouter.router.client.DfspTransferRequest;
import com.paymentrouter.router.client.DfspTransferResult;
import com.paymentrouter.router.entity.Provider;

/**
 * Behaviour for payments whose destination is DFSP-A.
 * <p>
 * Pricing uses the default rule from {@link DfspStrategy} with DFSP-A's fee from the database.
 * Transfers go through DFSP-A's adapter.
 */
@Component
public class DfspAStrategy implements DfspStrategy {

    private final DfspClient dfspClient;

    public DfspAStrategy(DfspAAdapter dfspAAdapter) {
        this.dfspClient = dfspAAdapter;
    }

    @Override
    public String getProviderCode() {
        return "DFSP_A";
    }

    @Override
    public DfspTransferResult executeTransfer(DfspTransferRequest request, Provider destination) {
        return dfspClient.transfer(destination.getBaseUrl(), request);
    }
}
