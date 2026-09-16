package com.paymentrouter.router.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.paymentrouter.router.exception.DfspCommunicationException;

/**
 * Verifies DFSP-B's wire format (different URL, field names, paisa, result values) against a
 * real, tiny local HTTP server ({@link FakeDfspServer}), not MockRestServiceServer.
 */
class DfspBAdapterTest {

    private static final UUID TRANSACTION_ID = UUID.fromString("3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f");

    private FakeDfspServer fakeDfspB;
    private DfspBAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        fakeDfspB = new FakeDfspServer("/v1/payments/receive");
        adapter = new DfspBAdapter(RestClient.create());
    }

    @AfterEach
    void tearDown() {
        fakeDfspB.stop();
    }

    @Test
    void sendsDfspBFormatInPaisaAndTranslatesAccepted() {
        fakeDfspB.respondWith(200, """
                {"result":"ACCEPTED","paymentRef":"B-PAY-000123"}
                """);

        DfspTransferResult result = adapter.transfer(fakeDfspB.baseUrl(), aToB("1000.00", "15.00"));

        assertThat(result.successful()).isTrue();
        assertThat(result.providerReference()).isEqualTo("B-PAY-000123");
        assertThat(result.message()).isEqualTo("ACCEPTED");
        assertThat(fakeDfspB.requestCount()).isEqualTo(1);
        assertThat(fakeDfspB.lastRequestBody())
                .contains("\"clientRef\":\"3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f\"")
                .contains("\"senderDfsp\":\"DFSP_A\"")
                .contains("\"amountInPaisa\":100000")
                .contains("\"feeInPaisa\":1500");
    }

    @Test
    void convertsDecimalTakaToWholePaisa() {
        fakeDfspB.respondWith(200, "{\"result\":\"ACCEPTED\",\"paymentRef\":\"B-PAY-000124\"}");

        DfspTransferResult result = adapter.transfer(fakeDfspB.baseUrl(), aToB("333.33", "5.00"));

        assertThat(result.successful()).isTrue();
        assertThat(fakeDfspB.lastRequestBody())
                .contains("\"amountInPaisa\":33333")
                .contains("\"feeInPaisa\":500");
    }

    @Test
    void translatesRejectedResultToUnsuccessfulResult() {
        fakeDfspB.respondWith(200, """
                {"result":"REJECTED","paymentRef":"B-PAY-000125","reason":"Amount exceeds DFSP-B limit"}
                """);

        DfspTransferResult result = adapter.transfer(fakeDfspB.baseUrl(), aToB("30000.00", "450.00"));

        assertThat(result.successful()).isFalse();
        assertThat(result.providerReference()).isEqualTo("B-PAY-000125");
        assertThat(result.message()).isEqualTo("Amount exceeds DFSP-B limit");
    }

    @Test
    void rejectsUnknownResult() {
        fakeDfspB.respondWith(200, "{\"result\":\"SUCCESS\"}");

        assertThatThrownBy(() -> adapter.transfer(fakeDfspB.baseUrl(), aToB("1000.00", "15.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessage("DFSP-B returned unknown result: SUCCESS");
    }

    @Test
    void wrapsConnectionFailureInCommunicationException() throws IOException {
        String unreachableUrl = FakeDfspServer.unusedBaseUrl();

        assertThatThrownBy(() -> adapter.transfer(unreachableUrl, aToB("1000.00", "15.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessageStartingWith("DFSP-B request failed");
    }

    @Test
    void keepsTimeoutAsRootCauseOfCommunicationException() {
        SimpleClientHttpRequestFactory shortTimeoutFactory = new SimpleClientHttpRequestFactory();
        shortTimeoutFactory.setConnectTimeout(Duration.ofMillis(500));
        shortTimeoutFactory.setReadTimeout(Duration.ofMillis(200));
        DfspBAdapter shortTimeoutAdapter = new DfspBAdapter(RestClient.builder().requestFactory(shortTimeoutFactory).build());
        // Responds, but only after the adapter's read timeout has already expired.
        fakeDfspB.respondAfterDelay(200, "{\"result\":\"ACCEPTED\",\"paymentRef\":\"B-PAY-000126\"}", 600);

        assertThatThrownBy(() -> shortTimeoutAdapter.transfer(fakeDfspB.baseUrl(), aToB("1000.00", "15.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
    }

    private static DfspTransferRequest aToB(String amount, String fee) {
        return new DfspTransferRequest(TRANSACTION_ID, "DFSP_A", new BigDecimal(amount), new BigDecimal(fee));
    }
}
