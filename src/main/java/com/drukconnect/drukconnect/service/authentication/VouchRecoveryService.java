package com.drukconnect.drukconnect.service.authentication;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.IdentityNormalizer;
import com.drukconnect.drukconnect.common.RequestMetadata;

import com.drukconnect.drukconnect.config.AppProperties;

import com.drukconnect.drukconnect.dto.authentication.VouchCountResponse;
import com.drukconnect.drukconnect.dto.vouchrecovery.*;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.authentication.VouchInvitation;
import com.drukconnect.drukconnect.entity.authentication.VouchRequestEntity;

import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;
import com.drukconnect.drukconnect.enums.authentication.VouchInvitationStatus;
import com.drukconnect.drukconnect.enums.authentication.VouchRequestStatus;
import com.drukconnect.drukconnect.enums.authentication.VouchStatus;

import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.authentication.VouchInvitationRepository;
import com.drukconnect.drukconnect.repository.authentication.VouchRepository;
import com.drukconnect.drukconnect.repository.authentication.VouchRequestRepository;

import com.drukconnect.drukconnect.util.VouchInvitationEmailSender;
import com.drukconnect.drukconnect.util.VouchRequestEmailSender;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.time.Instant;

import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class VouchRecoveryService {

    private static final long REQUIRED_VOUCH_COUNT = 2L;

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final UserRepository userRepository;

    private final VouchRepository vouchRepository;

    private final VouchRequestRepository vouchRequestRepository;

    private final VouchInvitationRepository vouchInvitationRepository;

    private final VouchRequestEmailSender vouchRequestEmailSender;

    private final VouchInvitationEmailSender invitationEmailSender;

    private final AuditService auditService;

    private final AppProperties properties;


    public VouchRecoveryService(
            UserRepository userRepository,
            VouchRepository vouchRepository,
            VouchRequestRepository vouchRequestRepository,
            VouchInvitationRepository vouchInvitationRepository,
            VouchRequestEmailSender vouchRequestEmailSender,
            VouchInvitationEmailSender invitationEmailSender,
            AuditService auditService,
            AppProperties properties
    ) {

        this.userRepository =
                userRepository;

        this.vouchRepository =
                vouchRepository;

        this.vouchRequestRepository =
                vouchRequestRepository;

        this.vouchInvitationRepository =
                vouchInvitationRepository;

        this.vouchRequestEmailSender =
                vouchRequestEmailSender;

        this.invitationEmailSender =
                invitationEmailSender;

        this.auditService =
                auditService;

        this.properties =
                properties;
    }


    /*
     * =========================================================
     * SEARCH REGISTERED BUYERS
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<RecoveryBuyerResponse> searchBuyers(
            UUID listerUserId,
            String query
    ) {

        requireRecoveryLister(
                listerUserId
        );


        if (
                query == null
                        ||
                        query.trim().length() < 2
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SEARCH_QUERY_TOO_SHORT",
                    "Enter at least 2 characters"
            );
        }


        return userRepository
                .searchVouchCandidates(
                        listerUserId,
                        query.trim(),
                        UserStatus.ACTIVE,
                        AccessTypeEnum.BUYER,
                        PageRequest.of(
                                0,
                                20
                        )
                )
                .stream()
                .map(
                        user ->
                                new RecoveryBuyerResponse(
                                        user.getId(),
                                        user.getFirstName(),
                                        user.getLastName(),
                                        user.getEmail(),
                                        user.getAccessType()
                                                .name()
                                )
                )
                .toList();
    }


    /*
     * =========================================================
     * SEND REQUEST TO REGISTERED BUYER
     * =========================================================
     *
     * This creates the SAME VouchRequestEntity used by the
     * normal vouch flow.
     *
     * Therefore your existing:
     *
     * /api/v1/vouch-requests/email/{requestId}/respond
     *
     * works without creating another response system.
     * =========================================================
     */
    @Transactional
    public RecoveryVouchRequestResponse requestVouch(
            UUID listerUserId,
            CreateRecoveryVouchRequest request,
            RequestMetadata meta
    ) {

        User requester =
                requireRecoveryLister(
                        listerUserId
                );


        User target =
                activeUser(
                        request.buyerUserId()
                );


        /*
         * =====================================================
         * SELF VOUCH
         * =====================================================
         */
        if (
                requester.getId()
                        .equals(
                                target.getId()
                        )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SELF_VOUCH_NOT_ALLOWED",
                    "You cannot ask yourself to vouch for you"
            );
        }


        /*
         * =====================================================
         * TARGET MUST BE BUYER
         * =====================================================
         */
        if (
                target.getAccessType()
                        != AccessTypeEnum.BUYER
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "BUYER_REQUIRED",
                    "Vouch requests can only be sent to Buyer accounts"
            );
        }


        /*
         * =====================================================
         * ALREADY ACTIVE VOUCH
         * =====================================================
         */
        if (
                vouchRepository
                        .existsByVoucherUserIdAndVouchedUserIdAndStatus(
                                target.getId(),
                                requester.getId(),
                                VouchStatus.ACTIVE
                        )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ALREADY_VOUCHED",
                    "This member has already vouched for you"
            );
        }


        /*
         * =====================================================
         * ALREADY PENDING
         * =====================================================
         */
        if (
                vouchRequestRepository
                        .existsByRequesterIdAndTargetIdAndStatus(
                                requester.getId(),
                                target.getId(),
                                VouchRequestStatus.PENDING
                        )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VOUCH_REQUEST_EXISTS",
                    "A pending vouch request already exists for this member"
            );
        }


        /*
         * =====================================================
         * CREATE REQUEST
         * =====================================================
         */
        VouchRequestEntity entity =
                new VouchRequestEntity();


        entity.setRequester(
                requester
        );


        entity.setTarget(
                target
        );


        String message =
                request.message() == null
                        ||
                        request.message().isBlank()
                        ? fullName(requester)
                          + " is requesting you to provide a vouch."
                        : request.message().trim();


        entity.setMessage(
                message
        );


        entity.setStatus(
                VouchRequestStatus.PENDING
        );


        /*
         * Save first so request ID exists.
         */
        entity =
                vouchRequestRepository
                        .saveAndFlush(
                                entity
                        );


        /*
         * =====================================================
         * GENERATE EMAIL RESPONSE TOKEN
         * =====================================================
         */
        String rawToken =
                generateInvitationToken();


        entity.setResponseTokenHash(
                hashInvitationToken(
                        rawToken
                )
        );


        entity.setResponseTokenExpiresAt(
                Instant.now()
                        .plus(
                                properties
                                        .vouch()
                                        .responseTokenTtl()
                        )
        );


        entity =
                vouchRequestRepository
                        .saveAndFlush(
                                entity
                        );


        /*
         * =====================================================
         * SEND ACCEPT / DECLINE EMAIL
         * =====================================================
         *
         * This sender should produce links such as:
         *
         * /api/v1/vouch-requests/email/{requestId}/respond
         *      ?token=xxx
         *      &action=accept
         *
         * and
         *
         * action=decline
         */
        try {

            vouchRequestEmailSender.send(
                    requester,
                    target,
                    entity,
                    rawToken
            );


            entity.setNotificationSentAt(
                    Instant.now()
            );


            vouchRequestRepository.save(
                    entity
            );


            auditService.log(
                    "VOUCH_REQUEST_EMAIL_SENT",
                    requester.getId(),
                    null,
                    target.getId(),
                    IdentityNormalizer.maskIdentifier(
                            target.getEmail()
                    ),
                    meta,
                    "{\"vouchRequestId\":\""
                            + entity.getId()
                            + "\"}"
            );

        } catch (Exception ex) {

            /*
             * Do not delete/cancel the request just because
             * email delivery failed.
             */
            auditService.log(
                    "VOUCH_REQUEST_EMAIL_FAILED",
                    requester.getId(),
                    null,
                    target.getId(),
                    IdentityNormalizer.maskIdentifier(
                            target.getEmail()
                    ),
                    meta,
                    "{\"vouchRequestId\":\""
                            + entity.getId()
                            + "\"}"
            );
        }


        auditService.log(
                "VOUCH_RECOVERY_REQUESTED",
                requester.getId(),
                null,
                target.getId(),
                null,
                meta,
                "{\"vouchRequestId\":\""
                        + entity.getId()
                        + "\"}"
        );


        return new RecoveryVouchRequestResponse(
                entity.getId(),
                requester.getId(),
                target.getId(),
                fullName(target),
                target.getEmail(),
                entity.getStatus()
                        .name(),
                entity.getRequestedAt(),
                "Vouch request sent successfully."
        );
    }


    /*
     * =========================================================
     * INVITE NON-REGISTERED PERSON BY EMAIL
     * =========================================================
     */
    @Transactional
    public RecoveryVouchInvitationResponse inviteBuyer(
            UUID listerUserId,
            CreateRecoveryVouchInvitationRequest request,
            RequestMetadata meta
    ) {

        User requester =
                requireRecoveryLister(
                        listerUserId
                );


        /*
         * =====================================================
         * NORMALIZE EMAIL
         * =====================================================
         */
        String email =
                IdentityNormalizer.email(
                        request.email()
                );


        /*
         * =====================================================
         * SELF INVITATION
         * =====================================================
         */
        if (
                requester.getEmail() != null
                        &&
                        requester.getEmail()
                                .equalsIgnoreCase(
                                        email
                                )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SELF_VOUCH_NOT_ALLOWED",
                    "You cannot send a vouch invitation to yourself"
            );
        }


        /*
         * =====================================================
         * USER ALREADY REGISTERED
         * =====================================================
         *
         * Registered users use /requests instead.
         */
        User existingUser =
                userRepository
                        .findByEmailIgnoreCase(
                                email
                        )
                        .orElse(null);


        if (
                existingUser != null
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "USER_ALREADY_REGISTERED",
                    "This email already belongs to a DrukConnect user. "
                            + "Search for the user and send a normal vouch request instead."
            );
        }


        /*
         * =====================================================
         * DUPLICATE PENDING INVITATION
         * =====================================================
         */
        if (
                vouchInvitationRepository
                        .existsByRequesterUserIdAndInviteeEmailIgnoreCaseAndStatus(
                                requester.getId(),
                                email,
                                VouchInvitationStatus.PENDING
                        )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INVITATION_ALREADY_PENDING",
                    "A pending invitation has already been sent to this email address"
            );
        }


        /*
         * =====================================================
         * GENERATE INVITATION TOKEN
         * =====================================================
         */
        String rawToken =
                generateInvitationToken();


        /*
         * =====================================================
         * CREATE INVITATION
         * =====================================================
         */
        VouchInvitation invitation =
                new VouchInvitation();


        invitation.setRequesterUser(
                requester
        );


        invitation.setInviteeEmail(
                email
        );


        String message =
                request.message() == null
                        ||
                        request.message().isBlank()
                        ? fullName(requester)
                          + " invited you to join DrukConnect and provide a vouch."
                        : request.message().trim();


        invitation.setMessage(
                message
        );


        /*
         * Never store raw token.
         */
        invitation.setTokenHash(
                hashInvitationToken(
                        rawToken
                )
        );


        invitation.setStatus(
                VouchInvitationStatus.PENDING
        );


        invitation.setExpiresAt(
                Instant.now()
                        .plus(
                                properties
                                        .vouch()
                                        .invitationTtl()
                        )
        );


        invitation =
                vouchInvitationRepository
                        .saveAndFlush(
                                invitation
                        );


        /*
         * =====================================================
         * SEND SIGNUP INVITATION EMAIL
         * =====================================================
         *
         * This email is different from registered-Buyer
         * Accept/Decline email.
         *
         * It should contain signup URL with:
         *
         * ?vouchInvitationToken=<rawToken>
         */
        invitationEmailSender.send(
                requester,
                email,
                rawToken
        );


        invitation.setSentAt(
                Instant.now()
        );


        vouchInvitationRepository.save(
                invitation
        );


        auditService.log(
                "VOUCH_INVITATION_SENT",
                requester.getId(),
                null,
                null,
                IdentityNormalizer.maskIdentifier(
                        email
                ),
                meta,
                null
        );


        return new RecoveryVouchInvitationResponse(
                invitation.getId(),
                invitation.getInviteeEmail(),
                invitation.getStatus()
                        .name(),
                invitation.getExpiresAt(),
                "Vouch invitation sent successfully."
        );
    }


    /*
     * =========================================================
     * REQUIRE RECOVERY LISTER
     * =========================================================
     */
    private User requireRecoveryLister(
            UUID userId
    ) {

        User user =
                activeUser(
                        userId
                );


        if (
                user.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "LISTER_REQUIRED",
                    "Only Lister accounts can use vouch recovery"
            );
        }


        long activeVouchCount =
                vouchRepository
                        .countActiveVouchesByUserId(
                                user.getId()
                        );


        /*
         * Recovery should stop once they have enough vouches.
         */
        if (
                activeVouchCount
                        >= REQUIRED_VOUCH_COUNT
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VOUCH_RECOVERY_NOT_REQUIRED",
                    "You now have enough active vouches. Please log in normally."
            );
        }


        return user;
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
                                        "User not found"
                                )
                        );


        if (
                user.getStatus()
                        != UserStatus.ACTIVE
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "This account is not active"
            );
        }


        return user;
    }


    /*
     * =========================================================
     * SECURE TOKEN
     * =========================================================
     */
    private String generateInvitationToken() {

        byte[] bytes =
                new byte[32];


        SECURE_RANDOM.nextBytes(
                bytes
        );


        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        bytes
                );
    }


    /*
     * =========================================================
     * SHA-256 TOKEN HASH
     * =========================================================
     */
    private String hashInvitationToken(
            String rawToken
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );


            byte[] hash =
                    digest.digest(
                            rawToken.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );


            return HexFormat
                    .of()
                    .formatHex(
                            hash
                    );

        } catch (
                NoSuchAlgorithmException ex
        ) {

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    ex
            );
        }
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


    @Transactional(readOnly = true)
    public RecoveryVouchRequestCountResponse getRequestCount(
            UUID listerUserId
    ) {

        requireRecoveryLister(
                listerUserId
        );

        long pendingRequestCount =
                vouchRequestRepository
                        .countByRequesterIdAndStatus(
                                listerUserId,
                                VouchRequestStatus.PENDING
                        );

        return new RecoveryVouchRequestCountResponse(
                pendingRequestCount
        );
    }

    @Transactional(readOnly = true)
    public VouchCountResponse getVouchCount(
            UUID listerUserId
    ) {

        User user =
                userRepository
                        .findById(listerUserId)
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User account was not found"
                                )
                        );


        if (
                user.getStatus()
                        != UserStatus.ACTIVE
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "This account is not active"
            );
        }


        if (
                user.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "LISTER_REQUIRED",
                    "Vouch recovery is only available to Lister accounts"
            );
        }


        long activeVouches =
                vouchRepository
                        .countActiveVouchesByUserId(
                                listerUserId
                        );


        long remainingVouches =
                Math.max(
                        0,
                        REQUIRED_VOUCH_COUNT - activeVouches
                );


        boolean requirementMet =
                activeVouches >= REQUIRED_VOUCH_COUNT;


        return new VouchCountResponse(
                listerUserId,
                activeVouches,
                REQUIRED_VOUCH_COUNT,
                remainingVouches,
                requirementMet
        );
    }
}