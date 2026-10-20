package com.drukconnect.drukconnect.repository.listing;
import com.drukconnect.drukconnect.entity.listing.ListingServiceAvailment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
public interface ListingServiceAvailmentRepository extends JpaRepository<ListingServiceAvailment, UUID> {
    boolean existsByListingIdAndBuyerId(UUID listingId, UUID buyerId);
    Optional<ListingServiceAvailment> findByListingIdAndBuyerId(UUID listingId, UUID buyerId);
    Page<ListingServiceAvailment> findByBuyerIdOrderByAvailedAtDesc(UUID buyerId, Pageable pageable);
    Page<ListingServiceAvailment> findByListingListerIdOrderByAvailedAtDesc(UUID listerId, Pageable pageable);
    interface DashboardAvailmentStats {
        long getServicesAvailedCount();
        long getUniqueBuyersServed();
        long getServicesAvailedLast30Days();
    }

    @org.springframework.data.jpa.repository.Query("""
        select count(a) as servicesAvailedCount,
               count(distinct a.buyer.id) as uniqueBuyersServed,
               coalesce(sum(case when a.availedAt >= :since then 1 else 0 end), 0) as servicesAvailedLast30Days
        from ListingServiceAvailment a
        where a.listing.lister.id = :listerId and a.listing.status <> :deleted
          and a.availedAt <= :asOf
        """)
    DashboardAvailmentStats getDashboardStats(
            @org.springframework.data.repository.query.Param("listerId") UUID listerId,
            @org.springframework.data.repository.query.Param("deleted") com.drukconnect.drukconnect.enums.listing.ListingStatus deleted,
            @org.springframework.data.repository.query.Param("since") java.time.Instant since,
            @org.springframework.data.repository.query.Param("asOf") java.time.Instant asOf);

}
