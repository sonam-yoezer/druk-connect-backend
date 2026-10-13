package com.drukconnect.drukconnect.controller.moderation;

import com.drukconnect.drukconnect.common.RequestMetadata;

import com.drukconnect.drukconnect.dto.moderation
        .CreateListerScamAppealRequest;

import com.drukconnect.drukconnect.dto.moderation
        .ListerRestrictionStatusResponse;

import com.drukconnect.drukconnect.dto.moderation
        .ListerScamAppealResponse;

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
@RequestMapping("/api/v1/lister-restrictions")
public class ListerRestrictionController {

    private final ListerScamModerationService
            scamModerationService;


    public ListerRestrictionController(
            ListerScamModerationService scamModerationService
    ) {

        this.scamModerationService =
                scamModerationService;
    }


    /*
     * Frontend checks after normal login.
     */
    @GetMapping("/me")
    public ListerRestrictionStatusResponse getMyStatus(
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID userId =
                UUID.fromString(
                        jwt.getSubject()
                );


        return scamModerationService
                .getMyStatus(
                        userId
                );
    }


    /*
     * Appeal CURRENT account restriction.
     */
    @PostMapping("/me/appeals")
    @ResponseStatus(HttpStatus.CREATED)
    public ListerScamAppealResponse submitAppeal(

            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            CreateListerScamAppealRequest request,

            HttpServletRequest httpRequest

    ) {

        UUID listerUserId =
                UUID.fromString(
                        jwt.getSubject()
                );


        return scamModerationService
                .submitAppeal(
                        listerUserId,
                        request,
                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }
}