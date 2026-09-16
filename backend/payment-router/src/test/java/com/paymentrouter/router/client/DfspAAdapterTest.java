package com.paymentrouter.router.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.paymentrouter.router.exception.DfspCommunicationException;

/**
 * Verifies DFSP-A's wire format against a real, tiny local HTTP server ({@link FakeDfspServer}),
 * not a running DFSP and not MockRestServiceServer: the adapter's request is really sent over a
 * socket and its response is really parsed.
 */
class DfspAAdapterTest {

    private static final UUID TRANSACTION_ID = UUID.fromString("3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f");

    private FakeDfspServer fakeDfspA;
    private DfspAAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        fakeDfspA = new FakeDfspServer("/api/dfsp-a/transfers");
        adapter = new DfspAAdapter(RestClient.create());
    }

    @AfterEach
    void tearDown() {
        fakeDfspA.stop();
    }

    @Test
    void sendsDfspAFormatAndTranslatesSuccess() {
        fakeDfspA.respondWith(200, """
                {"status":"SUCCESS","referenceId":"A-TXN-0001","message":"Transfer completed"}
                """);

        DfspTransferResult result = adapter.transfer(fakeDfspA.baseUrl(), bToA("1000.00", "10.00"));

        assertThat(result.successful()).isTrue();
        assertThat(result.providerReference()).isEqualTo("A-TXN-0001");
        assertThat(result.message()).isEqualTo("Transfer completed");
        assertThat(fakeDfspA.requestCount()).isEqualTo(1);
        assertThat(fakeDfspA.lastRequestBody())
                .contains("\"transactionId\":\"3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f\"")
                .contains("\"sourceProvider\":\"DFSP_B\"")
                .contains("\"amount\":1000.00")
                .contains("\"fee\":10.00");
    }

    @Test
    void translatesFailedStatusToUnsuccessfulResult() {
        fakeDfspA.respondWith(200, """
                {"status":"FAILED","referenceId":"A-TXN-0002","message":"Amount exceeds DFSP-A limit"}
                """);

        DfspTransferResult result = adapter.transfer(fakeDfspA.baseUrl(), bToA("60000.00", "600.00"));

        assertThat(result.successful()).isFalse();
        assertThat(result.message()).isEqualTo("Amount exceeds DFSP-A limit");
    }

    @Test
    void rejectsUnknownStatus() {
        fakeDfspA.respondWith(200, "{\"status\":\"PENDING\"}");

        assertThatThrownBy(() -> adapter.transfer(fakeDfspA.baseUrl(), bToA("1000.00", "10.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessage("DFSP-A returned unknown status: PENDING");
    }

    @Test
    void wrapsServerErrorInCommunicationException() {
        fakeDfspA.respondWith(500, "");

        assertThatThrownBy(() -> adapter.transfer(fakeDfspA.baseUrl(), bToA("1000.00", "10.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessageStartingWith("DFSP-A request failed");
    }

    @Test
    void wrapsConnectionFailureInCommunicationException() throws IOException {
        String unreachableUrl = FakeDfspServer.unusedBaseUrl();

        assertThatThrownBy(() -> adapter.transfer(unreachableUrl, bToA("1000.00", "10.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessageStartingWith("DFSP-A request failed");
    }

    private static DfspTransferRequest bToA(String amount, String fee) {
        return new DfspTransferRequest(TRANSACTION_ID, "DFSP_B", new BigDecimal(amount), new BigDecimal(fee));
    }
}
