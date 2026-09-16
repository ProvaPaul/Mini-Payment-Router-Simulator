package com.paymentrouter.router.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.paymentrouter.router.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Unit test of the JSON error contract: calls each {@code @ExceptionHandler} method directly
 * with a manually built exception and checks the {@link ErrorResponse} it returns. No MockMvc,
 * no HTTP dispatch and no {@code @Valid} pipeline — this tests the mapping logic
 * (exception → status/message/fieldErrors) in isolation, which is what the handler actually does.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRequestValidationBuildsSortedFieldErrorsFromBindingResult() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "sourceProviderCode", "sourceProviderCode is required"));
        bindingResult.addError(new FieldError("request", "amount", "amount must be greater than zero"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleRequestValidation(exception);

        assertConsistentShape(response, 400, "Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Request validation failed");
        assertThat(response.getBody().fieldErrors())
                .containsEntry("amount", "amount must be greater than zero")
                .containsEntry("sourceProviderCode", "sourceProviderCode is required");
        // TreeMap: keys come back in alphabetical order regardless of the order errors were added.
        assertThat(response.getBody().fieldErrors().keySet()).containsExactly("amount", "sourceProviderCode");
    }

    @Test
    void handleUnreadableBodyReturnsMalformedRequestMessage() {
        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException("JSON parse error", (org.springframework.http.HttpInputMessage) null);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(exception);

        assertConsistentShape(response, 400, "Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Malformed JSON request");
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    @Test
    void handleBusinessValidationReturnsTheExceptionMessageAsIs() {
        InvalidPaymentRequestException exception = new InvalidPaymentRequestException("Provider not found: DFSP_X");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessValidation(exception);

        assertConsistentShape(response, 400, "Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Provider not found: DFSP_X");
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    @Test
    void handleUnknownEndpointIncludesMethodAndPath() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/unknown");

        ResponseEntity<ErrorResponse> response = handler.handleUnknownEndpoint(request);

        assertConsistentShape(response, 404, "Not Found");
        assertThat(response.getBody().message()).isEqualTo("Endpoint not found: GET /api/unknown");
    }

    @Test
    void handleMethodNotSupportedNamesTheMethod() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("GET");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(exception);

        assertConsistentShape(response, 405, "Method Not Allowed");
        assertThat(response.getBody().message()).isEqualTo("HTTP method GET is not supported for this endpoint");
    }

    @Test
    void handleUnsupportedMediaTypeReturnsFixedMessage() {
        HttpMediaTypeNotSupportedException exception =
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ErrorResponse> response = handler.handleUnsupportedMediaType(exception);

        assertConsistentShape(response, 415, "Unsupported Media Type");
        assertThat(response.getBody().message()).isEqualTo("Content type not supported, use application/json");
    }

    @Test
    void handleUnexpectedHidesInternalDetails() {
        IllegalStateException exception = new IllegalStateException("No DFSP strategy registered for provider DFSP_C");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(exception);

        assertConsistentShape(response, 500, "Internal Server Error");
        // The client only ever gets the generic message; the real cause stays in the log.
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    /** Every error response must have this shape, whatever the exception. */
    private static void assertConsistentShape(ResponseEntity<ErrorResponse> response, int status, String error) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(status));
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.status()).isEqualTo(status);
        assertThat(body.error()).isEqualTo(error);
        assertThat(body.message()).isNotNull();
        assertThat(body.fieldErrors()).isNotNull();
    }
}
