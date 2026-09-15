package com.paymentrouter.router.exception;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.paymentrouter.router.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Converts every error raised while handling a request into the same JSON shape:
 * <pre>
 * {"timestamp":"...","status":400,"error":"Bad Request","message":"...","fieldErrors":{...}}
 * </pre>
 * Client errors (4xx) are logged as WARN. Unexpected errors (500) are logged as ERROR with the
 * stack trace, and the client only receives a generic message.
 * <p>
 * DFSP rejections and DFSP timeouts/unavailability are not handled here: the transfer is still
 * recorded, so they are returned by TransferService as a normal 201 response with status FAILED.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String VALIDATION_FAILED_MESSAGE = "Request validation failed";
    static final String MALFORMED_REQUEST_MESSAGE = "Malformed JSON request";
    static final String UNSUPPORTED_MEDIA_TYPE_MESSAGE = "Content type not supported, use application/json";
    static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";

    /** 400: a field rule failed, e.g. missing provider code, amount not positive, too many decimals. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleRequestValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new TreeMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Request validation failed: {}", fieldErrors);
        return error(HttpStatus.BAD_REQUEST, VALIDATION_FAILED_MESSAGE, fieldErrors);
    }

    /** 400: the body is not valid JSON or a value has the wrong type, e.g. "amount": "abc". */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        log.warn("Unreadable request body: {}", exception.getMostSpecificCause().getMessage());
        return error(HttpStatus.BAD_REQUEST, MALFORMED_REQUEST_MESSAGE, Map.of());
    }

    /** 400: a business rule failed, e.g. same source and destination, provider not found or inactive. */
    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidation(InvalidPaymentRequestException exception) {
        log.warn("Invalid payment request: {}", exception.getMessage());
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    /** 404: no endpoint exists for this URL. */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleUnknownEndpoint(HttpServletRequest request) {
        String message = "Endpoint not found: " + request.getMethod() + " " + request.getRequestURI();
        log.warn(message);
        return error(HttpStatus.NOT_FOUND, message, Map.of());
    }

    /** 405: the endpoint exists but not for this HTTP method, e.g. GET /api/quotes. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        String message = "HTTP method " + exception.getMethod() + " is not supported for this endpoint";
        log.warn(message);
        return error(HttpStatus.METHOD_NOT_ALLOWED, message, Map.of());
    }

    /** 415: the request body is not sent as application/json. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        log.warn("Unsupported content type: {}", exception.getContentType());
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE_MESSAGE, Map.of());
    }

    /** 500: anything not handled above. Details stay in the log and are never sent to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected error while handling request", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_MESSAGE, Map.of());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, Map<String, String> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
