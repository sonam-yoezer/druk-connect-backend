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
}
