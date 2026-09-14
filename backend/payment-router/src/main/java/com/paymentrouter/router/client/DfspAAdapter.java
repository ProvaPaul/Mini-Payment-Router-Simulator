package com.paymentrouter.router.client;

import java.math.BigDecimal;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.paymentrouter.router.exception.DfspCommunicationException;

/**
 * Adapter for DFSP-A's HTTP API.
 * <p>
 * DFSP-A API:
 * <pre>
 * POST {baseUrl}/api/dfsp-a/transfers
 * request:  {"transactionId":"...","sourceProvider":"DFSP_B","amount":1000.00,"fee":10.00}   (taka, decimal)
 * response: {"status":"SUCCESS"|"FAILED","referenceId":"...","message":"..."}
 * </pre>
 */
@Component
public class DfspAAdapter implements DfspClient {

    static final String TRANSFER_PATH = "/api/dfsp-a/transfers";

    private final RestClient restClient;

    public DfspAAdapter(RestClient dfspRestClient) {
        this.restClient = dfspRestClient;
    }

    @Override
    public DfspTransferResult transfer(String baseUrl, DfspTransferRequest request) {
        DfspATransferRequest body = new DfspATransferRequest(
                request.transactionId().toString(),
                request.sourceProviderCode(),
                request.amount(),
                request.feeAmount());

        DfspATransferResponse response;
        try {
            response = restClient.post()
                    .uri(baseUrl + TRANSFER_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(DfspATransferResponse.class);
        } catch (RestClientException exception) {
            throw new DfspCommunicationException("DFSP-A request failed: " + exception.getMessage(), exception);
        }

        return toResult(response);
    }

    private DfspTransferResult toResult(DfspATransferResponse response) {
        if (response == null || response.status() == null) {
            throw new DfspCommunicationException("DFSP-A returned an empty response");
        }
        return switch (response.status()) {
            case "SUCCESS" -> new DfspTransferResult(true, response.referenceId(), response.message());
            case "FAILED" -> new DfspTransferResult(false, response.referenceId(), response.message());
            default -> throw new DfspCommunicationException("DFSP-A returned unknown status: " + response.status());
        };
    }

    /** DFSP-A request body. Amounts are decimal taka. */
    record DfspATransferRequest(String transactionId, String sourceProvider, BigDecimal amount, BigDecimal fee) {
    }

    /** DFSP-A response body. Unknown extra fields from DFSP-A are ignored. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DfspATransferResponse(String status, String referenceId, String message) {
    }
}
