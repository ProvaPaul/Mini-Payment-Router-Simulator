package com.paymentrouter.router.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.paymentrouter.router.dto.QuoteRequest;
import com.paymentrouter.router.dto.QuoteResponse;
import com.paymentrouter.router.entity.Provider;

/**
 * Calculates quotes. A quote is only a price calculation: nothing is saved and no DFSP is called.
 */
@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    /** Money is handled with 2 decimal places (taka and paisa). */
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PaymentRequestValidator paymentRequestValidator;

    public QuoteService(PaymentRequestValidator paymentRequestValidator) {
        this.paymentRequestValidator = paymentRequestValidator;
    }

    public QuoteResponse calculateQuote(QuoteRequest request) {
        log.info("Quote request received: {}", request);

        PaymentProviders providers = paymentRequestValidator.validate(
                request.sourceProviderCode(), request.destinationProviderCode());
        Provider destination = providers.destination();

        BigDecimal amount = request.amount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal feePercentage = destination.getFeePercentage();
        BigDecimal feeAmount = calculateFeeAmount(amount, feePercentage);
        BigDecimal totalAmount = amount.add(feeAmount);

        QuoteResponse response = new QuoteResponse(
                providers.source().getCode(),
                destination.getCode(),
                amount,
                feePercentage,
                feeAmount,
                totalAmount);

        log.info("Quote calculated: {}", response);
        return response;
    }

    /** feeAmount = amount × feePercentage / 100, rounded to 2 decimal places (HALF_UP). */
    private BigDecimal calculateFeeAmount(BigDecimal amount, BigDecimal feePercentage) {
        return amount.multiply(feePercentage).divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
