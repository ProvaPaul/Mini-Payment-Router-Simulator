package com.paymentrouter.router.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.paymentrouter.router.exception.DfspCommunicationException;

/**
 * Verifies DFSP-B's wire format (different URL, field names, paisa, result values)
 * without a running DFSP.
 */
class DfspBAdapterTest {

    private static final String BASE_URL = "http://dfsp-b.test";
    private static final UUID TRANSACTION_ID = UUID.fromString("3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f");

    private MockRestServiceServer server;
    private DfspBAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new DfspBAdapter(builder.build());
    }

    @Test
    void sendsDfspBFormatInPaisaAndTranslatesAccepted() {
        server.expect(requestTo(BASE_URL + "/v1/payments/receive"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"clientRef":"3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f",
                         "senderDfsp":"DFSP_A","amountInPaisa":100000,"feeInPaisa":1500}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess("""
                        {"result":"ACCEPTED","paymentRef":"B-PAY-000123"}
                        """, MediaType.APPLICATION_JSON));

        DfspTransferResult result = adapter.transfer(BASE_URL, aToB("1000.00", "15.00"));

        assertThat(result.successful()).isTrue();
        assertThat(result.providerReference()).isEqualTo("B-PAY-000123");
        assertThat(result.message()).isEqualTo("ACCEPTED");
        server.verify();
    }

    @Test
    void convertsDecimalTakaToWholePaisa() {
        server.expect(requestTo(BASE_URL + "/v1/payments/receive"))
                .andExpect(content().json("""
                        {"clientRef":"3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f",
                         "senderDfsp":"DFSP_A","amountInPaisa":33333,"feeInPaisa":500}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess("{\"result\":\"ACCEPTED\",\"paymentRef\":\"B-PAY-000124\"}",
                        MediaType.APPLICATION_JSON));

        DfspTransferResult result = adapter.transfer(BASE_URL, aToB("333.33", "5.00"));

        assertThat(result.successful()).isTrue();
        server.verify();
    }

    @Test
    void translatesRejectedResultToUnsuccessfulResult() {
        server.expect(requestTo(BASE_URL + "/v1/payments/receive"))
                .andRespond(withSuccess("""
                        {"result":"REJECTED","paymentRef":"B-PAY-000125","reason":"Amount exceeds DFSP-B limit"}
                        """, MediaType.APPLICATION_JSON));

        DfspTransferResult result = adapter.transfer(BASE_URL, aToB("30000.00", "450.00"));

        assertThat(result.successful()).isFalse();
        assertThat(result.providerReference()).isEqualTo("B-PAY-000125");
        assertThat(result.message()).isEqualTo("Amount exceeds DFSP-B limit");
    }

    @Test
    void rejectsUnknownResult() {
        server.expect(requestTo(BASE_URL + "/v1/payments/receive"))
                .andRespond(withSuccess("{\"result\":\"SUCCESS\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.transfer(BASE_URL, aToB("1000.00", "15.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessage("DFSP-B returned unknown result: SUCCESS");
    }

    @Test
    void wrapsConnectionFailureInCommunicationException() {
        server.expect(requestTo(BASE_URL + "/v1/payments/receive"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> adapter.transfer(BASE_URL, aToB("1000.00", "15.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessageContaining("Connection refused");
    }

    private static DfspTransferRequest aToB(String amount, String fee) {
        return new DfspTransferRequest(TRANSACTION_ID, "DFSP_A", new BigDecimal(amount), new BigDecimal(fee));
    }
}
