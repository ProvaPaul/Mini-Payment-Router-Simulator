package com.paymentrouter.router.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.paymentrouter.router.dto.ProviderResponse;
import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;
import com.paymentrouter.router.repository.ProviderRepository;

/**
 * Unit test of the provider-listing business logic (previously exercised indirectly through
 * {@code GET /api/providers} via MockMvc). Repository is mocked, so no database is needed.
 */
@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private ProviderService providerService;

    @Test
    void returnsActiveProvidersAsCodeAndNameOnly() {
        when(providerRepository.findByStatusOrderByCodeAsc(ProviderStatus.ACTIVE)).thenReturn(List.of(
                new Provider("DFSP_A", "DFSP-A", "http://dfsp-a:8081", new BigDecimal("1.00"), ProviderStatus.ACTIVE),
                new Provider("DFSP_B", "DFSP-B", "http://dfsp-b:8082", new BigDecimal("1.50"), ProviderStatus.ACTIVE)));

        List<ProviderResponse> providers = providerService.getActiveProviders();

        // Only code and name are exposed: base_url and fee_percentage never leak to the UI.
        assertThat(providers).containsExactly(
                new ProviderResponse("DFSP_A", "DFSP-A"),
                new ProviderResponse("DFSP_B", "DFSP-B"));
    }

    @Test
    void returnsEmptyListWhenNoProviderIsActive() {
        when(providerRepository.findByStatusOrderByCodeAsc(ProviderStatus.ACTIVE)).thenReturn(List.of());

        assertThat(providerService.getActiveProviders()).isEmpty();
    }
}
