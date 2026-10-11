package com.drukconnect.drukconnect.repository.listing;
import com.drukconnect.drukconnect.entity.listing.ListingReviewContact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ListingReviewContactRepository extends JpaRepository<ListingReviewContact, UUID> {
    boolean existsByListingIdAndBuyerId(UUID listingId, UUID buyerId);
}
