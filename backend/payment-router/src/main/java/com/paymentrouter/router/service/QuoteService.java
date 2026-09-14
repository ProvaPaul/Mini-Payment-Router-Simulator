package com.paymentrouter.router.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.paymentrouter.router.dto.QuoteRequest;
import com.paymentrouter.router.dto.QuoteResponse;

/**
 * Business logic for quotes.
 * <p>
 * API foundation only: the request reaches this service, but quote calculation
 * is not implemented yet, so the caller receives 501 Not Implemented.
 */
@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    public QuoteResponse calculateQuote(QuoteRequest request) {
        log.info("Quote request received: {}", request);
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Quote calculation is not implemented yet");
    }
}
