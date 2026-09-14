package com.paymentrouter.router.exception;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.paymentrouter.router.dto.ErrorResponse;

/**
 * Converts validation failures from any controller into a consistent HTTP 400 JSON body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Request validation: a field annotation (@NotBlank, @NotNull, @Positive) failed. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleRequestValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new TreeMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return badRequest("Request validation failed", fieldErrors);
    }

    /** Business validation: same providers, unknown provider or inactive provider. */
    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidation(InvalidPaymentRequestException exception) {
        return badRequest(exception.getMessage(), Map.of());
    }

    private ResponseEntity<ErrorResponse> badRequest(String message, Map<String, String> fieldErrors) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse body = new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
