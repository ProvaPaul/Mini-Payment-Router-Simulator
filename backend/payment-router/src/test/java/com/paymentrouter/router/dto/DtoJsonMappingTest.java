package com.paymentrouter.router.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import com.paymentrouter.router.entity.TransactionStatus;

/**
 * Verifies the JSON contract of the API DTOs using the same Jackson configuration
 * Spring Boot uses for real HTTP requests and responses. No database or web server is started.
 */
@JsonTest
class DtoJsonMappingTest {

    @Autowired
    private JacksonTester<QuoteRequest> quoteRequestJson;

    @Autowired
    private JacksonTester<QuoteResponse> quoteResponseJson;

    @Autowired
    private JacksonTester<TransferRequest> transferRequestJson;

    @Autowired
    private JacksonTester<TransferResponse> transferResponseJson;

    @Test
    void quoteRequestIsReadFromJson() throws Exception {
        String json = """
                {"sourceProviderCode":"DFSP_A","destinationProviderCode":"DFSP_B","amount":1000.00}
                """;

        QuoteRequest request = quoteRequestJson.parseObject(json);

        assertThat(request.sourceProviderCode()).isEqualTo("DFSP_A");
        assertThat(request.destinationProviderCode()).isEqualTo("DFSP_B");
        assertThat(request.amount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void quoteResponseIsWrittenAsJson() throws Exception {
        QuoteResponse response = new QuoteResponse(
                "DFSP_A", "DFSP_B",
                new BigDecimal("1000.00"), new BigDecimal("1.50"),
                new BigDecimal("15.00"), new BigDecimal("1015.00"));

        String json = quoteResponseJson.write(response).getJson();

        assertThat(json)
                .contains("\"sourceProviderCode\":\"DFSP_A\"")
                .contains("\"destinationProviderCode\":\"DFSP_B\"")
                .contains("\"amount\":1000.00")
                .contains("\"feePercentage\":1.50")
                .contains("\"feeAmount\":15.00")
                .contains("\"totalAmount\":1015.00");
    }

    @Test
    void transferRequestIgnoresFieldsTheClientMustNotControl() throws Exception {
        String json = """
                {"sourceProviderCode":"DFSP_A","destinationProviderCode":"DFSP_B","amount":1000.00,
                 "feeAmount":0.00,"status":"SUCCESS"}
                """;

        TransferRequest request = transferRequestJson.parseObject(json);

        assertThat(request.sourceProviderCode()).isEqualTo("DFSP_A");
        assertThat(request.destinationProviderCode()).isEqualTo("DFSP_B");
        assertThat(request.amount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void transferResponseIsWrittenAsJson() throws Exception {
        UUID transactionId = UUID.fromString("3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f");
        TransferResponse response = new TransferResponse(
                transactionId, "DFSP_A", "DFSP_B",
                new BigDecimal("1000.00"), new BigDecimal("1.50"),
                new BigDecimal("15.00"), new BigDecimal("1015.00"),
                TransactionStatus.SUCCESS, "Accepted by DFSP-B",
                Instant.parse("2026-09-14T10:01:12Z"));

        String json = transferResponseJson.write(response).getJson();

        assertThat(json)
                .contains("\"transactionId\":\"3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f\"")
                .contains("\"feePercentage\":1.50")
                .contains("\"feeAmount\":15.00")
                .contains("\"totalAmount\":1015.00")
                .contains("\"status\":\"SUCCESS\"")
                .contains("\"message\":\"Accepted by DFSP-B\"")
                .contains("\"createdAt\":\"2026-09-14T10:01:12Z\"")
                .doesNotContain("\"id\"");
    }
}
