package com.paymentrouter.router.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
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
 * Verifies DFSP-A's wire format without a running DFSP: MockRestServiceServer
 * intercepts the HTTP call, checks the JSON the adapter sends and returns a canned response.
 */
class DfspAAdapterTest {

    private static final String BASE_URL = "http://dfsp-a.test";
    private static final UUID TRANSACTION_ID = UUID.fromString("3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f");

    private MockRestServiceServer server;
    private DfspAAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new DfspAAdapter(builder.build());
    }

    @Test
    void sendsDfspAFormatAndTranslatesSuccess() {
        server.expect(requestTo(BASE_URL + "/api/dfsp-a/transfers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"transactionId":"3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f",
                         "sourceProvider":"DFSP_B","amount":1000.00,"fee":10.00}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess("""
                        {"status":"SUCCESS","referenceId":"A-TXN-0001","message":"Transfer completed",
                         "processedAt":"2026-09-14T10:01:12Z"}
                        """, MediaType.APPLICATION_JSON));

        DfspTransferResult result = adapter.transfer(BASE_URL, bToA("1000.00", "10.00"));

        assertThat(result.successful()).isTrue();
        assertThat(result.providerReference()).isEqualTo("A-TXN-0001");
        assertThat(result.message()).isEqualTo("Transfer completed");
        server.verify();
    }

    @Test
    void translatesFailedStatusToUnsuccessfulResult() {
        server.expect(requestTo(BASE_URL + "/api/dfsp-a/transfers"))
                .andRespond(withSuccess("""
                        {"status":"FAILED","referenceId":"A-TXN-0002","message":"Amount exceeds DFSP-A limit"}
                        """, MediaType.APPLICATION_JSON));

        DfspTransferResult result = adapter.transfer(BASE_URL, bToA("60000.00", "600.00"));

        assertThat(result.successful()).isFalse();
        assertThat(result.message()).isEqualTo("Amount exceeds DFSP-A limit");
    }

    @Test
    void rejectsUnknownStatus() {
        server.expect(requestTo(BASE_URL + "/api/dfsp-a/transfers"))
                .andRespond(withSuccess("{\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.transfer(BASE_URL, bToA("1000.00", "10.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessage("DFSP-A returned unknown status: PENDING");
    }

    @Test
    void wrapsServerErrorInCommunicationException() {
        server.expect(requestTo(BASE_URL + "/api/dfsp-a/transfers")).andRespond(withServerError());

        assertThatThrownBy(() -> adapter.transfer(BASE_URL, bToA("1000.00", "10.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessageStartingWith("DFSP-A request failed");
    }

    @Test
    void wrapsConnectionFailureInCommunicationException() {
        server.expect(requestTo(BASE_URL + "/api/dfsp-a/transfers"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> adapter.transfer(BASE_URL, bToA("1000.00", "10.00")))
                .isInstanceOf(DfspCommunicationException.class)
                .hasMessageContaining("Connection refused");
    }

    private static DfspTransferRequest bToA(String amount, String fee) {
        return new DfspTransferRequest(TRANSACTION_ID, "DFSP_B", new BigDecimal(amount), new BigDecimal(fee));
    }
}
