package com.drukconnect.drukconnect.repository.authentication;

import com.drukconnect.drukconnect.entity.authentication.VouchInvitation;
import com.drukconnect.drukconnect.enums.authentication.VouchInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VouchInvitationRepository
        extends JpaRepository<VouchInvitation, UUID> {

    Optional<VouchInvitation> findByTokenHashAndStatus(
            String tokenHash,
            VouchInvitationStatus status
    );

    Optional<VouchInvitation> findByRegisteredUserIdAndStatus(
            UUID registeredUserId,
            VouchInvitationStatus status
    );

    boolean existsByRequesterUserIdAndInviteeEmailIgnoreCaseAndStatus(
            UUID requesterUserId,
            String inviteeEmail,
            VouchInvitationStatus status
    );
}