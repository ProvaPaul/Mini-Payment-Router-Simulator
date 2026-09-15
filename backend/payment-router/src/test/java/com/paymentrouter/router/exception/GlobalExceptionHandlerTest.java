package com.paymentrouter.router.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.paymentrouter.router.controller.QuoteController;
import com.paymentrouter.router.controller.TransferController;
import com.paymentrouter.router.service.QuoteService;
import com.paymentrouter.router.service.TransferService;

/**
 * Web-layer test of the JSON error contract. Services are mocked; no database or DFSP is needed.
 * Every error must have the same shape: timestamp, status, error, message, fieldErrors.
 */
@WebMvcTest(controllers = {QuoteController.class, TransferController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuoteService quoteService;

    @MockitoBean
    private TransferService transferService;

    @Test
    void invalidAmountReturns400WithFieldError() throws Exception {
        postJson("/api/quotes", """
                {"sourceProviderCode":"DFSP_A","destinationProviderCode":"DFSP_B","amount":0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(consistentShape(400, "Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.amount").value("amount must be greater than zero"));
    }

    @Test
    void amountTooLargeToStoreReturns400WithFieldError() throws Exception {
        postJson("/api/transfers", """
                {"sourceProviderCode":"DFSP_A","destinationProviderCode":"DFSP_B","amount":9999999999.99}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(consistentShape(400, "Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.amount").value("amount must have at most 9 digits and 2 decimal places"));
    }

    @Test
    void nonNumericAmountReturns400MalformedRequest() throws Exception {
        postJson("/api/transfers", """
                {"sourceProviderCode":"DFSP_A","destinationProviderCode":"DFSP_B","amount":"abc"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(consistentShape(400, "Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void missingProviderReturns400WithFieldError() throws Exception {
        postJson("/api/transfers", """
                {"destinationProviderCode":"DFSP_B","amount":1000}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(consistentShape(400, "Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.sourceProviderCode").value("sourceProviderCode is required"));
    }

    @Test
    void sameSourceAndDestinationReturns400() throws Exception {
        when(transferService.executeTransfer(any()))
                .thenThrow(new InvalidPaymentRequestException("Source and destination provider cannot be the same"));

        postJson("/api/transfers", """
                {"sourceProviderCode":"DFSP_A","destinationProviderCode":"DFSP_A","amount":1000}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(consistentShape(400, "Bad Request"))
                .andExpect(jsonPath("$.message").value("Source and destination provider cannot be the same"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void providerNotFoundReturns400() throws Exception {
        when(quoteService.calculateQuote(any()))
                .thenThrow(new InvalidPaymentRequestException("Provider not found: DFSP_X"));

        postJson("/api/quotes", """
                {"sourceProviderCode":"DFSP_A","destinationProviderCode":"DFSP_X","amount":1000}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(consistentShape(400, "Bad Request"))
                .andExpect(jsonPath("$.message").value("Provider not found: DFSP_X"));
    }

    @Test
    void unknownEndpointReturns404() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(consistentShape(404, "Not Found"))
                .andExpect(jsonPath("$.message").value("Endpoint not found: GET /api/unknown"));
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(get("/api/quotes"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(consistentShape(405, "Method Not Allowed"))
                .andExpect(jsonPath("$.message").value("HTTP method GET is not supported for this endpoint"));
    }

    @Test
    void wrongContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/quotes").contentType(MediaType.TEXT_PLAIN).content("amount=1000"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(consistentShape(415, "Unsupported Media Type"))
                .andExpect(jsonPath("$.message").value("Content type not supported, use application/json"));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        when(quoteService.calculateQuote(any()))
                .thenThrow(new IllegalStateException("No DFSP strategy registered for provider DFSP_C"));

        postJson("/api/quotes", """
                {"sourceProviderCode":"DFSP_A","destinationProviderCode":"DFSP_C","amount":1000}
                """)
                .andExpect(status().isInternalServerError())
                .andExpect(consistentShape(500, "Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("DFSP_C"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** The fields every error response must contain. */
    private static org.springframework.test.web.servlet.ResultMatcher consistentShape(int status, String error) {
        return result -> {
            jsonPath("$.timestamp").exists().match(result);
            jsonPath("$.status").value(status).match(result);
            jsonPath("$.error").value(error).match(result);
            jsonPath("$.message").exists().match(result);
            jsonPath("$.fieldErrors").exists().match(result);
        };
    }
}
