package com.paymentrouter.dfspb.controller;

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

import com.paymentrouter.dfspb.service.PaymentService;

/**
 * Contract test: sends exactly the JSON the Payment Router's DfspBAdapter sends and checks
 * the response fields DfspBAdapter reads.
 */
@WebMvcTest(PaymentController.class)
@Import(PaymentService.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsRouterRequestAndAnswersInDfspBFormat() throws Exception {
        mockMvc.perform(post("/v1/payments/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientRef":"3f6e2c1a-8b0d-4c5e-9a4f-1d2b3c4d5e6f",
                                 "senderDfsp":"DFSP_A","amountInPaisa":100000,"feeInPaisa":1500}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("ACCEPTED"))
                .andExpect(jsonPath("$.paymentRef", matchesPattern("B-PAY-[0-9A-F]{8}")))
                .andExpect(jsonPath("$.reason").doesNotExist());
    }

    @Test
    void answersRejectedWithReasonAndHttp200WhenAmountExceedsLimit() throws Exception {
        mockMvc.perform(post("/v1/payments/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientRef":"9a7e1b2c-3d4e-4f50-8a6b-7c8d9e0f1a2b",
                                 "senderDfsp":"DFSP_A","amountInPaisa":3000000,"feeInPaisa":45000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Amount exceeds DFSP-B limit of 2500000 paisa"));
    }

    @Test
    void rejectsMalformedJsonWithHttp400() throws Exception {
        mockMvc.perform(post("/v1/payments/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountInPaisa\":"))
                .andExpect(status().isBadRequest());
    }
}
