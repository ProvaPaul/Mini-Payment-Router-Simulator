package com.paymentrouter.router.config;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.paymentrouter.router.controller.QuoteController;
import com.paymentrouter.router.dto.QuoteResponse;
import com.paymentrouter.router.service.QuoteService;

/**
 * The browser's CORS checks as MockMvc requests: an allowed local frontend origin gets the
 * Access-Control-* headers, an unknown origin is refused.
 */
@WebMvcTest(QuoteController.class)
@Import(CorsConfig.class)
class CorsConfigTest {

    private static final String LOCAL_FRONTEND = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuoteService quoteService;

    @Test
    void allowsPreflightFromLocalFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/quotes")
                        .header("Origin", LOCAL_FRONTEND)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", LOCAL_FRONTEND))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")));
    }

    @Test
    void rejectsPreflightFromUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/quotes")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void addsAllowOriginHeaderToActualApiResponse() throws Exception {
        when(quoteService.calculateQuote(any())).thenReturn(new QuoteResponse("DFSP_A", "DFSP_B",
                new BigDecimal("1000.00"), new BigDecimal("1.50"), new BigDecimal("15.00"), new BigDecimal("1015.00")));

        mockMvc.perform(post("/api/quotes")
                        .header("Origin", LOCAL_FRONTEND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceProviderCode\":\"DFSP_A\",\"destinationProviderCode\":\"DFSP_B\",\"amount\":1000}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", LOCAL_FRONTEND));
    }
}
