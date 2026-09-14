package com.drukconnect.drukconnect.repository.authentication;

import com.drukconnect.drukconnect.entity.authentication.CommunityGuidelineAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunityGuidelineAcceptanceRepository extends JpaRepository<CommunityGuidelineAcceptance, UUID> {
}
