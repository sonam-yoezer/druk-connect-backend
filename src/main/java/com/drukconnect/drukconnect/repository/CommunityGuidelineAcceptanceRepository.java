package com.drukconnect.drukconnect.repository;

import com.drukconnect.drukconnect.entity.CommunityGuidelineAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunityGuidelineAcceptanceRepository extends JpaRepository<CommunityGuidelineAcceptance, UUID> {
}
