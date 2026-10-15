package com.drukconnect.drukconnect.controller.feedback;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.RequestMetadata;

import com.drukconnect.drukconnect.dto.feedback.CreateMicroFeedbackRequest;
import com.drukconnect.drukconnect.dto.feedback.CreatePlatformFeedbackRequest;
import com.drukconnect.drukconnect.dto.feedback.FeedbackSubmissionResponse;

import com.drukconnect.drukconnect.service.feedback.PlatformFeedbackService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feedback")
public class PlatformFeedbackController {

    private final PlatformFeedbackService feedbackService;

    private final ObjectMapper objectMapper;

    private final Validator validator;


    public PlatformFeedbackController(

            PlatformFeedbackService feedbackService,

            ObjectMapper objectMapper,

            Validator validator

    ) {

        this.feedbackService =
                feedbackService;

        this.objectMapper =
                objectMapper;

        this.validator =
                validator;
    }


    /*
     * =========================================================
     * MICRO FEEDBACK
     * =========================================================
     */
    @PostMapping("/micro")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackSubmissionResponse submitMicroFeedback(

            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            CreateMicroFeedbackRequest request,

            HttpServletRequest httpRequest

    ) {

        UUID userId =
                UUID.fromString(
                        jwt.getSubject()
                );


        return feedbackService
                .submitMicroFeedback(

                        userId,

                        request,

                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }


    /*
     * =========================================================
     * FULL FEEDBACK FORM
     * =========================================================
     *
     * POSTMAN:
     *
     * Body -> form-data
     *
     * request      Text
     * screenshot   File
     *
     * No special Content-Type needs to be set on the
     * "request" field.
     * =========================================================
     */
    @PostMapping(
            value = "/form",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackSubmissionResponse submitFeedback(

            @AuthenticationPrincipal
            Jwt jwt,

            /*
             * IMPORTANT:
             *
             * Receive JSON as normal TEXT.
             *
             * This matches your Postman screenshot.
             */
            @RequestParam("request")
            String requestJson,

            /*
             * Optional screenshot.
             */
            @RequestParam(
                    value = "screenshot",
                    required = false
            )
            MultipartFile screenshot,

            HttpServletRequest httpRequest

    ) {

        UUID userId =
                UUID.fromString(
                        jwt.getSubject()
                );


        /*
         * =====================================================
         * CONVERT TEXT JSON -> DTO
         * =====================================================
         */
        CreatePlatformFeedbackRequest request =
                parseFeedbackRequest(
                        requestJson
                );


        /*
         * =====================================================
         * SERVICE
         * =====================================================
         */
        return feedbackService
                .submitFeedback(

                        userId,

                        request,

                        screenshot,

                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }


    /*
     * =========================================================
     * PARSE FEEDBACK JSON
     * =========================================================
     */
    private CreatePlatformFeedbackRequest parseFeedbackRequest(
            String requestJson
    ) {

        if (
                requestJson == null
                        ||
                        requestJson.isBlank()
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "FEEDBACK_REQUEST_REQUIRED",
                    "Feedback request is required"
            );
        }


        CreatePlatformFeedbackRequest request;


        try {

            request =
                    objectMapper.readValue(

                            requestJson,

                            CreatePlatformFeedbackRequest.class
                    );

        } catch (JsonProcessingException ex) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FEEDBACK_REQUEST",
                    "Feedback request contains invalid JSON"
            );
        }


        /*
         * =====================================================
         * MANUAL BEAN VALIDATION
         * =====================================================
         *
         * Because we manually converted String -> DTO,
         * @Valid is not automatically called.
         * =====================================================
         */
        Set<ConstraintViolation<CreatePlatformFeedbackRequest>>
                violations =
                validator.validate(
                        request
                );


        if (
                !violations.isEmpty()
        ) {

            ConstraintViolation<CreatePlatformFeedbackRequest>
                    firstViolation =
                    violations
                            .iterator()
                            .next();


            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FEEDBACK_REQUEST",
                    firstViolation
                            .getPropertyPath()
                            + ": "
                            + firstViolation
                            .getMessage()
            );
        }


        return request;
    }
}