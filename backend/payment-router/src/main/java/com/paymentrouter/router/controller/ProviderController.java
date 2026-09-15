package com.paymentrouter.router.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentrouter.router.dto.ProviderResponse;
import com.paymentrouter.router.service.ProviderService;

/**
 * Lists providers for the frontend's source and destination dropdowns.
 */
@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    /** GET /api/providers: active providers as {code, name}. */
    @GetMapping
    public List<ProviderResponse> getActiveProviders() {
        return providerService.getActiveProviders();
    }
}
