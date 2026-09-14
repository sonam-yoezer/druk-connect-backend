package com.drukconnect.drukconnect.repository.authentication;

import com.drukconnect.drukconnect.entity.authentication.OtpVerification;
import com.drukconnect.drukconnect.enums.authentication.OtpChannel;
import com.drukconnect.drukconnect.enums.authentication.OtpPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findTopByUserIdAndChannelAndPurposeOrderByCreatedAtDesc(
            UUID userId, OtpChannel channel, OtpPurpose purpose);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OtpVerification> findTopByUserIdAndChannelAndPurposeAndVerifiedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(
            UUID userId, OtpChannel channel, OtpPurpose purpose);
}
