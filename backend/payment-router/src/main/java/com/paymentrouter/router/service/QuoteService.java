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
 * Business validation is in place. Quote calculation is not implemented yet,
 * so a valid request currently receives 501 Not Implemented.
 */
@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    private final PaymentRequestValidator paymentRequestValidator;

    public QuoteService(PaymentRequestValidator paymentRequestValidator) {
        this.paymentRequestValidator = paymentRequestValidator;
    }

    public QuoteResponse calculateQuote(QuoteRequest request) {
        log.info("Quote request received: {}", request);
        paymentRequestValidator.validate(request.sourceProviderCode(), request.destinationProviderCode());

        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Quote calculation is not implemented yet");
    }
}
