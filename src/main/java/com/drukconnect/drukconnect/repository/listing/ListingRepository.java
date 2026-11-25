package com.drukconnect.drukconnect.repository.listing;

import com.drukconnect.drukconnect.entity.listing.Listing;
import com.drukconnect.drukconnect.enums.listing.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ListingRepository
        extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {

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

    /** Omit absent filters so Hibernate never binds an untyped null to LOWER(). */
    default Page<Listing> searchListings(ListingStatus status, String category, String suburb,
            String state, String postcode, String query, Pageable pageable) {
        return findAll((root, criteriaQuery, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(builder.equal(root.get("status"), status));
            if (category != null) {
                filters.add(builder.equal(builder.lower(root.get("listingCategory")), category.toLowerCase(Locale.ROOT)));
            }
            if (suburb != null) {
                filters.add(builder.equal(builder.lower(root.get("suburb")), suburb.toLowerCase(Locale.ROOT)));
            }
            if (state != null) {
                filters.add(builder.equal(builder.lower(root.get("state")), state.toLowerCase(Locale.ROOT)));
            }
            if (postcode != null) {
                filters.add(builder.equal(root.get("postcode"), postcode));
            }
            if (query != null) {
                String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
                List<Predicate> matches = new ArrayList<>();
                for (String field : List.of("listingTitle", "listingCategory", "description", "serviceType",
                        "suburb", "state")) {
                    matches.add(builder.like(builder.lower(root.get(field)), pattern));
                }
                matches.add(builder.like(root.get("postcode"), "%" + query + "%"));
                var owner = root.join("lister");
                matches.add(builder.like(builder.lower(owner.get("firstName")), pattern));
                matches.add(builder.like(builder.lower(owner.get("lastName")), pattern));
                var fullName = builder.concat(builder.concat(owner.get("firstName"), " "), owner.get("lastName"));
                matches.add(builder.like(builder.lower(fullName), pattern));
                filters.add(builder.or(matches.toArray(Predicate[]::new)));
            }
            return builder.and(filters.toArray(Predicate[]::new));
        }, pageable);
    }

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
