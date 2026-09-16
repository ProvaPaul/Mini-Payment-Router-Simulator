package com.paymentrouter.router.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Unit test of the field validation rules on {@link TransferRequest} (previously exercised
 * indirectly by sending JSON through MockMvc and letting {@code @Valid} fire).
 * <p>
 * This validates the DTO directly with the same Bean Validation engine Spring uses internally,
 * without any HTTP request, controller, or {@code @Valid} pipeline — the annotations and their
 * exact messages are tested in isolation. {@link QuoteRequest} carries the identical annotations
 * and is not re-tested separately.
 */
class PaymentRequestValidationTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    @Test
    void validRequestHasNoViolations() {
        TransferRequest request = new TransferRequest("DFSP_A", "DFSP_B", new BigDecimal("1000.00"));

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void blankSourceProviderCodeFailsValidation() {
        TransferRequest request = new TransferRequest("", "DFSP_B", new BigDecimal("1000.00"));

        assertThat(messagesFor(request, "sourceProviderCode")).containsExactly("sourceProviderCode is required");
    }

    @Test
    void blankDestinationProviderCodeFailsValidation() {
        TransferRequest request = new TransferRequest("DFSP_A", "", new BigDecimal("1000.00"));

        assertThat(messagesFor(request, "destinationProviderCode"))
                .containsExactly("destinationProviderCode is required");
    }

    @Test
    void missingAmountFailsValidation() {
        TransferRequest request = new TransferRequest("DFSP_A", "DFSP_B", null);

        assertThat(messagesFor(request, "amount")).containsExactly("amount is required");
    }

    @Test
    void zeroOrNegativeAmountFailsValidation() {
        TransferRequest zero = new TransferRequest("DFSP_A", "DFSP_B", new BigDecimal("0"));
        TransferRequest negative = new TransferRequest("DFSP_A", "DFSP_B", new BigDecimal("-50"));

        assertThat(messagesFor(zero, "amount")).containsExactly("amount must be greater than zero");
        assertThat(messagesFor(negative, "amount")).containsExactly("amount must be greater than zero");
    }

    /**
     * Regression test: an amount with 10+ integer digits used to reach the database and overflow
     * the {@code NUMERIC(12,2)} columns once the fee was added, causing an HTTP 500. The limit was
     * lowered to 9 integer digits so this is now rejected here, before any calculation happens.
     */
    @Test
    void amountWithTooManyIntegerDigitsFailsValidation() {
        TransferRequest request = new TransferRequest("DFSP_A", "DFSP_B", new BigDecimal("9999999999.99"));

        assertThat(messagesFor(request, "amount"))
                .containsExactly("amount must have at most 9 digits and 2 decimal places");
    }

    private static Set<String> messagesFor(TransferRequest request, String property) {
        return VALIDATOR.validate(request).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals(property))
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }
}
