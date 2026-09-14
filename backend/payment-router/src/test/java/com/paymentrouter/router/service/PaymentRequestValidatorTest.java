package com.paymentrouter.router.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;
import com.paymentrouter.router.exception.InvalidPaymentRequestException;
import com.paymentrouter.router.repository.ProviderRepository;

/**
 * Unit test for business validation. The repository is mocked, so no database is needed.
 */
@ExtendWith(MockitoExtension.class)
class PaymentRequestValidatorTest {

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private PaymentRequestValidator validator;

    @Test
    void returnsBothProvidersWhenRequestIsValid() {
        Provider dfspA = provider("DFSP_A", ProviderStatus.ACTIVE);
        Provider dfspB = provider("DFSP_B", ProviderStatus.ACTIVE);
        when(providerRepository.findByCode("DFSP_A")).thenReturn(Optional.of(dfspA));
        when(providerRepository.findByCode("DFSP_B")).thenReturn(Optional.of(dfspB));

        PaymentProviders providers = validator.validate("DFSP_A", "DFSP_B");

        assertThat(providers.source()).isSameAs(dfspA);
        assertThat(providers.destination()).isSameAs(dfspB);
    }

    @Test
    void rejectsSameSourceAndDestinationWithoutQueryingTheDatabase() {
        assertThatThrownBy(() -> validator.validate("DFSP_A", "DFSP_A"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessage("Source and destination provider cannot be the same");

        verifyNoInteractions(providerRepository);
    }

    @Test
    void rejectsUnknownProvider() {
        when(providerRepository.findByCode("DFSP_A")).thenReturn(Optional.of(provider("DFSP_A", ProviderStatus.ACTIVE)));
        when(providerRepository.findByCode("DFSP_X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate("DFSP_A", "DFSP_X"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessage("Provider not found: DFSP_X");
    }

    @Test
    void rejectsInactiveProvider() {
        when(providerRepository.findByCode("DFSP_A")).thenReturn(Optional.of(provider("DFSP_A", ProviderStatus.INACTIVE)));

        assertThatThrownBy(() -> validator.validate("DFSP_A", "DFSP_B"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessage("Provider is not active: DFSP_A");
    }

    private static Provider provider(String code, ProviderStatus status) {
        return new Provider(code, code, "http://localhost:9999", new BigDecimal("1.00"), status);
    }
}
