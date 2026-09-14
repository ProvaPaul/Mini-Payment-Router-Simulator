package com.paymentrouter.router.client;

import java.math.BigDecimal;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.paymentrouter.router.exception.DfspCommunicationException;

/**
 * Adapter for DFSP-B's HTTP API, which differs from DFSP-A in URL, field names,
 * money unit (paisa) and result values.
 * <p>
 * DFSP-B API:
 * <pre>
 * POST {baseUrl}/v1/payments/receive
 * request:  {"clientRef":"...","senderDfsp":"DFSP_A","amountInPaisa":100000,"feeInPaisa":1500}   (paisa, integer)
 * response: {"result":"ACCEPTED"|"REJECTED","paymentRef":"...","reason":"..."}
 * </pre>
 */
@Component
public class DfspBAdapter implements DfspClient {

    static final String PAYMENT_PATH = "/v1/payments/receive";

    private final RestClient restClient;

    public DfspBAdapter(RestClient dfspRestClient) {
        this.restClient = dfspRestClient;
    }

    @Override
    public DfspTransferResult transfer(String baseUrl, DfspTransferRequest request) {
        DfspBPaymentRequest body = new DfspBPaymentRequest(
                request.transactionId().toString(),
                request.sourceProviderCode(),
                toPaisa(request.amount()),
                toPaisa(request.feeAmount()));

        DfspBPaymentResponse response;
        try {
            response = restClient.post()
                    .uri(baseUrl + PAYMENT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(DfspBPaymentResponse.class);
        } catch (RestClientException exception) {
            throw new DfspCommunicationException("DFSP-B request failed: " + exception.getMessage(), exception);
        }

        return toResult(response);
    }

    private DfspTransferResult toResult(DfspBPaymentResponse response) {
        if (response == null || response.result() == null) {
            throw new DfspCommunicationException("DFSP-B returned an empty response");
        }
        String message = response.reason() != null ? response.reason() : response.result();
        return switch (response.result()) {
            case "ACCEPTED" -> new DfspTransferResult(true, response.paymentRef(), message);
            case "REJECTED" -> new DfspTransferResult(false, response.paymentRef(), message);
            default -> throw new DfspCommunicationException("DFSP-B returned unknown result: " + response.result());
        };
    }

    /**
     * Converts taka to paisa: 1000.00 → 100000. Amounts have at most 2 decimal places
     * (request validation), so the conversion is exact.
     */
    private static long toPaisa(BigDecimal taka) {
        return taka.movePointRight(2).longValueExact();
    }

    /** DFSP-B request body. Amounts are whole paisa. */
    record DfspBPaymentRequest(String clientRef, String senderDfsp, long amountInPaisa, long feeInPaisa) {
    }

    /** DFSP-B response body. Unknown extra fields from DFSP-B are ignored. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DfspBPaymentResponse(String result, String paymentRef, String reason) {
    }
}
