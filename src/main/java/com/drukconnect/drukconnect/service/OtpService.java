package com.drukconnect.drukconnect.service;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.IdentityNormalizer;
import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.entity.OtpVerification;
import com.drukconnect.drukconnect.entity.User;
import com.drukconnect.drukconnect.enums.OtpChannel;
import com.drukconnect.drukconnect.enums.OtpDeliveryStatus;
import com.drukconnect.drukconnect.enums.OtpPurpose;
import com.drukconnect.drukconnect.enums.UserStatus;
import com.drukconnect.drukconnect.repository.OtpVerificationRepository;
import com.drukconnect.drukconnect.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class OtpService {
    private final OtpVerificationRepository otpRepository;
    private final UserRepository userRepository;
    private final OtpDispatchService dispatchService;
    private final AppProperties properties;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final byte[] hmacKey;

    public OtpService(OtpVerificationRepository otpRepository,
                      UserRepository userRepository,
                      OtpDispatchService dispatchService,
                      AppProperties properties,
                      AuditService auditService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.dispatchService = dispatchService;
        this.properties = properties;
        this.auditService = auditService;
        this.hmacKey = Base64.getDecoder().decode(properties.otp().hmacSecretBase64());
        if (this.hmacKey.length < 32) {
            throw new IllegalStateException("OTP_HMAC_SECRET_BASE64 must decode to at least 32 bytes");
        }
    }

    @Transactional
    public void sendSignupOtp(
            User user,
            OtpChannel channel,
            RequestMetadata meta,
            boolean enforceCooldown
    ) {

        String destination =
                channel == OtpChannel.PHONE
                        ? user.getPhoneNumber()
                        : user.getEmail();

        Instant now = Instant.now();


        /*
         * =========================================================
         * CHECK PREVIOUS OTP / RESEND COOLDOWN
         * =========================================================
         */
        otpRepository
                .findTopByUserIdAndChannelAndPurposeOrderByCreatedAtDesc(
                        user.getId(),
                        channel,
                        OtpPurpose.SIGNUP
                )
                .ifPresent(previous -> {

                    if (
                            enforceCooldown
                                    &&
                                    previous.getCreatedAt()
                                            .plus(
                                                    properties.otp()
                                                            .resendCooldown()
                                            )
                                            .isAfter(now)
                    ) {

                        long seconds =
                                previous.getCreatedAt()
                                        .plus(
                                                properties.otp()
                                                        .resendCooldown()
                                        )
                                        .getEpochSecond()
                                        - now.getEpochSecond();

                        throw new ApiException(
                                HttpStatus.TOO_MANY_REQUESTS,
                                "OTP_RESEND_TOO_SOON",
                                "Please wait "
                                        + Math.max(seconds, 1)
                                        + " seconds before requesting another OTP"
                        );
                    }


                    if (
                            previous.getVerifiedAt() == null
                                    &&
                                    previous.getInvalidatedAt() == null
                    ) {

                        previous.setInvalidatedAt(now);

                        otpRepository.save(previous);
                    }
                });


        /*
         * =========================================================
         * GENERATE OTP ONLY ONCE
         *
         * SAME OTP WILL BE USED FOR EMAIL + SMS
         * =========================================================
         */
        String otp =
                generateOtp();


        /*
         * =========================================================
         * CREATE OTP RECORD
         * =========================================================
         */
        OtpVerification record =
                new OtpVerification();


        record.setUser(user);

        record.setChannel(channel);

        record.setPurpose(
                OtpPurpose.SIGNUP
        );

        record.setDestination(
                destination
        );

        record.setOtpHash(
                hash(
                        user.getId(),
                        channel,
                        OtpPurpose.SIGNUP,
                        otp
                )
        );

        record.setExpiresAt(
                now.plus(
                        properties.otp().ttl()
                )
        );

        record.setMaxAttempts(
                properties.otp().maxAttempts()
        );

        record.setRequestedIp(
                meta == null
                        ? null
                        : meta.ipAddress()
        );

        record.setDeliveryStatus(
                OtpDeliveryStatus.PENDING
        );


        otpRepository.save(record);


        /*
         * =========================================================
         * PRIMARY OTP DELIVERY
         * =========================================================
         */
        try {

            /*
             * Existing delivery.
             *
             * EMAIL -> Email sender
             * PHONE -> SMS sender
             */
            dispatchService.send(
                    channel,
                    destination,
                    otp
            );


            /*
             * =====================================================
             * NEW:
             *
             * WHEN OTP IS SENT TO EMAIL,
             * ALSO SEND THE SAME OTP TO MOBILE.
             *
             * No new OTP is generated.
             *
             * Example:
             *
             * otp = 1755
             *
             * Email -> 1755
             * Phone -> 1755
             * =====================================================
             */
            if (
                    channel == OtpChannel.EMAIL
                            &&
                            properties.sms().enabled()
                            &&
                            user.getPhoneNumber() != null
                            &&
                            !user.getPhoneNumber().isBlank()
            ) {

                try {

                    dispatchService.send(
                            OtpChannel.PHONE,
                            user.getPhoneNumber(),
                            otp
                    );


                    auditService.log(
                            "OTP_SMS_SENT",
                            user.getId(),
                            null,
                            null,
                            null,
                            meta,
                            "{\"phone\":\""
                                    + IdentityNormalizer.maskIdentifier(
                                    user.getPhoneNumber()
                            )
                                    + "\"}"
                    );

                } catch (Exception smsException) {

                    /*
                     * SMS failure should NOT mark the EMAIL OTP
                     * delivery as failed.
                     *
                     * Email OTP can still be verified.
                     */
                    String smsError =
                            smsException.getMessage() == null
                                    ? smsException
                                    .getClass()
                                    .getSimpleName()
                                    : smsException.getMessage();


                    auditService.log(
                            "OTP_SMS_DELIVERY_FAILED",
                            user.getId(),
                            null,
                            null,
                            null,
                            meta,
                            "{\"error\":\""
                                    + escapeJson(
                                    smsError
                            )
                                    + "\"}"
                    );
                }
            }


            /*
             * =====================================================
             * PRIMARY DELIVERY SUCCESS
             * =====================================================
             */
            record.setDeliveryStatus(
                    OtpDeliveryStatus.SENT
            );


            record.setSentAt(
                    Instant.now()
            );


            record.setDeliveryError(
                    null
            );


            auditService.log(
                    "OTP_SENT",
                    user.getId(),
                    null,
                    null,
                    null,
                    meta,
                    "{\"channel\":\""
                            + channel
                            + "\"}"
            );

        } catch (Exception ex) {

            /*
             * =====================================================
             * PRIMARY EMAIL/PHONE DELIVERY FAILED
             * =====================================================
             */
            record.setDeliveryStatus(
                    OtpDeliveryStatus.FAILED
            );


            String msg =
                    ex.getMessage() == null
                            ? ex.getClass()
                            .getSimpleName()
                            : ex.getMessage();


            record.setDeliveryError(
                    msg.length() > 500
                            ? msg.substring(
                            0,
                            500
                    )
                            : msg
            );


            auditService.log(
                    "OTP_DELIVERY_FAILED",
                    user.getId(),
                    null,
                    null,
                    null,
                    meta,
                    "{\"channel\":\""
                            + channel
                            + "\"}"
            );
        }


        otpRepository.save(record);
    }

    private String escapeJson(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public VerificationResult verify(UUID userId, OtpChannel channel, String otp, RequestMetadata meta) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        if (user.getStatus() == UserStatus.DELETED || user.getStatus() == UserStatus.SUSPENDED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_VERIFIABLE", "This account cannot be verified");
        }

        OtpVerification record = otpRepository
                .findTopByUserIdAndChannelAndPurposeAndVerifiedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
                        userId, channel, OtpPurpose.SIGNUP)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "OTP_NOT_FOUND", "No active OTP was found"));

        Instant now = Instant.now();
        if (!record.getExpiresAt().isAfter(now)) {
            record.setInvalidatedAt(now);
            otpRepository.save(record);
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "OTP has expired. Request a new OTP");
        }
        if (record.getAttemptCount() >= record.getMaxAttempts()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_ATTEMPTS_EXCEEDED", "Maximum OTP attempts exceeded");
        }

        String suppliedHash = hash(userId, channel, OtpPurpose.SIGNUP, otp);
        boolean matches = MessageDigest.isEqual(
                suppliedHash.getBytes(StandardCharsets.UTF_8),
                record.getOtpHash().getBytes(StandardCharsets.UTF_8));

        if (!matches) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            if (record.getAttemptCount() >= record.getMaxAttempts()) record.setInvalidatedAt(now);
            otpRepository.save(record);
            auditService.log("OTP_VERIFICATION_FAILED", userId, null, null, null, meta,
                    "{\"channel\":\"" + channel + "\"}");
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "OTP is incorrect");
        }

        record.setVerifiedAt(now);
        otpRepository.save(record);

