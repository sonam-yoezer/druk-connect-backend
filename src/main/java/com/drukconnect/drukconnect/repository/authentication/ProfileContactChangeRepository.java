package com.drukconnect.drukconnect.repository.authentication;
import com.drukconnect.drukconnect.entity.authentication.ProfileContactChange;
import com.drukconnect.drukconnect.enums.authentication.OtpChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface ProfileContactChangeRepository extends JpaRepository<ProfileContactChange, UUID> {
    Optional<ProfileContactChange> findByUserIdAndChannel(UUID userId, OtpChannel channel);
}
