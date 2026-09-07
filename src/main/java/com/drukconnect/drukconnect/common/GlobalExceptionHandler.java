package com.drukconnect.drukconnect.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);


    /*
     * =========================================================
     * CUSTOM API EXCEPTION
     * =========================================================
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(
            ApiException ex,
            HttpServletRequest request
    ) {

        log.warn(
                "API error. code={}, message={}, path={}",
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(ex.getStatus())
                .body(
                        buildResponse(
                                ex.getStatus(),
                                ex.getCode(),
                                ex.getMessage(),
                                request.getRequestURI(),
                                null
                        )
                );
    }


    /*
     * =========================================================
     * @VALID BODY VALIDATION
     * =========================================================
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> fieldErrors =
                new LinkedHashMap<>();


        for (FieldError fieldError :
                ex.getBindingResult().getFieldErrors()) {

            fieldErrors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }


        log.warn(
                "Validation failed for path={} errors={}",
                request.getRequestURI(),
                fieldErrors
        );


        return ResponseEntity
                .badRequest()
                .body(
                        buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "VALIDATION_ERROR",
                                "Request validation failed",
                                request.getRequestURI(),
                                fieldErrors
                        )
                );
    }


    /*
     * =========================================================
     * INVALID JSON
     * =========================================================
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        log.error(
                "Invalid request body on path={}",
                request.getRequestURI(),
                ex
        );


        return ResponseEntity
                .badRequest()
                .body(
                        buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_REQUEST_BODY",
                                getMostSpecificMessage(ex),
                                request.getRequestURI(),
                                null
                        )
                );
    }


    /*
     * =========================================================
     * CONSTRAINT VALIDATION
     * =========================================================
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {

        return ResponseEntity
                .badRequest()
                .body(
                        buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "CONSTRAINT_VIOLATION",
                                ex.getMessage(),
                                request.getRequestURI(),
                                null
                        )
                );
    }


    /*
     * =========================================================
     * DATABASE CONSTRAINT ERROR
     * =========================================================
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {

        String cause =
                getRootCauseMessage(ex);


        log.error(
                "DATABASE ERROR on {}: {}",
                request.getRequestURI(),
                cause,
                ex
        );


        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        buildResponse(
                                HttpStatus.CONFLICT,
                                "DATABASE_CONSTRAINT_ERROR",
                                cause,
                                request.getRequestURI(),
                                null
                        )
                );
    }


    /*
     * =========================================================
     * ILLEGAL ARGUMENT
     * =========================================================
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        log.error(
                "Illegal argument on {}",
                request.getRequestURI(),
                ex
        );


        return ResponseEntity
                .badRequest()
                .body(
                        buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_ARGUMENT",
                                ex.getMessage(),
                                request.getRequestURI(),
                                null
                        )
                );
    }


    /*
     * =========================================================
     * ILLEGAL STATE
     * =========================================================
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {

        log.error(
                "Illegal state on {}",
                request.getRequestURI(),
                ex
        );


        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "SYSTEM_CONFIGURATION_ERROR",
                                ex.getMessage(),
                                request.getRequestURI(),
                                null
                        )
                );
    }


    /*
     * =========================================================
     * GENERIC EXCEPTION
     *
     * IMPORTANT:
     * During development we expose the exact exception.
     * =========================================================
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {

        /*
         * THIS MUST PRINT THE FULL STACKTRACE.
         */
        log.error(
                "============================================"
        );

        log.error(
                "UNEXPECTED EXCEPTION"
        );

        log.error(
                "Request: {} {}",
                request.getMethod(),
                request.getRequestURI()
        );

        log.error(
                "Exception type: {}",
                ex.getClass().getName()
        );

        log.error(
                "Exception message: {}",
                ex.getMessage()
        );

        log.error(
                "Root cause: {}",
                getRootCauseMessage(ex)
        );

        log.error(
                "Full stacktrace:",
                ex
        );

        log.error(
                "============================================"
        );


        Map<String, Object> details =
                new LinkedHashMap<>();

        details.put(
                "exception",
                ex.getClass().getName()
        );

        details.put(
                "exceptionMessage",
                ex.getMessage()
        );

        details.put(
                "rootCause",
                getRootCauseMessage(ex)
        );


        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        buildResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "INTERNAL_ERROR",

                                /*
                                 * DEVELOPMENT ONLY:
                                 * Show real cause in Postman.
                                 */
                                getRootCauseMessage(ex),

                                request.getRequestURI(),
                                details
                        )
                );
    }


    /*
     * =========================================================
     * RESPONSE BUILDER
     * =========================================================
     */
    private Map<String, Object> buildResponse(
            HttpStatus status,
            String code,
            String message,
            String path,
            Object details
    ) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "timestamp",
                Instant.now()
        );

        body.put(
                "status",
                status.value()
        );

        body.put(
                "error",
                status.getReasonPhrase()
        );

        body.put(
                "code",
                code
        );

        body.put(
                "message",
                message != null
                        ? message
                        : "No additional error message available"
        );

        body.put(
                "path",
                path
        );


        if (details != null) {

            body.put(
                    "details",
                    details
            );
        }


        return body;
    }


    /*
     * =========================================================
     * ROOT CAUSE
     * =========================================================
     */
    private String getRootCauseMessage(
            Throwable throwable
    ) {

        if (throwable == null) {
            return "Unknown error";
        }


        Throwable root =
                throwable;


        while (
                root.getCause() != null
                        &&
                        root.getCause() != root
        ) {

            root =
                    root.getCause();
        }


        if (root.getMessage() != null) {
            return root.getMessage();
        }


        return root.getClass().getName();
    }


    private String getMostSpecificMessage(
            Throwable throwable
    ) {

        return getRootCauseMessage(
                throwable
        );
    }
}