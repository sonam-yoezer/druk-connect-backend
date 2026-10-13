package com.drukconnect.drukconnect.controller.moderation;

import com.drukconnect.drukconnect.common.RequestMetadata;

import com.drukconnect.drukconnect.dto.moderation.*;

import com.drukconnect.drukconnect.enums.moderation
        .ScamAppealStatus;

import com.drukconnect.drukconnect.enums.moderation
        .ScamReportStatus;

import com.drukconnect.drukconnect.service.moderation
        .ListerScamModerationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/lister-scam")
public class AdminListerScamController {

    private final ListerScamModerationService
            scamModerationService;


    public AdminListerScamController(
            ListerScamModerationService scamModerationService
    ) {

        this.scamModerationService =
                scamModerationService;
    }


    /*
     * =========================================================
     * GET REPORTS
     * =========================================================
     */
    @GetMapping("/reports")
    public PagedListerScamReportResponse getReports(

            @RequestParam(
                    defaultValue = "PENDING"
            )
            ScamReportStatus status,

            @RequestParam(
                    defaultValue = "1"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size

    ) {

        return scamModerationService
                .getReports(
                        status,
                        page,
                        size
                );
    }


    /*
     * =========================================================
     * APPROVE / REJECT REPORT
     * =========================================================
     */
    @PostMapping(
            "/reports/{reportId}/moderate"
    )
    public ListerScamReportResponse moderateReport(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID reportId,

            @Valid
            @RequestBody
            ModerateListerScamReportRequest request,

            HttpServletRequest httpRequest

    ) {

        UUID adminUserId =
                UUID.fromString(
                        jwt.getSubject()
                );


        return scamModerationService
                .moderateReport(
                        adminUserId,
                        reportId,
                        request,
                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }


    /*
     * =========================================================
     * GET APPEALS
     * =========================================================
     */
    @GetMapping("/appeals")
    public PagedListerScamAppealResponse getAppeals(

            @RequestParam(
                    defaultValue = "PENDING"
            )
            ScamAppealStatus status,

            @RequestParam(
                    defaultValue = "1"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size

    ) {

        return scamModerationService
                .getAppeals(
                        status,
                        page,
                        size
                );
    }


    /*
     * =========================================================
     * APPROVE / REJECT APPEAL
     * =========================================================
     */
    @PostMapping(
            "/appeals/{appealId}/moderate"
    )
    public ListerScamAppealResponse moderateAppeal(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID appealId,

            @Valid
            @RequestBody
            ModerateListerScamAppealRequest request,

            HttpServletRequest httpRequest

    ) {

        UUID adminUserId =
                UUID.fromString(
                        jwt.getSubject()
                );


        return scamModerationService
                .moderateAppeal(
                        adminUserId,
                        appealId,
                        request,
                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }
}