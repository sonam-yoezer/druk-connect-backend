package com.drukconnect.drukconnect.repository.listing;
import com.drukconnect.drukconnect.entity.listing.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ListingImageRepository
        extends JpaRepository<ListingImage, UUID> {

    List<ListingImage>
    findByListingIdOrderBySortOrderAsc(
            UUID listingId
    );
}