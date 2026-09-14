package com.paymentrouter.router.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.paymentrouter.router.dto.QuoteRequest;
import com.paymentrouter.router.dto.QuoteResponse;
import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;
import com.paymentrouter.router.exception.InvalidPaymentRequestException;

/**
 * Unit test for quote calculation. Validation is mocked, so no database is needed.
 */
@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    private final Provider dfspA = provider("DFSP_A", "1.00");
    private final Provider dfspB = provider("DFSP_B", "1.50");

    @Mock
    private PaymentRequestValidator paymentRequestValidator;

    @InjectMocks
    private QuoteService quoteService;

    @Test
    void usesDestinationFeeForAToB() {
        when(paymentRequestValidator.validate("DFSP_A", "DFSP_B")).thenReturn(new PaymentProviders(dfspA, dfspB));

        QuoteResponse quote = quoteService.calculateQuote(new QuoteRequest("DFSP_A", "DFSP_B", new BigDecimal("1000")));

        assertThat(quote.sourceProviderCode()).isEqualTo("DFSP_A");
        assertThat(quote.destinationProviderCode()).isEqualTo("DFSP_B");
        assertThat(quote.amount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(quote.feePercentage()).isEqualTo(new BigDecimal("1.50"));
        assertThat(quote.feeAmount()).isEqualTo(new BigDecimal("15.00"));
        assertThat(quote.totalAmount()).isEqualTo(new BigDecimal("1015.00"));
    }

    @Test
    void usesDestinationFeeForBToA() {
        when(paymentRequestValidator.validate("DFSP_B", "DFSP_A")).thenReturn(new PaymentProviders(dfspB, dfspA));

        QuoteResponse quote = quoteService.calculateQuote(new QuoteRequest("DFSP_B", "DFSP_A", new BigDecimal("1000")));

        assertThat(quote.feePercentage()).isEqualTo(new BigDecimal("1.00"));
        assertThat(quote.feeAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(quote.totalAmount()).isEqualTo(new BigDecimal("1010.00"));
    }

    @Test
    void roundsFeeHalfUpToTwoDecimalPlaces() {
        when(paymentRequestValidator.validate("DFSP_A", "DFSP_B")).thenReturn(new PaymentProviders(dfspA, dfspB));

        // 333.33 × 1.50 / 100 = 4.99995 → 5.00
        QuoteResponse quote = quoteService.calculateQuote(new QuoteRequest("DFSP_A", "DFSP_B", new BigDecimal("333.33")));

        assertThat(quote.feeAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(quote.totalAmount()).isEqualTo(new BigDecimal("338.33"));
    }

    @Test
    void propagatesValidationFailure() {
        when(paymentRequestValidator.validate("DFSP_A", "DFSP_A"))
                .thenThrow(new InvalidPaymentRequestException("Source and destination provider cannot be the same"));

        assertThatThrownBy(() -> quoteService.calculateQuote(new QuoteRequest("DFSP_A", "DFSP_A", new BigDecimal("1000"))))
                .isInstanceOf(InvalidPaymentRequestException.class);
    }

    private static Provider provider(String code, String feePercentage) {
        return new Provider(code, code, "http://localhost:9999", new BigDecimal(feePercentage), ProviderStatus.ACTIVE);
    }
}
