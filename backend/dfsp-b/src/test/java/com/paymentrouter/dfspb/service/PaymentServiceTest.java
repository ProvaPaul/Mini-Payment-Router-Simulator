package com.paymentrouter.dfspb.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.paymentrouter.dfspb.dto.PaymentRequest;
import com.paymentrouter.dfspb.dto.PaymentResponse;
import com.paymentrouter.dfspb.dto.PaymentResult;

/**
 * Plain unit test of DFSP-B's decision rules. No Spring, no HTTP.
 */
class PaymentServiceTest {

    private final PaymentService service = new PaymentService(2_500_000L);

    @Test
    void acceptsAmountWithinLimitWithoutReason() {
        PaymentResponse response = service.receive(request(100_000L));

        assertThat(response.result()).isEqualTo(PaymentResult.ACCEPTED);
        assertThat(response.paymentRef()).matches("B-PAY-[0-9A-F]{8}");
        assertThat(response.reason()).isNull();
    }

    @Test
    void acceptsAmountExactlyAtLimit() {
        assertThat(service.receive(request(2_500_000L)).result()).isEqualTo(PaymentResult.ACCEPTED);
    }

    @Test
    void rejectsAmountAboveLimit() {
        PaymentResponse response = service.receive(request(2_500_001L));

        assertThat(response.result()).isEqualTo(PaymentResult.REJECTED);
        assertThat(response.reason()).isEqualTo("Amount exceeds DFSP-B limit of 2500000 paisa");
    }

    @Test
    void rejectsMissingOrNonPositiveAmount() {
        assertThat(service.receive(request(null)).result()).isEqualTo(PaymentResult.REJECTED);
        assertThat(service.receive(request(0L)).reason()).isEqualTo("Invalid amount");
    }

    private static PaymentRequest request(Long amountInPaisa) {
        return new PaymentRequest("3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f", "DFSP_A", amountInPaisa, 1_500L);
    }
}
