package com.diploma.atsbff.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handleApiException(
        ApiException exception,
        HttpServletRequest request
    ) {
        return buildResponse(exception.getStatus(), exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("Validation failed");

        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(RestClientException.class)
    ResponseEntity<Map<String, Object>> handleUpstream(
        RestClientException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.BAD_GATEWAY,
            "Upstream service is unavailable: " + exception.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(
        Exception exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected server error",
            request.getRequestURI()
        );
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
        HttpStatus status,
        String message,
        String path
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}
