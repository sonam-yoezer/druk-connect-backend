package com.drukconnect.drukconnect.controller.moderation;

import com.drukconnect.drukconnect.common.RequestMetadata;

import com.drukconnect.drukconnect.dto.moderation
        .CreateListerScamReportRequest;

import com.drukconnect.drukconnect.dto.moderation
        .ListerScamReportResponse;

import com.drukconnect.drukconnect.service.moderation
        .ListerScamModerationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
public class ListerScamReportController {

    private final ListerScamModerationService
            scamModerationService;


    public ListerScamReportController(
            ListerScamModerationService scamModerationService
    ) {

        this.scamModerationService =
                scamModerationService;
    }


    /*
     * User clicks REPORT SCAM from listing page.
     *
     * The listing identifies the LISTER.
     *
     * Report itself is against LISTER account.
     */
    @PostMapping("/{listingId}/report-scam")
    @ResponseStatus(HttpStatus.CREATED)
    public ListerScamReportResponse reportScam(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID listingId,

            @Valid
            @RequestBody
            CreateListerScamReportRequest request,

            HttpServletRequest httpRequest

    ) {

        UUID reporterUserId =
                UUID.fromString(
                        jwt.getSubject()
                );


        return scamModerationService
                .reportLister(
                        reporterUserId,
                        listingId,
                        request,
                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }
}