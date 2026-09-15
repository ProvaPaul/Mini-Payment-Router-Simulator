package com.paymentrouter.router.strategy;

import org.springframework.stereotype.Component;

import com.paymentrouter.router.client.DfspBAdapter;
import com.paymentrouter.router.client.DfspClient;
import com.paymentrouter.router.client.DfspTransferRequest;
import com.paymentrouter.router.client.DfspTransferResult;
import com.paymentrouter.router.entity.Provider;

/**
 * Behaviour for payments whose destination is DFSP-B.
 * <p>
 * Pricing uses the default rule from {@link DfspStrategy} with DFSP-B's fee from the database.
 * Transfers go through DFSP-B's adapter.
 */
@Component
public class DfspBStrategy implements DfspStrategy {

    private final DfspClient dfspClient;

    public DfspBStrategy(DfspBAdapter dfspBAdapter) {
        this.dfspClient = dfspBAdapter;
    }

    @Override
    public String getProviderCode() {
        return "DFSP_B";
    }

    @Override
    public DfspTransferResult executeTransfer(DfspTransferRequest request, Provider destination) {
        return dfspClient.transfer(destination.getBaseUrl(), request);
    }
}
