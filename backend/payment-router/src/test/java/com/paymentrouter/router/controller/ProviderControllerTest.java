package com.paymentrouter.router.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.paymentrouter.router.entity.Provider;
import com.paymentrouter.router.entity.ProviderStatus;
import com.paymentrouter.router.repository.ProviderRepository;
import com.paymentrouter.router.service.ProviderService;

/**
 * GET /api/providers returns active providers as {code, name} only.
 */
@WebMvcTest(ProviderController.class)
@Import(ProviderService.class)
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderRepository providerRepository;

    @Test
    void returnsActiveProvidersWithoutInternalConfiguration() throws Exception {
        when(providerRepository.findByStatusOrderByCodeAsc(ProviderStatus.ACTIVE)).thenReturn(List.of(
                new Provider("DFSP_A", "DFSP-A", "http://dfsp-a:8081", new BigDecimal("1.00"), ProviderStatus.ACTIVE),
                new Provider("DFSP_B", "DFSP-B", "http://dfsp-b:8082", new BigDecimal("1.50"), ProviderStatus.ACTIVE)));

        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("DFSP_A"))
                .andExpect(jsonPath("$[0].name").value("DFSP-A"))
                .andExpect(jsonPath("$[1].code").value("DFSP_B"))
                .andExpect(jsonPath("$[0].baseUrl").doesNotExist())
                .andExpect(jsonPath("$[0].feePercentage").doesNotExist());
    }
}
