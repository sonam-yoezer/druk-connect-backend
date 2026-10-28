package com.drukconnect.drukconnect.service.profile;

import com.drukconnect.drukconnect.common.*;
import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.dto.profile.*;
import com.drukconnect.drukconnect.entity.authentication.*;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.repository.authentication.*;
import com.drukconnect.drukconnect.service.authentication.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
public class ProfileContactChangeService {
    private final UserRepository users;
    private final ProfileContactChangeRepository changes;
    private final PasswordEncoder passwords;
    private final OtpDispatchService delivery;
    private final AppProperties properties;
    private final UserProfileService profiles;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public ProfileContactChangeService(UserRepository users, ProfileContactChangeRepository changes,
            PasswordEncoder passwords, OtpDispatchService delivery, AppProperties properties,
            UserProfileService profiles, AuditService audit) {
        this.users = users; this.changes = changes; this.passwords = passwords; this.delivery = delivery;
        this.properties = properties; this.profiles = profiles; this.audit = audit;
    }

    @Transactional
    public ContactChangeResponse start(UUID userId, StartContactChangeRequest request, RequestMetadata meta) {
        User user = activeUser(userId);
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) throw error(HttpStatus.FORBIDDEN, "CURRENT_PASSWORD_INVALID", "Your current password is incorrect.");
        String destination = normalize(request.channel(), request.destination());
        String current = request.channel() == OtpChannel.EMAIL ? user.getEmail() : user.getPhoneNumber();
        if (destination.equalsIgnoreCase(current)) throw error(HttpStatus.BAD_REQUEST, "CONTACT_UNCHANGED", "Enter a different email or phone number.");
        assertAvailable(request.channel(), destination);
        if ((request.channel() == OtpChannel.EMAIL && !properties.mail().enabled())
                || (request.channel() == OtpChannel.PHONE && !properties.sms().enabled())) {
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "CONTACT_DELIVERY_DISABLED", "Verification delivery is not configured for this channel.");
        }
        Instant now = Instant.now();
        ProfileContactChange change = changes.findByUserIdAndChannel(userId, request.channel()).orElseGet(ProfileContactChange::new);
        if (change.getCreatedAt() != null && change.getCreatedAt().plus(properties.otp().resendCooldown()).isAfter(now)) {
            throw error(HttpStatus.TOO_MANY_REQUESTS, "OTP_RESEND_TOO_SOON", "Please wait before requesting another verification code.");
        }
        change.setUser(user); change.setChannel(request.channel()); change.setDestination(destination);
        change.setAttempts(0); change.setCreatedAt(now); change.setExpiresAt(now.plus(properties.otp().ttl())); change.setConsumedAt(null);
        String otp = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        change.setOtpHash(hash(userId, request.channel(), destination, otp));
        changes.saveAndFlush(change);
        try { delivery.send(request.channel(), destination, otp); }
        catch (RuntimeException exception) { throw error(HttpStatus.SERVICE_UNAVAILABLE, "OTP_DELIVERY_FAILED", "Unable to send verification code. Please try again."); }
        audit.log("PROFILE_CONTACT_CODE_SENT", userId, null, null, IdentityNormalizer.maskIdentifier(destination), meta, null);
        return new ContactChangeResponse(change.getId(), IdentityNormalizer.maskIdentifier(destination), change.getExpiresAt(),
                properties.otp().resendCooldown().toSeconds(), "Verification code sent to your new contact.");
    }

    // Persist failed attempt counts; otherwise rolling back an invalid code would allow unlimited guesses.
    @Transactional(noRollbackFor = ApiException.class)
    public ProfileResponse verify(UUID userId, VerifyContactChangeRequest request, RequestMetadata meta) {
        User user = activeUser(userId);
        ProfileContactChange change = changes.findById(request.challengeId()).filter(value -> value.getUser().getId().equals(userId))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "CONTACT_CHANGE_NOT_FOUND", "Verification request not found."));
        if (change.getConsumedAt() != null || !change.getExpiresAt().isAfter(Instant.now())) throw error(HttpStatus.GONE, "OTP_EXPIRED", "This verification code is expired or already used.");
        if (change.getAttempts() >= properties.otp().maxAttempts()) throw error(HttpStatus.TOO_MANY_REQUESTS, "OTP_ATTEMPTS_EXCEEDED", "Too many attempts. Request a new code.");
        String expected = hash(userId, change.getChannel(), change.getDestination(), request.otp());
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), change.getOtpHash().getBytes(StandardCharsets.UTF_8))) {
            change.setAttempts(change.getAttempts() + 1); changes.saveAndFlush(change);
            throw error(HttpStatus.BAD_REQUEST, "OTP_INVALID", "The verification code is incorrect.");
        }
        assertAvailable(change.getChannel(), change.getDestination());
        Instant now = Instant.now();
        if (change.getChannel() == OtpChannel.EMAIL) { user.setEmail(change.getDestination()); user.setEmailVerifiedAt(now); }
        else { user.setPhoneNumber(change.getDestination()); user.setPhoneVerifiedAt(now); }
        change.setConsumedAt(now); users.saveAndFlush(user); changes.saveAndFlush(change);
        audit.log("PROFILE_CONTACT_VERIFIED", userId, null, null, IdentityNormalizer.maskIdentifier(change.getDestination()), meta, null);
        return profiles.get(userId);
    }

    private User activeUser(UUID userId) {
        User user = users.findForProfileUpdate(userId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
        if (user.getStatus() != UserStatus.ACTIVE) throw error(HttpStatus.FORBIDDEN, "ACTIVE_ACCOUNT_REQUIRED", "An active account is required.");
        return user;
    }
    private String normalize(OtpChannel channel, String value) {
        String text = value.trim();
        if (channel == OtpChannel.EMAIL) {
            if (!text.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw error(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "Enter a valid email address.");
            return text.toLowerCase(Locale.ROOT);
        }
        text = text.replaceAll("[\\s()\\-]", "");
        if (!text.matches("\\+[1-9][0-9]{7,14}")) throw error(HttpStatus.BAD_REQUEST, "INVALID_PHONE", "Use an international phone number, for example +61412345678.");
        return text;
    }
    private void assertAvailable(OtpChannel channel, String destination) {
        boolean exists = channel == OtpChannel.EMAIL ? users.existsByEmailIgnoreCase(destination) : users.existsByPhoneNumber(destination);
        if (exists) throw error(HttpStatus.CONFLICT, "CONTACT_ALREADY_USED", "That contact is already associated with an account.");
    }
    private String hash(UUID userId, OtpChannel channel, String destination, String otp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(properties.otp().hmacSecretBase64()), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((userId + ":" + channel + ":" + destination + ":" + otp).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) { throw new IllegalStateException("Unable to verify contact code", exception); }
    }
    private ApiException error(HttpStatus status, String code, String message) { return new ApiException(status, code, message); }
}
