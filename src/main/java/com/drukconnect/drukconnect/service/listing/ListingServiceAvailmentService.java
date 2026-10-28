package com.drukconnect.drukconnect.service.listing;

import com.drukconnect.drukconnect.common.*;
import com.drukconnect.drukconnect.dto.listing.*;
import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.*;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.listing.*;
import com.drukconnect.drukconnect.service.authentication.AuditService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ListingServiceAvailmentService {
    private final ListingServiceAvailmentRepository availments;
    private final ListingRepository listings;
    private final UserRepository users;
    private final ListingReviewRepository reviews;
    private final AuditService audit;
    public ListingServiceAvailmentService(ListingServiceAvailmentRepository availments, ListingRepository listings,
            UserRepository users, ListingReviewRepository reviews, AuditService audit) {
        this.availments=availments; this.listings=listings; this.users=users; this.reviews=reviews; this.audit=audit;
    }
    @Transactional
    public ServiceAvailmentResponse mark(UUID listerId, UUID listingId, UUID buyerId, RequestMetadata meta) {
        User lister = activeUser(listerId);
        Listing listing = listings.findForReviewUpdate(listingId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"LISTING_NOT_FOUND","Listing not found"));
        if (lister.getAccessType()!=AccessTypeEnum.LISTER || !listing.getLister().getId().equals(listerId))
            throw error(HttpStatus.FORBIDDEN,"LISTING_OWNER_REQUIRED","Only the listing owner can mark this service as availed");
        if (listing.getStatus()!=ListingStatus.ACTIVE) throw error(HttpStatus.CONFLICT,"LISTING_NOT_ACTIVE","Only an active listing can be marked as availed");
        User buyer=activeUser(buyerId);
        if (buyer.getAccessType()!=AccessTypeEnum.BUYER || buyerId.equals(listerId))
            throw error(HttpStatus.FORBIDDEN,"BUYER_REQUIRED","Select an active Buyer account");
        var existing=availments.findByListingIdAndBuyerId(listingId,buyerId);
        if (existing.isPresent()) return response(existing.get(),"This service is already marked as availed for this Buyer.");
        var record=new ListingServiceAvailment(); record.setListing(listing); record.setBuyer(buyer); record.setAvailedAt(Instant.now());
        availments.saveAndFlush(record);
        audit.log("LISTING_SERVICE_AVAILED",listerId,null,buyerId,null,meta,"{\"listingId\":\""+listingId+"\"}");
        return response(record,"Service marked as availed successfully. No email was sent.");
    }
    @Transactional(readOnly=true)
    public PagedServiceAvailmentsResponse history(UUID userId,int page,int size) {
        User user=activeUser(userId);
        var pageable=PageRequest.of(Math.max(1,page)-1,Math.min(50,Math.max(1,size)));
        var result=user.getAccessType()==AccessTypeEnum.LISTER
            ? availments.findByListingListerIdOrderByAvailedAtDesc(userId,pageable)
            : availments.findByBuyerIdOrderByAvailedAtDesc(userId,pageable);
        return new PagedServiceAvailmentsResponse(result.getContent().stream().map(record->response(record,"Service availed.")).toList(),
            result.getNumber()+1,result.getSize(),result.getTotalElements(),result.getTotalPages(),result.hasNext(),result.hasPrevious());
    }
    private User activeUser(UUID id) {
        User user=users.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"USER_NOT_FOUND","User not found"));
        if (user.getStatus()!=UserStatus.ACTIVE) throw error(HttpStatus.FORBIDDEN,"ACTIVE_ACCOUNT_REQUIRED","An active account is required");
        return user;
    }
    private ServiceAvailmentResponse response(ListingServiceAvailment record,String message) {
        Listing listing=record.getListing(); User buyer=record.getBuyer(); User lister=listing.getLister();
        boolean reviewed=reviews.existsByListingIdAndReviewerIdAndStatusIn(listing.getId(),buyer.getId(),List.of(ReviewStatus.PENDING,ReviewStatus.APPROVED));
        return new ServiceAvailmentResponse(record.getId(),listing.getId(),listing.getListingTitle(),listing.getStatus().name(),
            buyer.getId(),buyer.getFirstName()+" "+buyer.getLastName(),lister.getId(),lister.getFirstName()+" "+lister.getLastName(),record.getAvailedAt(),reviewed,message,
                       buyer.getProfileImageUrl(), lister.getProfileImageUrl()
               );
    }
    private ApiException error(HttpStatus status,String code,String message) {return new ApiException(status,code,message);}
}
