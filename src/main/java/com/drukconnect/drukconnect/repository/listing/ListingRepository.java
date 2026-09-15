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

    Page<Listing> findByListerIdAndStatusNot(
            UUID listerId,
            ListingStatus status,
            Pageable pageable
    );

    Optional<Listing> findByIdAndListerIdAndStatusNot(
            UUID id,
            UUID listerId,
            ListingStatus status
    );

    @Query("""
        SELECT l
        FROM Listing l
        JOIN l.lister u
        WHERE l.status = :status

        AND (
            :category IS NULL
            OR LOWER(l.listingCategory) = LOWER(:category)
        )

        AND (
            :city IS NULL
            OR LOWER(l.city) = LOWER(:city)
        )

        AND (
            :query IS NULL

            OR LOWER(l.listingTitle)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(l.listingCategory)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(l.description)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(l.cuisine)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(l.serviceType)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(u.firstName)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(u.lastName)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(
                CONCAT(
                    u.firstName,
                    ' ',
                    u.lastName
                )
            )
                LIKE LOWER(CONCAT('%', :query, '%'))
        )
        """)
    Page<Listing> searchListings(

            @Param("status")
            ListingStatus status,

            @Param("category")
            String category,

            @Param("city")
            String city,

            @Param("query")
            String query,

            Pageable pageable
    );
}
