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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Listing l where l.id = :id")
    Optional<Listing> findForReviewUpdate(@Param("id") UUID id);

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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
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
            :suburb IS NULL
            OR LOWER(l.suburb) = LOWER(:suburb)
        )

        AND (
            :state IS NULL
            OR LOWER(l.state) = LOWER(:state)
        )

        AND (
            :postcode IS NULL
            OR l.postcode = :postcode
        )

        AND (
            :query IS NULL

            OR LOWER(l.listingTitle)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(l.listingCategory)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(l.description)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(l.serviceType)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(l.suburb)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR LOWER(l.state)
                LIKE LOWER(CONCAT('%', :query, '%'))

            OR l.postcode
                LIKE CONCAT('%', :query, '%')

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

            @Param("suburb")
            String suburb,

            @Param("state")
            String state,

            @Param("postcode")
            String postcode,

            @Param("query")
            String query,

            Pageable pageable
    );
    interface DashboardStats {
        long getTotalListings();
        long getActiveListings();
        long getInactiveListings();
        long getTotalListingViews();
    }

    @Query("""
        select count(l) as totalListings,
               coalesce(sum(case when l.status = :active then 1 else 0 end), 0) as activeListings,
               coalesce(sum(case when l.status = :inactive then 1 else 0 end), 0) as inactiveListings,
               coalesce(sum(l.views), 0) as totalListingViews
        from Listing l
        where l.lister.id = :listerId and l.status <> :deleted
        """)
    DashboardStats getDashboardStats(@Param("listerId") UUID listerId,
            @Param("active") ListingStatus active, @Param("inactive") ListingStatus inactive,
            @Param("deleted") ListingStatus deleted);

}
