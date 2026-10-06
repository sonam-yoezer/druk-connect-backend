package com.drukconnect.drukconnect.controller.authentication;

import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.authentication.VouchCountResponse;
import com.drukconnect.drukconnect.dto.vouchrecovery.*;
import com.drukconnect.drukconnect.service.authentication.VouchRecoveryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vouch-recovery")
public class VouchRecoveryController {

    private final VouchRecoveryService vouchRecoveryService;

    public VouchRecoveryController(
            VouchRecoveryService vouchRecoveryService
    ) {
        this.vouchRecoveryService = vouchRecoveryService;
    }


    /*
     * Search registered BUYERs by:
     * - first name
     * - last name
     * - full name
     * - email
     */
    @GetMapping("/buyers/search")
    public List<RecoveryBuyerResponse> searchBuyers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String query
    ) {

        UUID listerUserId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return vouchRecoveryService.searchBuyers(
                listerUserId,
                query
        );
    }


    /*
     * Send vouch request to registered BUYER.
     *
     * This ALSO sends the Accept/Decline email.
     */
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RecoveryVouchRequestResponse requestVouch(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRecoveryVouchRequest request,
            HttpServletRequest httpRequest
    ) {

        UUID listerUserId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return vouchRecoveryService.requestVouch(
                listerUserId,
                request,
                RequestMetadata.from(httpRequest)
        );
    }


    /*
     * Invite somebody who DOES NOT yet have
     * a DrukConnect account.
     */
    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public RecoveryVouchInvitationResponse inviteBuyer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRecoveryVouchInvitationRequest request,
            HttpServletRequest httpRequest
    ) {

        UUID listerUserId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return vouchRecoveryService.inviteBuyer(
                listerUserId,
                request,
                RequestMetadata.from(httpRequest)
        );
    }

    @GetMapping("/request-count")
    public RecoveryVouchRequestCountResponse getRequestCount(
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID listerUserId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return vouchRecoveryService.getRequestCount(
                listerUserId
        );
    }

    @GetMapping("/vouch-count")
    public VouchCountResponse getVouchCount(
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID listerUserId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return vouchRecoveryService.getVouchCount(
                listerUserId
        );
    }
}