package com.paymentrouter.router.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.paymentrouter.router.client.DfspTransferRequest;
import com.paymentrouter.router.client.DfspTransferResult;
import com.paymentrouter.router.entity.Provider;

/**
 * Strategy Pattern: DFSP-specific behaviour of the router, one implementation per DFSP.
 * <p>
 * Services never check provider codes with if/else. They select the strategy whose
 * {@link #getProviderCode()} matches the destination provider and call it.
 */
public interface DfspStrategy {

    /** Money is handled with 2 decimal places (taka and paisa). */
    int MONEY_SCALE = 2;

    BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * Code of the provider this strategy handles. Must match {@code providers.code}, e.g. {@code DFSP_A}.
     */
    String getProviderCode();

    /**
     * Prices a payment sent to this provider.
     * <p>
     * Default rule shared by all current DFSPs, using the provider's CURRENT fee percentage
     * from the database:
     * <pre>
     * feeAmount   = amount × feePercentage / 100   (2 decimal places, HALF_UP)
     * totalAmount = amount + feeAmount
     * </pre>
     * A DFSP with different pricing rules overrides this method.
     */
    default QuoteCalculation calculateQuote(BigDecimal amount, Provider destination) {
        BigDecimal scaledAmount = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal feePercentage = destination.getFeePercentage();
        BigDecimal feeAmount = scaledAmount.multiply(feePercentage)
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
        return new QuoteCalculation(scaledAmount, feePercentage, feeAmount, scaledAmount.add(feeAmount));
    }

    /**
     * Sends the transfer to this DFSP through the DFSP's adapter.
     *
     * @param request     provider-neutral transfer data
     * @param destination destination provider; its base URL is used for the call
     * @return the DFSP's decision
     * @throws com.paymentrouter.router.exception.DfspCommunicationException if the DFSP cannot be reached
     */
    DfspTransferResult executeTransfer(DfspTransferRequest request, Provider destination);
}
