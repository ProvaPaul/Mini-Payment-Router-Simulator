package com.paymentrouter.router.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentrouter.router.dto.QuoteRequest;
import com.paymentrouter.router.dto.QuoteResponse;
import com.paymentrouter.router.service.QuoteService;

import jakarta.validation.Valid;

/**
 * HTTP entry point for quotes. Delegates all work to {@link QuoteService}.
 */
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /** POST /api/quotes: calculates fee and total for a transfer without executing it. */
    @PostMapping
    public QuoteResponse calculateQuote(@Valid @RequestBody QuoteRequest request) {
        return quoteService.calculateQuote(request);
    }
}
