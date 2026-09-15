package com.paymentrouter.dfspa.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.paymentrouter.dfspa.service.TransferService;

/**
 * Contract test: sends exactly the JSON the Payment Router's DfspAAdapter sends and checks
 * the response has exactly the fields DfspAAdapter reads.
 */
@WebMvcTest(TransferController.class)
@Import(TransferService.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsRouterRequestAndAnswersInDfspAFormat() throws Exception {
        mockMvc.perform(post("/api/dfsp-a/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionId":"3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f",
                                 "sourceProvider":"DFSP_B","amount":1000.00,"fee":10.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.referenceId", matchesPattern("A-TXN-[0-9A-F]{8}")))
                .andExpect(jsonPath("$.message").value("Transfer completed"));
    }

    @Test
    void answersFailedWithHttp200WhenAmountExceedsLimit() throws Exception {
        mockMvc.perform(post("/api/dfsp-a/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transactionId":"3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f",
                                 "sourceProvider":"DFSP_B","amount":60000.00,"fee":600.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("Amount exceeds DFSP-A limit of 50000.00"));
    }

    @Test
    void rejectsMalformedJsonWithHttp400() throws Exception {
        mockMvc.perform(post("/api/dfsp-a/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":"))
                .andExpect(status().isBadRequest());
    }
}
