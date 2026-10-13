package com.drukconnect.drukconnect.service.moderation;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.RequestMetadata;

import com.drukconnect.drukconnect.dto.moderation.*;

import com.drukconnect.drukconnect.entity.authentication.User;

import com.drukconnect.drukconnect.entity.moderation.ListerPostingRestriction;
import com.drukconnect.drukconnect.entity.moderation.ListerScamAppeal;
import com.drukconnect.drukconnect.entity.moderation.ListerScamReport;

import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;

import com.drukconnect.drukconnect.enums.moderation.ModerationAction;
import com.drukconnect.drukconnect.enums.moderation.ScamAppealStatus;
import com.drukconnect.drukconnect.enums.moderation.ScamReportStatus;

import com.drukconnect.drukconnect.repository.authentication.UserRepository;

import com.drukconnect.drukconnect.repository.moderation
        .ListerPostingRestrictionRepository;

import com.drukconnect.drukconnect.repository.moderation
        .ListerScamAppealRepository;

import com.drukconnect.drukconnect.repository.moderation
        .ListerScamReportRepository;

import com.drukconnect.drukconnect.service.authentication.AuditService;

import com.drukconnect.drukconnect.util.ListerScamNotificationSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.Locale;
import java.util.UUID;

@Service
public class ListerScamModerationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ListerScamModerationService.class
            );


    private final UserRepository userRepository;

    private final ListerScamReportRepository reportRepository;

    private final ListerPostingRestrictionRepository
            restrictionRepository;

    private final ListerScamAppealRepository appealRepository;

    private final ListingModerationLookup listingLookup;

    private final AuditService auditService;

    private final ListerScamNotificationSender
            notificationSender;


    public ListerScamModerationService(

            UserRepository userRepository,

            ListerScamReportRepository reportRepository,

            ListerPostingRestrictionRepository restrictionRepository,

            ListerScamAppealRepository appealRepository,

            ListingModerationLookup listingLookup,

            AuditService auditService,

            ListerScamNotificationSender notificationSender

    ) {

        this.userRepository =
                userRepository;

        this.reportRepository =
                reportRepository;

        this.restrictionRepository =
                restrictionRepository;

        this.appealRepository =
                appealRepository;

        this.listingLookup =
                listingLookup;

        this.auditService =
                auditService;

        this.notificationSender =
                notificationSender;
    }


    /*
     * =========================================================
     * REPORT LISTER THROUGH LISTING
     * =========================================================
     *
     * The listing is only evidence/reference.
     *
     * The actual report target is the LISTER who owns
     * the listing.
     * =========================================================
     */
    @Transactional
    public ListerScamReportResponse reportLister(

            UUID reporterUserId,

            UUID listingId,

            CreateListerScamReportRequest request,

            RequestMetadata meta

    ) {

        /*
         * =====================================================
         * REPORTER
         * =====================================================
         */
        User reporter =
                activeUser(
                        reporterUserId
                );


        /*
         * =====================================================
         * FIND LISTING
         * =====================================================
         *
         * Listing identifies the LISTER.
         */
        ListingModerationLookup.ListingTarget listing =
                listingLookup
                        .requireListing(
                                listingId
                        );


        /*
         * =====================================================
         * FIND REPORTED LISTER
         * =====================================================
         */
        User reportedLister =
                userRepository
                        .findById(
                                listing.ownerUserId()
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "LISTING_OWNER_NOT_FOUND",
                                        "Listing owner was not found"
                                )
                        );


        /*
         * =====================================================
         * ONLY LISTER ACCOUNTS CAN BE REPORTED HERE
         * =====================================================
         */
        if (
                reportedLister.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "LISTING_OWNER_NOT_LISTER",
                    "The reported listing does not belong to a Lister account"
            );
        }


        /*
         * =====================================================
         * PREVENT SELF REPORT
         * =====================================================
         */
        if (
                reporter.getId()
                        .equals(
                                reportedLister.getId()
                        )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SELF_REPORT_NOT_ALLOWED",
                    "You cannot report your own account"
            );
        }


        /*
         * =====================================================
         * PREVENT DUPLICATE PENDING REPORT
         * =====================================================
         *
         * Same reporter cannot repeatedly create pending
         * reports from the same listing.
         */
        if (
                reportRepository
                        .existsBySourceListingIdAndReporterIdAndStatus(
                                listingId,
                                reporterUserId,
                                ScamReportStatus.PENDING
                        )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SCAM_REPORT_ALREADY_PENDING",
                    "You already have a pending scam report from this listing"
            );
        }


        /*
         * =====================================================
         * CREATE REPORT
         * =====================================================
         */
        ListerScamReport report =
                new ListerScamReport();


        /*
         * Actual account being reported.
         */
        report.setReportedLister(
                reportedLister
        );


        report.setReporter(
                reporter
        );


        /*
         * Listing is only evidence.
         */
        report.setSourceListingId(
                listing.listingId()
        );


        report.setSourceListingTitle(
                listing.title()
        );


        report.setReason(
                request.reason()
                        .trim()
        );


        report.setDetails(
                normalizeNullable(
                        request.details()
                )
        );


        report.setStatus(
                ScamReportStatus.PENDING
        );


        report =
                reportRepository
                        .saveAndFlush(
                                report
                        );


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         */
        auditService.log(
                "LISTER_SCAM_REPORTED",
                reporter.getId(),
                null,
                reportedLister.getId(),
                null,
                meta,
                "{\"reportId\":\""
                        + report.getId()
                        + "\",\"sourceListingId\":\""
                        + listingId
                        + "\"}"
        );


        /*
         * IMPORTANT:
         *
         * No email to the LISTER here.
         *
         * This is only an accusation at this point.
         * Admin has not approved it yet.
         */


        return toReportResponse(
                report,
                "Scam report submitted successfully."
        );
    }


    /*
     * =========================================================
     * ADMIN GET REPORTS
     * =========================================================
     */
    @Transactional(readOnly = true)
    public PagedListerScamReportResponse getReports(

            ScamReportStatus status,

            int page,

            int size

    ) {

        int safePage =
                Math.max(
                        page,
                        1
                );


        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        50
                );


        Page<ListerScamReport> result =
                reportRepository
                        .findAllByStatus(
                                status,
                                PageRequest.of(
                                        safePage - 1,
                                        safeSize
                                )
                        );


        return new PagedListerScamReportResponse(

                result.getContent()
                        .stream()
                        .map(report ->
                                toReportResponse(
                                        report,
                                        null
                                )
                        )
                        .toList(),

                result.getNumber() + 1,

                result.getSize(),

                result.getTotalElements(),

                result.getTotalPages(),

                result.hasNext(),

                result.hasPrevious()
        );
    }


    /*
     * =========================================================
     * ADMIN APPROVE / REJECT SCAM REPORT
     * =========================================================
     */
    @Transactional
    public ListerScamReportResponse moderateReport(

            UUID adminUserId,

            UUID reportId,

            ModerateListerScamReportRequest request,

            RequestMetadata meta

    ) {

        /*
         * =====================================================
         * ADMIN USER
         * =====================================================
         *
         * SecurityConfig should already require ROLE_ADMIN
         * for the controller endpoint.
         */
        User admin =
                activeUser(
                        adminUserId
                );


        /*
         * =====================================================
         * LOCK REPORT
         * =====================================================
         */
        ListerScamReport report =
                reportRepository
                        .findByIdForUpdate(
                                reportId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "SCAM_REPORT_NOT_FOUND",
                                        "Scam report was not found"
                                )
                        );


        /*
         * =====================================================
         * REPORT MUST STILL BE PENDING
         * =====================================================
         */
        if (
                report.getStatus()
                        != ScamReportStatus.PENDING
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SCAM_REPORT_ALREADY_MODERATED",
                    "This report has already been moderated"
            );
        }


        Instant now =
                Instant.now();


        report.setModeratedBy(
                admin
        );


        report.setModeratedAt(
                now
        );


        report.setModerationReason(
                request.reason()
                        .trim()
        );


        String responseMessage;


        /*
         * =====================================================
         * APPROVE REPORT
         * =====================================================
         */
        if (
                request.action()
                        == ModerationAction.APPROVE
        ) {

            /*
             * Mark report approved.
             */
            report.setStatus(
                    ScamReportStatus.APPROVED
            );


            reportRepository
                    .saveAndFlush(
                            report
                    );


            /*
             * =================================================
             * ACCOUNT-LEVEL RESTRICTION
             * =================================================
             *
             * IMPORTANT:
             *
             * We are NOT disabling the User account.
             *
             * User remains ACTIVE.
             *
             * They can:
             * - login
             * - browse
             *
             * They cannot:
             * - create new listings
             *
             * The listing create service checks this table.
             */
            ListerPostingRestriction restriction =
                    restrictionRepository
                            .findByListerId(
                                    report.getReportedLister()
                                            .getId()
                            )
                            .orElseGet(
                                    ListerPostingRestriction::new
                            );


            restriction.setLister(
                    report.getReportedLister()
            );


            /*
             * Latest approved report becomes the source
             * of the current restriction.
             */
            restriction.setSourceReport(
                    report
            );


            restriction.setActive(
                    true
            );


            restriction.setReason(
                    request.reason()
                            .trim()
            );


            restriction.setRestrictedAt(
                    now
            );


            /*
             * Restriction is active again.
             */
            restriction.setClearedAt(
                    null
            );


            restriction.setClearedBy(
                    null
            );


            restriction.setClearanceReason(
                    null
            );


            restrictionRepository
                    .saveAndFlush(
                            restriction
                    );


            /*
             * =================================================
             * AUDIT FIRST
             * =================================================
             */
            auditService.log(
                    "LISTER_SCAM_REPORT_APPROVED",
                    adminUserId,
                    null,
                    report.getReportedLister()
                            .getId(),
                    null,
                    meta,
                    "{\"reportId\":\""
                            + report.getId()
                            + "\",\"restrictionId\":\""
                            + restriction.getId()
                            + "\"}"
            );


            /*
             * =================================================
             * EMAIL LISTER
             * =================================================
             *
             * Email failure MUST NOT cancel the approved
             * moderation decision.
             */
            safeSendRestrictionApplied(
                    report.getReportedLister(),
                    report
            );


            responseMessage =
                    "Scam report approved. "
                            + "The Lister can still log in, "
                            + "but cannot create new listings.";
        }


        /*
         * =====================================================
         * REJECT REPORT
         * =====================================================
         */
        else {

            report.setStatus(
                    ScamReportStatus.REJECTED
            );


            reportRepository
                    .saveAndFlush(
                            report
                    );


            /*
             * No restriction is created.
             *
             * Your current notification interface has no
             * sendReportRejected() method, so no email is
             * sent to the LISTER here.
             */
            auditService.log(
                    "LISTER_SCAM_REPORT_REJECTED",
                    adminUserId,
                    null,
                    report.getReportedLister()
                            .getId(),
                    null,
                    meta,
                    "{\"reportId\":\""
                            + report.getId()
                            + "\"}"
            );


            responseMessage =
                    "Scam report rejected. "
                            + "No restriction was applied to the Lister.";
        }


        return toReportResponse(
                report,
                responseMessage
        );
    }


    /*
     * =========================================================
     * LISTER SUBMITS APPEAL
     * =========================================================
     *
     * Appeal is against their CURRENT account restriction.
     *
     * No reportId or listingId needs to come from frontend.
     * =========================================================
     */
    @Transactional
    public ListerScamAppealResponse submitAppeal(

            UUID listerUserId,

            CreateListerScamAppealRequest request,

            RequestMetadata meta

    ) {

        /*
         * =====================================================
         * USER
         * =====================================================
         */
        User lister =
                activeUser(
                        listerUserId
                );


        /*
         * =====================================================
         * MUST BE LISTER
         * =====================================================
         */
        if (
                lister.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "LISTER_REQUIRED",
                    "Only Lister accounts can submit this appeal"
            );
        }


        /*
         * =====================================================
         * FIND ACTIVE ACCOUNT RESTRICTION
         * =====================================================
         */
        ListerPostingRestriction restriction =
                restrictionRepository
                        .findByListerIdAndActiveTrue(
                                listerUserId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.CONFLICT,
                                        "NO_ACTIVE_RESTRICTION",
                                        "Your account does not have an active listing restriction"
                                )
                        );


        /*
         * =====================================================
         * ONE PENDING APPEAL AT A TIME
         * =====================================================
         */
        if (
                appealRepository
                        .existsByListerIdAndStatus(
                                listerUserId,
                                ScamAppealStatus.PENDING
                        )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "APPEAL_ALREADY_PENDING",
                    "You already have an appeal waiting for admin review"
            );
        }


        /*
         * =====================================================
         * CREATE APPEAL
         * =====================================================
         */
        ListerScamAppeal appeal =
                new ListerScamAppeal();


        appeal.setLister(
                lister
        );


        appeal.setRestriction(
                restriction
        );


        appeal.setSourceReport(
                restriction.getSourceReport()
        );


        appeal.setAppealReason(
                request.reason()
                        .trim()
        );


        appeal.setStatus(
                ScamAppealStatus.PENDING
        );


        appeal =
                appealRepository
                        .saveAndFlush(
                                appeal
                        );


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         */
        auditService.log(
                "LISTER_SCAM_APPEAL_SUBMITTED",
                listerUserId,
                null,
                null,
                null,
                meta,
                "{\"appealId\":\""
                        + appeal.getId()
                        + "\",\"restrictionId\":\""
                        + restriction.getId()
                        + "\"}"
        );


        /*
         * =====================================================
         * EMAIL CONFIRMATION
         * =====================================================
         *
         * Failure does not rollback the appeal.
         */
        safeSendAppealSubmitted(
                lister,
                appeal
        );


        return toAppealResponse(
                appeal,
                "Appeal submitted successfully and is waiting for admin review."
        );
    }


    /*
     * =========================================================
     * ADMIN GET APPEALS
     * =========================================================
     */
    @Transactional(readOnly = true)
    public PagedListerScamAppealResponse getAppeals(

            ScamAppealStatus status,

            int page,

            int size

    ) {

        int safePage =
                Math.max(
                        page,
                        1
                );


        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        50
                );


        Page<ListerScamAppeal> result =
                appealRepository
                        .findAllByStatus(
                                status,
                                PageRequest.of(
                                        safePage - 1,
                                        safeSize
                                )
                        );


        return new PagedListerScamAppealResponse(

                result.getContent()
                        .stream()
                        .map(appeal ->
                                toAppealResponse(
                                        appeal,
                                        null
                                )
                        )
                        .toList(),

                result.getNumber() + 1,

                result.getSize(),

                result.getTotalElements(),

                result.getTotalPages(),

                result.hasNext(),

                result.hasPrevious()
        );
    }


    /*
     * =========================================================
     * ADMIN APPROVE / REJECT APPEAL
     * =========================================================
     */
    @Transactional
    public ListerScamAppealResponse moderateAppeal(

            UUID adminUserId,

            UUID appealId,

            ModerateListerScamAppealRequest request,

            RequestMetadata meta

    ) {

        /*
         * =====================================================
         * ADMIN
         * =====================================================
         */
        User admin =
                activeUser(
                        adminUserId
                );


        /*
         * =====================================================
         * LOCK APPEAL
         * =====================================================
         */
        ListerScamAppeal appeal =
                appealRepository
                        .findByIdForUpdate(
                                appealId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "SCAM_APPEAL_NOT_FOUND",
                                        "Appeal was not found"
                                )
                        );


        /*
         * =====================================================
         * MUST STILL BE PENDING
         * =====================================================
         */
        if (
                appeal.getStatus()
                        != ScamAppealStatus.PENDING
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "APPEAL_ALREADY_MODERATED",
                    "This appeal has already been moderated"
            );
        }


        Instant now =
                Instant.now();


        appeal.setModeratedBy(
                admin
        );


        appeal.setModeratedAt(
                now
        );


        appeal.setModerationReason(
                request.reason()
                        .trim()
        );


        String responseMessage;


        /*
         * =====================================================
         * APPEAL APPROVED
         * =====================================================
         */
        if (
                request.action()
                        == ModerationAction.APPROVE
        ) {

            appeal.setStatus(
                    ScamAppealStatus.APPROVED
            );


            /*
             * =================================================
             * GET RESTRICTION
             * =================================================
             */
            ListerPostingRestriction restriction =
                    appeal.getRestriction();


            /*
             * Extra protection:
             *
             * Make sure the restriction is still active.
             */
            if (
                    !restriction.isActive()
            ) {

                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "RESTRICTION_ALREADY_CLEARED",
                        "This account restriction has already been cleared"
                );
            }


            /*
             * =================================================
             * REMOVE ACCOUNT RESTRICTION
             * =================================================
             */
            restriction.setActive(
                    false
            );


            restriction.setClearedAt(
                    now
            );


            restriction.setClearedBy(
                    admin
            );


            restriction.setClearanceReason(
                    request.reason()
                            .trim()
            );


            restrictionRepository
                    .saveAndFlush(
                            restriction
                    );


            /*
             * Persist appeal decision.
             */
            appealRepository
                    .saveAndFlush(
                            appeal
                    );


            /*
             * =================================================
             * AUDIT
             * =================================================
             */
            auditService.log(
                    "LISTER_SCAM_APPEAL_APPROVED",
                    adminUserId,
                    null,
                    appeal.getLister()
                            .getId(),
                    null,
                    meta,
                    "{\"appealId\":\""
                            + appeal.getId()
                            + "\",\"restrictionId\":\""
                            + restriction.getId()
                            + "\"}"
            );


            /*
             * =================================================
             * EMAIL LISTER - APPEAL APPROVED
             * =================================================
             *
             * Tell the LISTER:
             *
             * - appeal was approved
             * - restriction removed
             * - can create listings again
             */
            safeSendAppealApproved(
                    appeal.getLister(),
                    appeal
            );


            responseMessage =
                    "Appeal approved. "
                            + "The Lister can create listings again.";
        }


        /*
         * =====================================================
         * APPEAL REJECTED
         * =====================================================
         */
        else {

            appeal.setStatus(
                    ScamAppealStatus.REJECTED
            );


            /*
             * IMPORTANT:
             *
             * Do NOT modify the restriction.
             *
             * restriction.active remains TRUE.
             */
            appealRepository
                    .saveAndFlush(
                            appeal
                    );


            /*
             * =================================================
             * AUDIT
             * =================================================
             */
            auditService.log(
                    "LISTER_SCAM_APPEAL_REJECTED",
                    adminUserId,
                    null,
                    appeal.getLister()
                            .getId(),
                    null,
                    meta,
                    "{\"appealId\":\""
                            + appeal.getId()
                            + "\",\"restrictionId\":\""
                            + appeal.getRestriction()
                            .getId()
                            + "\"}"
            );


            /*
             * =================================================
             * EMAIL LISTER - APPEAL REJECTED
             * =================================================
             *
             * Tell user restriction remains.
             */
            safeSendAppealRejected(
                    appeal.getLister(),
                    appeal
            );


            responseMessage =
                    "Appeal rejected. "
                            + "The Lister remains restricted from creating listings.";
        }


        return toAppealResponse(
                appeal,
                responseMessage
        );
    }


    /*
     * =========================================================
     * GET CURRENT USER RESTRICTION STATUS
     * =========================================================
     *
     * Frontend can call this after login.
     *
     * This is what drives your:
     *
     * "Your account has been suspended/restricted"
     *
     * page.
     * =========================================================
     */
    @Transactional(readOnly = true)
    public ListerRestrictionStatusResponse getMyStatus(
            UUID userId
    ) {

        User user =
                activeUser(
                        userId
                );


        ListerPostingRestriction restriction =
                restrictionRepository
                        .findByListerIdAndActiveTrue(
                                userId
                        )
                        .orElse(null);


        /*
         * =====================================================
         * NO ACTIVE RESTRICTION
         * =====================================================
         */
        if (
                restriction == null
        ) {

            return new ListerRestrictionStatusResponse(

                    userId,

                    false,

                    true,

                    true,

                    true,

                    null,

                    null,

                    null,

                    null,

                    null,

                    null,

                    null,

                    false,

                    null,

                    "Your account has no active restrictions."
            );
        }


        /*
         * =====================================================
         * CURRENT REPORT
         * =====================================================
         */
        ListerScamReport report =
                restriction.getSourceReport();


        /*
         * =====================================================
         * LATEST APPEAL
         * =====================================================
         */
        ListerScamAppeal latestAppeal =
                appealRepository
                        .findTopByListerIdOrderBySubmittedAtDesc(
                                userId
                        )
                        .orElse(null);


        /*
         * =====================================================
         * PENDING APPEAL?
         * =====================================================
         */
        boolean pendingAppeal =
                appealRepository
                        .existsByListerIdAndStatus(
                                userId,
                                ScamAppealStatus.PENDING
                        );


        return new ListerRestrictionStatusResponse(

                user.getId(),

                /*
                 * restricted
                 */
                true,

                /*
                 * canLogin
                 */
                true,

                /*
                 * canBrowseListings
                 */
                true,

                /*
                 * canCreateListings
                 */
                false,

                restriction.getId(),

                report.getId(),

                reportReference(
                        report.getId()
                ),

                report.getSourceListingId(),

                report.getSourceListingTitle(),

                restriction.getReason(),

                restriction.getRestrictedAt(),

                /*
                 * Appeal button enabled only if there is
                 * currently no pending appeal.
                 */
                !pendingAppeal,

                latestAppeal == null
                        ? null
                        : latestAppeal
                        .getStatus()
                        .name(),

                "Your account can still log in and browse, "
                        + "but creating new listings is currently disabled."
        );
    }


    /*
     * =========================================================
     * SAFE EMAIL - RESTRICTION APPLIED
     * =========================================================
     *
     * Email failure must NEVER rollback:
     *
     * report approval
     * restriction creation
     *
     * =========================================================
     */
    private void safeSendRestrictionApplied(

            User lister,

            ListerScamReport report

    ) {

        try {

            notificationSender
                    .sendRestrictionApplied(
                            lister,
                            report
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to send scam restriction email. "
                            + "listerUserId={}, reportId={}",
                    lister.getId(),
                    report.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * SAFE EMAIL - APPEAL SUBMITTED
     * =========================================================
     */
    private void safeSendAppealSubmitted(

            User lister,

            ListerScamAppeal appeal

    ) {

        try {

            notificationSender
                    .sendAppealSubmitted(
                            lister,
                            appeal
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to send appeal submitted email. "
                            + "listerUserId={}, appealId={}",
                    lister.getId(),
                    appeal.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * SAFE EMAIL - APPEAL APPROVED
     * =========================================================
     */
    private void safeSendAppealApproved(

            User lister,

            ListerScamAppeal appeal

    ) {

        try {

            notificationSender
                    .sendAppealApproved(
                            lister,
                            appeal
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to send appeal approved email. "
                            + "listerUserId={}, appealId={}",
                    lister.getId(),
                    appeal.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * SAFE EMAIL - APPEAL REJECTED
     * =========================================================
     */
    private void safeSendAppealRejected(

            User lister,

            ListerScamAppeal appeal

    ) {

        try {

            notificationSender
                    .sendAppealRejected(
                            lister,
                            appeal
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to send appeal rejected email. "
                            + "listerUserId={}, appealId={}",
                    lister.getId(),
                    appeal.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * ACTIVE USER
     * =========================================================
     */
    private User activeUser(
            UUID userId
    ) {

        User user =
                userRepository
                        .findById(
                                userId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User was not found"
                                )
                        );


        if (
                user.getStatus()
                        != UserStatus.ACTIVE
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "User account is not active"
            );
        }


        return user;
    }


    /*
     * =========================================================
     * NORMALIZE OPTIONAL TEXT
     * =========================================================
     */
    private String normalizeNullable(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return null;
        }


        return value.trim();
    }


    /*
     * =========================================================
     * FULL NAME
     * =========================================================
     */
    private String fullName(
            User user
    ) {

        String firstName =
                user.getFirstName() == null
                        ? ""
                        : user.getFirstName();


        String lastName =
                user.getLastName() == null
                        ? ""
                        : user.getLastName();


        return (
                firstName
                        + " "
                        + lastName
        ).trim();
    }


    /*
     * =========================================================
     * HUMAN READABLE REPORT REFERENCE
     * =========================================================
     */
    private String reportReference(
            UUID reportId
    ) {

        return "RPT-"
                + reportId
                .toString()
                .substring(
                        0,
                        8
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }


    /*
     * =========================================================
     * REPORT RESPONSE
     * =========================================================
     */
    private ListerScamReportResponse toReportResponse(

            ListerScamReport report,

            String message

    ) {

        return new ListerScamReportResponse(

                report.getId(),

                report.getReportedLister()
                        .getId(),

                fullName(
                        report.getReportedLister()
                ),

                report.getReporter()
                        .getId(),

                report.getSourceListingId(),

                report.getSourceListingTitle(),

                report.getReason(),

                report.getDetails(),

                report.getStatus()
                        .name(),

                report.getReportedAt(),

                report.getModeratedAt(),

                report.getModerationReason(),

                message
        );
    }


    /*
     * =========================================================
     * APPEAL RESPONSE
     * =========================================================
     */
    private ListerScamAppealResponse toAppealResponse(

            ListerScamAppeal appeal,

            String message

    ) {

        return new ListerScamAppealResponse(

                appeal.getId(),

                appeal.getLister()
                        .getId(),

                appeal.getRestriction()
                        .getId(),

                appeal.getSourceReport()
                        .getId(),

                appeal.getStatus()
                        .name(),

                appeal.getAppealReason(),

                appeal.getSubmittedAt(),

                appeal.getModeratedAt(),

                appeal.getModerationReason(),

                message
        );
    }
}