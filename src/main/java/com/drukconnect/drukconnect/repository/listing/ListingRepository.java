package com.drukconnect.drukconnect.repository.listing;

import com.drukconnect.drukconnect.entity.listing.Listing;
import com.drukconnect.drukconnect.enums.listing.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ListingRepository
        extends JpaRepository<Listing, UUID> {

    Page<Listing> findByStatus(
            ListingStatus status,
            Pageable pageable
    );

    Optional<Listing> findByIdAndStatus(
            UUID id,
            ListingStatus status
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            UPDATE Listing l
            SET l.views = l.views + 1
            WHERE l.id = :listingId
            AND l.status = :status
            """)
    int incrementViews(
            @Param("listingId")
            UUID listingId,

            @Param("status")
            ListingStatus status
    );
}
