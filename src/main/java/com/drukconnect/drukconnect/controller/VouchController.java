package com.drukconnect.drukconnect.controller;

import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.*;
import com.drukconnect.drukconnect.service.VouchService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vouch-requests")
public class VouchController {

    private final VouchService vouchService;

    public VouchController(
            VouchService vouchService
    ) {
        this.vouchService =
                vouchService;
    }

    /*
     * =========================================================
     * SEARCH BUYERS
     *
     * LISTER does not need login token.
     * LISTER userId is supplied in path.
     *
     * Search using:
     * - email
     * - first name
     * - last name
     * - full name
     * =========================================================
     */
    @GetMapping("/users/{requesterUserId}/search")
    public List<VouchUserSearchResponse> searchUsers(
            @PathVariable UUID requesterUserId,
            @RequestParam String query
    ) {

        return vouchService.searchUsers(
                requesterUserId,
                query
        );
    }

    /*
     * =========================================================
     * CREATE NORMAL VOUCH REQUEST
     *
     * LISTER -> Existing BUYER
     * =========================================================
     */
    @PostMapping("/users/{requesterUserId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public VouchRequestResponse create(
            @PathVariable UUID requesterUserId,
            @Valid
            @RequestBody
            CreateVouchRequest request,
            HttpServletRequest httpRequest
    ) {

        return vouchService.create(
                requesterUserId,
                request,
                RequestMetadata.from(
                        httpRequest
                )
        );
    }

    /*
     * =========================================================
     * BUYER RESPONDS TO VOUCH REQUEST
     *
     * currentUserId = BUYER user id
     * =========================================================
     */
    @PostMapping(
            "/users/{currentUserId}/requests/{requestId}/respond"
    )
    public VouchRequestResponse respond(
            @PathVariable UUID currentUserId,
            @PathVariable UUID requestId,
            @Valid
            @RequestBody
            RespondVouchRequest request,
            HttpServletRequest httpRequest
    ) {

        return vouchService.respond(
                currentUserId,
                requestId,
                request,
                RequestMetadata.from(
                        httpRequest
                )
        );
    }

    /*
     * =========================================================
     * INVITE NON-REGISTERED USER THROUGH EMAIL
     *
     * LISTER user id supplied in path.
     * =========================================================
     */
    @PostMapping(
            "/users/{requesterUserId}/invitations"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public VouchInvitationResponse inviteByEmail(
            @PathVariable UUID requesterUserId,
            @Valid
            @RequestBody
            CreateVouchInvitationRequest request,
            HttpServletRequest httpRequest
    ) {

        return vouchService.inviteByEmail(
                requesterUserId,
                request,
                RequestMetadata.from(
                        httpRequest
                )
        );
    }

    @GetMapping("/me/incoming")
    public List<IncomingVouchRequestResponse> getReceivedVouchRequests(
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID userId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return vouchService
                .getReceivedVouchRequests(
                        userId
                );
    }

    @RequestMapping(
            value = "/email/{requestId}/respond",
            method = {
                    RequestMethod.GET,
                    RequestMethod.POST
            }
    )
    public ResponseEntity<?> respondFromEmail(
            @PathVariable UUID requestId,
            @RequestParam String token,
            @RequestParam(required = false) String action,
            @RequestBody(required = false) RespondVouchRequest request,
            HttpServletRequest httpRequest
    ) {

        boolean accept;

        /*
         * =========================================================
         * EMAIL BUTTON
         * GET /respond?action=accept
         * GET /respond?action=decline
         * =========================================================
         */
        if (
                "GET".equalsIgnoreCase(
                        httpRequest.getMethod()
                )
        ) {

            if (
                    "accept".equalsIgnoreCase(
                            action
                    )
            ) {

                accept = true;

            } else if (
                    "decline".equalsIgnoreCase(
                            action
                    )
            ) {

                accept = false;

            } else {

                return ResponseEntity
                        .badRequest()
                        .contentType(
                                MediaType.TEXT_HTML
                        )
                        .body(
                                """
                                <html>
                                <body>
                                    <script>
                                        alert("Invalid vouch action.");
                                    </script>
                                </body>
                                </html>
                                """
                        );
            }

            RespondVouchRequest emailRequest =
                    new RespondVouchRequest(
                            accept
                    );

            vouchService.respondFromEmail(
                    requestId,
                    token,
                    emailRequest,
                    RequestMetadata.from(
                            httpRequest
                    )
            );

            String message =
                    accept
                            ? "Vouch accepted successfully."
                            : "Vouch request declined successfully.";

            /*
             * Return a tiny HTML response showing only alert.
             */
            String html =
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>DrukConnect</title>
                    </head>
    
                    <body>
    
                        <script>
    
                            alert("%s");
    
                            /*
                             * Try to close the page if browser allows it.
                             */
                            window.close();
    
                        </script>
    
                    </body>
                    </html>
                    """
                            .formatted(
                                    message
                            );

            return ResponseEntity
                    .ok()
                    .contentType(
                            MediaType.TEXT_HTML
                    )
                    .body(
                            html
                    );
        }

        /*
         * =========================================================
         * NORMAL API POST
         * =========================================================
         */
        if (
                request == null
        ) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Request body is required"
                    );
        }

        VouchRequestResponse response =
                vouchService.respondFromEmail(
                        requestId,
                        token,
                        request,
                        RequestMetadata.from(
                                httpRequest
                        )
                );

        return ResponseEntity.ok(
                response
        );
    }

    @GetMapping(
            "/users/{userId}/vouch-count"
    )
    public VouchCountResponse getVouchCount(
            @PathVariable UUID userId
    ) {

        return vouchService
                .getVouchCount(
                        userId
                );
    }
}