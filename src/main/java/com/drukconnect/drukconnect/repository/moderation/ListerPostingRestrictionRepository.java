package com.drukconnect.drukconnect.repository.moderation;

import com.drukconnect.drukconnect.entity.moderation.ListerPostingRestriction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ListerPostingRestrictionRepository
        extends JpaRepository<ListerPostingRestriction, UUID> {


    boolean existsByListerIdAndActiveTrue(
            UUID listerUserId
    );


    Optional<ListerPostingRestriction>
    findByListerId(
            UUID listerUserId
    );


    Optional<ListerPostingRestriction>
    findByListerIdAndActiveTrue(
            UUID listerUserId
    );
}