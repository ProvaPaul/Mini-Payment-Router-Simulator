package com.paymentrouter.router.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.paymentrouter.router.dto.ProviderResponse;
import com.paymentrouter.router.entity.ProviderStatus;
import com.paymentrouter.router.repository.ProviderRepository;

/**
 * Read-only access to the providers the UI may offer.
 */
@Service
public class ProviderService {

    private final ProviderRepository providerRepository;

    public ProviderService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    /** Active providers only, ordered by code, without internal configuration. */
    public List<ProviderResponse> getActiveProviders() {
        return providerRepository.findByStatusOrderByCodeAsc(ProviderStatus.ACTIVE).stream()
                .map(provider -> new ProviderResponse(provider.getCode(), provider.getName()))
                .toList();
    }
}