//        if (channel == OtpChannel.PHONE) user.setPhoneVerifiedAt(now);
//        else user.setEmailVerifiedAt(now);

//        boolean emailSatisfied = !properties.auth().requireEmailOtp() || user.getEmailVerifiedAt() != null;
//        boolean fullyVerified = user.getPhoneVerifiedAt() != null && emailSatisfied;
//        if (fullyVerified) user.setStatus(UserStatus.ACTIVE);
//        userRepository.save(user);

//        boolean fullyVerified =
//                channel == OtpChannel.EMAIL
//                        && user.getEmailVerifiedAt() != null;
//
//        if (fullyVerified) {
//            user.setStatus(UserStatus.ACTIVE);
//        }

        if (channel == OtpChannel.PHONE) {
            user.setPhoneVerifiedAt(now);
        } else if (channel == OtpChannel.EMAIL) {
            user.setEmailVerifiedAt(now);
        }

        /*
         * Phone verification logic is still preserved.
         *
         * For now:
         * app.auth.require-phone-otp=false
         *
         * Therefore email verification alone can activate the account.
         *
         * Later:
         * app.auth.require-phone-otp=true
         *
         * Then both phone and email will be required automatically.
         */
        boolean phoneSatisfied =
                !properties.auth().requirePhoneOtp()
                        || user.getPhoneVerifiedAt() != null;

        boolean emailSatisfied =
                !properties.auth().requireEmailOtp()
                        || user.getEmailVerifiedAt() != null;

        boolean fullyVerified =
                phoneSatisfied && emailSatisfied;

        if (fullyVerified) {
            user.setStatus(UserStatus.ACTIVE);
        }

        /*
         * IMPORTANT:
         * Persist email_verified_at / phone_verified_at / status.
         */
        userRepository.saveAndFlush(user);

        auditService.log("OTP_VERIFIED", userId, null, null, null, meta,
                "{\"channel\":\"" + channel + "\",\"accountActive\":" + fullyVerified + "}");

        return new VerificationResult(
                user.getPhoneVerifiedAt() != null,
                user.getEmailVerifiedAt() != null,
                user.getStatus() == UserStatus.ACTIVE
        );
    }

    private String generateOtp() {
        if (properties.otp().digits() != 4) {
            throw new IllegalStateException("This implementation expects a 4-digit OTP to match the current DrukConnect requirement");
        }
        return String.valueOf(1000 + secureRandom.nextInt(9000));
    }

    private String hash(UUID userId, OtpChannel channel, OtpPurpose purpose, String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            String value = userId + "|" + channel + "|" + purpose + "|" + otp;
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return HexFormatHelper.toHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash OTP", ex);
        }
    }

    public record VerificationResult(boolean phoneVerified, boolean emailVerified, boolean accountActive) {}

    private static final class HexFormatHelper {
        private static final char[] HEX = "0123456789abcdef".toCharArray();
        static String toHex(byte[] bytes) {
            char[] out = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xff;
                out[i * 2] = HEX[v >>> 4];
                out[i * 2 + 1] = HEX[v & 0x0f];
            }
            return new String(out);
        }
    }
}
