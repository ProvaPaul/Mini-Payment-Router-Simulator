package com.paymentrouter.router.service;

import org.springframework.stereotype.Component;

import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;
import com.paymentrouter.router.exception.InvalidPaymentRequestException;
import com.paymentrouter.router.repository.ProviderRepository;

/**
 * Business validation shared by quotes and transfers.
 * <p>
 * Runs after request (field) validation, so both provider codes are already
 * known to be non-blank and the amount is known to be positive.
 * <p>
 * Source and destination are allowed to be the same provider: a DFSP can send a
 * payment to itself, and this is treated like any other transfer.
 */
@Component
public class PaymentRequestValidator {

    private final ProviderRepository providerRepository;

    public PaymentRequestValidator(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    /**
     * Checks that the source and destination providers exist and are active.
     *
     * @return the source and destination providers loaded from the database
     * @throws InvalidPaymentRequestException when a rule is broken
     */
    public PaymentProviders validate(String sourceProviderCode, String destinationProviderCode) {
        Provider source = findActiveProvider(sourceProviderCode);
        Provider destination = findActiveProvider(destinationProviderCode);
        return new PaymentProviders(source, destination);
    }

    private Provider findActiveProvider(String code) {
        Provider provider = providerRepository.findByCode(code)
                .orElseThrow(() -> new InvalidPaymentRequestException("Provider not found: " + code));

        if (provider.getStatus() != ProviderStatus.ACTIVE) {
            throw new InvalidPaymentRequestException("Provider is not active: " + code);
        }
        return provider;
    }
}
