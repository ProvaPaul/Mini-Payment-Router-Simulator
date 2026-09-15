package com.paymentrouter.dfspa.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.paymentrouter.dfspa.dto.TransferRequest;
import com.paymentrouter.dfspa.dto.TransferResponse;
import com.paymentrouter.dfspa.dto.TransferStatus;

/**
 * Plain unit test of DFSP-A's decision rules. No Spring, no HTTP.
 */
class TransferServiceTest {

    private final TransferService service = new TransferService(new BigDecimal("50000.00"));

    @Test
    void acceptsAmountWithinLimit() {
        TransferResponse response = service.process(request("1000.00"));

        assertThat(response.status()).isEqualTo(TransferStatus.SUCCESS);
        assertThat(response.referenceId()).matches("A-TXN-[0-9A-F]{8}");
        assertThat(response.message()).isEqualTo("Transfer completed");
    }

    @Test
    void acceptsAmountExactlyAtLimit() {
        assertThat(service.process(request("50000.00")).status()).isEqualTo(TransferStatus.SUCCESS);
    }

    @Test
    void rejectsAmountAboveLimit() {
        TransferResponse response = service.process(request("50000.01"));

        assertThat(response.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(response.message()).isEqualTo("Amount exceeds DFSP-A limit of 50000.00");
    }

    @Test
    void rejectsMissingOrNonPositiveAmount() {
        assertThat(service.process(request(null)).status()).isEqualTo(TransferStatus.FAILED);
        assertThat(service.process(request("0")).message()).isEqualTo("Invalid amount");
    }

    private static TransferRequest request(String amount) {
        return new TransferRequest("3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f", "DFSP_B",
                amount == null ? null : new BigDecimal(amount), new BigDecimal("10.00"));
    }
}
