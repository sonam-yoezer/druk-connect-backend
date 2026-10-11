package com.drukconnect.drukconnect.repository.listing;
import com.drukconnect.drukconnect.entity.listing.ListingReviewRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
public interface ListingReviewRequestRepository extends JpaRepository<ListingReviewRequest, UUID> {
    Optional<ListingReviewRequest> findByListingIdAndBuyerId(UUID listingId, UUID buyerId);
    Page<ListingReviewRequest> findByListingIdOrderByRequestedAtDesc(UUID listingId, Pageable pageable);
}
