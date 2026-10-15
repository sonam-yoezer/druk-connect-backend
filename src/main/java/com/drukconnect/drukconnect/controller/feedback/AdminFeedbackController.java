package com.drukconnect.drukconnect.controller.feedback;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.RequestMetadata;

import com.drukconnect.drukconnect.dto.feedback.*;

import com.drukconnect.drukconnect.entity.feedback.PlatformFeedback;
import com.drukconnect.drukconnect.enums.feedback.*;

import com.drukconnect.drukconnect.repository.feedback.PlatformFeedbackRepository;
import com.drukconnect.drukconnect.service.feedback.PlatformFeedbackService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/feedback")
public class AdminFeedbackController {

    private final PlatformFeedbackService
            feedbackService;

    @Autowired
    private PlatformFeedbackRepository feedbackRepository;


    public AdminFeedbackController(
            PlatformFeedbackService feedbackService
    ) {

        this.feedbackService =
                feedbackService;
    }


    /*
     * =========================================================
     * ADMIN INBOX + FILTERS
     * =========================================================
     */
    @GetMapping
    public PagedAdminFeedbackResponse getFeedback(

            @RequestParam(required = false)
            FeedbackType type,

            @RequestParam(required = false)
            FeedbackPerspective perspective,

            @RequestParam(required = false)
            FeedbackSource source,

            @RequestParam(required = false)
            FeedbackStatus status,

            /*
             * LISTER or BUYER
             */
            @RequestParam(required = false)
            String submittedAs,

            @RequestParam(required = false)
            Boolean anonymous,

            @RequestParam(
                    defaultValue = "false"
            )
            boolean unresolved,

            @RequestParam(
                    defaultValue = "1"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size

    ) {

        return feedbackService
                .getFeedback(
                        type,
                        perspective,
                        source,
                        status,
                        submittedAs,
                        anonymous,
                        unresolved,
                        page,
                        size
                );
    }


    /*
     * =========================================================
     * DASHBOARD COUNTS / THEMES
     * =========================================================
     */
    @GetMapping("/dashboard")
    public FeedbackDashboardResponse dashboard() {

        return feedbackService
                .dashboard();
    }


    /*
     * =========================================================
     * NEW / IN_PROGRESS / RESOLVED
     * =========================================================
     */
    @PatchMapping(
            "/{feedbackId}/status"
    )
    public AdminFeedbackItemResponse updateStatus(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID feedbackId,

            @Valid
            @RequestBody
            UpdateFeedbackStatusRequest request,

            HttpServletRequest httpRequest

    ) {

        UUID adminUserId =
                UUID.fromString(
                        jwt.getSubject()
                );


        return feedbackService
                .updateStatus(
                        adminUserId,
                        feedbackId,
                        request,
                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }


    /*
     * =========================================================
     * ASSIGN RECURRING THEME
     * =========================================================
     */
    @PatchMapping(
            "/{feedbackId}/theme"
    )
    public AdminFeedbackItemResponse updateTheme(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID feedbackId,

            @Valid
            @RequestBody
            UpdateFeedbackThemeRequest request,

            HttpServletRequest httpRequest

    ) {

        UUID adminUserId =
                UUID.fromString(
                        jwt.getSubject()
                );


        return feedbackService
                .updateTheme(
                        adminUserId,
                        feedbackId,
                        request,
                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }

    @GetMapping("/{feedbackId}/image")
    public ResponseEntity<byte[]> getFeedbackImage(
            @PathVariable UUID feedbackId
    ) {

        PlatformFeedback feedback =
                feedbackRepository
                        .findById(feedbackId)
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "FEEDBACK_NOT_FOUND",
                                        "Feedback was not found"
                                )
                        );


        if (
                feedback.getScreenshotPath() == null
                        ||
                        feedback.getScreenshotPath().isBlank()
        ) {

            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "FEEDBACK_IMAGE_NOT_FOUND",
                    "No image is attached to this feedback"
            );
        }


        try {

            Path imagePath =
                    Path.of(
                            feedback.getScreenshotPath()
                    );


            if (
                    !Files.exists(imagePath)
            ) {

                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "FEEDBACK_IMAGE_NOT_FOUND",
                        "Feedback image was not found"
                );
            }


            byte[] imageBytes =
                    Files.readAllBytes(
                            imagePath
                    );


            String contentType =
                    feedback.getScreenshotContentType();


            if (
                    contentType == null
                            ||
                            contentType.isBlank()
            ) {

                contentType =
                        Files.probeContentType(
                                imagePath
                        );
            }


            if (
                    contentType == null
            ) {

                contentType =
                        MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }


            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.parseMediaType(
                                    contentType
                            )
                    )
                    .body(
                            imageBytes
                    );


        } catch (IOException ex) {

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FEEDBACK_IMAGE_READ_FAILED",
                    "Unable to read feedback image"
            );
        }
    }
}