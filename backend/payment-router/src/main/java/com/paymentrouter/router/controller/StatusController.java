package com.paymentrouter.router.controller;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentrouter.router.dto.StatusResponse;

/**
 * Minimal status endpoint used to check that the Payment Router is running.
 */
@RestController
public class StatusController {

    private static final String STATUS_UP = "UP";

    private final String applicationName;

    public StatusController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping("/api/status")
    public StatusResponse getStatus() {
        return new StatusResponse(applicationName, STATUS_UP, Instant.now());
    }
}
