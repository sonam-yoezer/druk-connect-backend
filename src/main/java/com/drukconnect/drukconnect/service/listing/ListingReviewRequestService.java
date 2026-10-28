package com.drukconnect.drukconnect.service.listing;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.listing.*;
import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.*;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.listing.*;
import com.drukconnect.drukconnect.service.authentication.AuditService;
import com.drukconnect.drukconnect.util.ReviewRequestEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ListingReviewRequestService {
    private final ListingRepository listings;
    private final UserRepository users;
    private final ListingReviewRepository reviews;
    private final ListingReviewContactRepository contacts;
    private final ListingReviewRequestRepository invitations;
    private final ReviewRequestEmailSender email;
    private final AuditService audit;
    private final String frontendUrl;
    private final ListingServiceAvailmentRepository availments;

    public ListingReviewRequestService(ListingRepository listings, UserRepository users,
            ListingReviewRepository reviews, ListingReviewContactRepository contacts,
            ListingReviewRequestRepository invitations, ReviewRequestEmailSender email,
            AuditService audit, ListingServiceAvailmentRepository availments, @Value("${app.review.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.listings = listings; this.users = users; this.reviews = reviews;
        this.contacts = contacts; this.invitations = invitations; this.email = email;
        this.audit = audit; this.availments = availments; this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public ReviewEligibilityResponse recordWhatsappContact(UUID buyerId, UUID listingId, RequestMetadata meta) {
        User buyer = requireUser(buyerId, AccessTypeEnum.BUYER);
        Listing listing = lockActiveListing(listingId);
        if (listing.getLister().getId().equals(buyerId)) throw forbidden("You cannot review your own listing");
        if (!contacts.existsByListingIdAndBuyerId(listingId, buyerId)) {
            var contact = new ListingReviewContact();
            contact.setListing(listing); contact.setBuyer(buyer); contact.setContactedAt(Instant.now());
            contacts.saveAndFlush(contact);
            audit.log("LISTING_WHATSAPP_CONTACT_RECORDED", buyerId, null, listing.getLister().getId(),
                null, meta, "{\"listingId\":\"" + listingId + "\"}");
        }
        return eligibility(buyerId, listingId);
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityResponse eligibility(UUID buyerId, UUID listingId) {
        requireUser(buyerId, AccessTypeEnum.BUYER);
        Listing listing = activeListing(listingId);
        boolean contacted = contacts.existsByListingIdAndBuyerId(listingId, buyerId);
        boolean invited = invitations.findByListingIdAndBuyerId(listingId, buyerId)
            .map(this::validInvitation).orElse(false);
        boolean reviewed = alreadyReviewed(listingId, buyerId);
        boolean allowed = !listing.getLister().getId().equals(buyerId) && !reviewed && (contacted || invited || availments.existsByListingIdAndBuyerId(listingId, buyerId));
        return new ReviewEligibilityResponse(listingId, allowed, contacted, invited, reviewed,
            reviewed ? "You already have a pending or approved review for this listing."
            : allowed ? "You can review this service."
            : "Contact this provider on WhatsApp or receive a review invitation before reviewing.");
    }

    // Called inside the existing review transaction; the listing lock also serializes duplicate submissions.
    @Transactional
    public void requireReviewEligibility(UUID buyerId, UUID listingId) {
        lockActiveListing(listingId);
        var eligibility = eligibility(buyerId, listingId);
        if (eligibility.reviewAlreadyExists()) throw new ApiException(HttpStatus.CONFLICT,
            "REVIEW_ALREADY_EXISTS", "You already have a pending or approved review for this listing");
        if (!eligibility.canReview()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REVIEW_NOT_ELIGIBLE",
                "Contact this provider on WhatsApp or receive a valid review invitation before reviewing.");
        }
    }

    @Transactional
    public void markCompleted(UUID buyerId, UUID listingId) {
        invitations.findByListingIdAndBuyerId(listingId, buyerId).ifPresent(request -> {
            request.setStatus("COMPLETED"); request.setCompletedAt(Instant.now());
        });
    }

    @Transactional(readOnly = true)
    public PagedReviewRecipientsResponse search(UUID listerId, UUID listingId, String query, int page, int size) {
        requireOwner(listerId, activeListing(listingId));
        String text = query == null ? "" : query.trim();
        if (text.length() < 2 || text.length() > 100) throw new ApiException(HttpStatus.BAD_REQUEST,
            "INVALID_SEARCH_QUERY", "Enter between 2 and 100 characters to search Buyers.");
        var result = users.searchActiveBuyers(AccessTypeEnum.BUYER, UserStatus.ACTIVE, text, pageable(page, size));
        var buyers = result.getContent().stream().map(buyer -> new ReviewRecipientResponse(
            buyer.getId(), buyer.getFirstName(), buyer.getLastName(), buyer.getEmail(), buyer.getPhoneNumber(),
            contacts.existsByListingIdAndBuyerId(listingId, buyer.getId()),
            invitations.findByListingIdAndBuyerId(listingId, buyer.getId())
                .map(request -> "PENDING".equals(request.getStatus()) && validInvitation(request)).orElse(false),
            availments.existsByListingIdAndBuyerId(listingId, buyer.getId()),
            alreadyReviewed(listingId, buyer.getId()),
                                                                       buyer.getProfileImageUrl()
                                                               )).toList();
        return new PagedReviewRecipientsResponse(buyers, result.getNumber() + 1, result.getSize(),
            result.getTotalElements(), result.getTotalPages(), result.hasNext(), result.hasPrevious());
    }

    @Transactional
    public ReviewInvitationResponse requestReview(UUID listerId, UUID listingId, UUID buyerId, RequestMetadata meta) {
        Listing listing = lockActiveListing(listingId);
        requireOwner(listerId, listing);
        User buyer = requireUser(buyerId, AccessTypeEnum.BUYER);
        if (listerId.equals(buyerId)) throw forbidden("You cannot request a review from yourself");
        if (alreadyReviewed(listingId, buyerId)) throw new ApiException(HttpStatus.CONFLICT,
            "REVIEW_ALREADY_EXISTS", "This Buyer already has a pending or approved review for this listing.");
        var request = invitations.findByListingIdAndBuyerId(listingId, buyerId).orElseGet(ListingReviewRequest::new);
        if ("PENDING".equals(request.getStatus()) && validInvitation(request)) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_REQUEST_PENDING", "A review request is already pending for this Buyer.");
        }
        Instant now = Instant.now();
        request.setListing(listing); request.setBuyer(buyer); request.setStatus("PENDING");
        request.setRequestedAt(now); request.setExpiresAt(now.plus(30, ChronoUnit.DAYS));
        request.setCompletedAt(null); request.setEmailSentAt(null);
        invitations.saveAndFlush(request);
        audit.log("LISTING_REVIEW_REQUESTED", listerId, null, buyerId, null, meta,
            "{\"listingId\":\"" + listingId + "\",\"requestId\":\"" + request.getId() + "\"}");
        try {
            email.send(buyer, listing.getLister(), listing, reviewUrl(request));
        } catch (RuntimeException exception) {
            // A failed email must not create an invitation that falsely claims delivery.
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "REVIEW_REQUEST_EMAIL_FAILED",
                "Unable to send the review request email. Please try again.");
        }
        request.setEmailSentAt(Instant.now());
        return response(request, "Review request email sent successfully.");
    }

    @Transactional(readOnly = true)
    public ReviewInvitationResponse getForBuyer(UUID buyerId, UUID requestId) {
        requireUser(buyerId, AccessTypeEnum.BUYER);
        var request = invitations.findById(requestId).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "REVIEW_REQUEST_NOT_FOUND", "Review request not found"));
        if (!request.getBuyer().getId().equals(buyerId)) throw forbidden("This review request belongs to another Buyer");
        activeListing(request.getListing().getId());
        if (!validInvitation(request)) throw new ApiException(HttpStatus.GONE,
            "REVIEW_REQUEST_EXPIRED", "This review invitation has expired. Ask the provider for a new request.");
        return response(request, "Sign in and share your experience with this service.");
    }

    @Transactional(readOnly = true)
    public PagedReviewInvitationsResponse list(UUID listerId, UUID listingId, int page, int size) {
        requireOwner(listerId, activeListing(listingId));
        var result = invitations.findByListingIdOrderByRequestedAtDesc(listingId, pageable(page, size));
        return new PagedReviewInvitationsResponse(result.getContent().stream()
            .map(request -> response(request, "Review request details.")).toList(), result.getNumber() + 1,
            result.getSize(), result.getTotalElements(), result.getTotalPages(), result.hasNext(), result.hasPrevious());
    }

    private boolean validInvitation(ListingReviewRequest request) {
        return request.getEmailSentAt() != null && request.getExpiresAt().isAfter(Instant.now());
    }
    private boolean alreadyReviewed(UUID listingId, UUID buyerId) {
        return reviews.existsByListingIdAndReviewerIdAndStatusIn(listingId, buyerId,
            List.of(ReviewStatus.PENDING, ReviewStatus.APPROVED));
    }
    private User requireUser(UUID id, AccessTypeEnum accessType) {
        User user = users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE || user.getAccessType() != accessType)
            throw forbidden("An active " + accessType + " account is required");
        return user;
    }
    private void requireOwner(UUID listerId, Listing listing) {
        requireUser(listerId, AccessTypeEnum.LISTER);
        if (!listing.getLister().getId().equals(listerId)) throw forbidden("Only the listing owner can request reviews");
    }
    private Listing activeListing(UUID listingId) {
        return listings.findByIdAndStatus(listingId, ListingStatus.ACTIVE).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND", "Active listing not found"));
    }
    private Listing lockActiveListing(UUID listingId) {
        Listing listing = listings.findForReviewUpdate(listingId).orElseThrow(() -> new ApiException(
            HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND", "Active listing not found"));
        if (listing.getStatus() != ListingStatus.ACTIVE) throw new ApiException(HttpStatus.NOT_FOUND, "LISTING_NOT_FOUND", "Active listing not found");
        return listing;
    }
    private ApiException forbidden(String message) { return new ApiException(HttpStatus.FORBIDDEN, "REVIEW_ACCESS_DENIED", message); }
    private PageRequest pageable(int page, int size) { return PageRequest.of(Math.max(1, page) - 1, Math.min(50, Math.max(1, size))); }
    private String reviewPath(ListingReviewRequest request) {
        return "/buyer-dashboard/services/" + request.getListing().getId() + "?reviewRequest=" + request.getId() + "&review=true";
    }
    private String reviewUrl(ListingReviewRequest request) { return frontendUrl + reviewPath(request); }
    private ReviewInvitationResponse response(ListingReviewRequest request, String message) {
        String status = "PENDING".equals(request.getStatus()) && !request.getExpiresAt().isAfter(Instant.now()) ? "EXPIRED" : request.getStatus();
        String loginUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/login")
            .queryParam("redirect", reviewPath(request)).build().encode().toUriString();
        return new ReviewInvitationResponse(request.getId(), request.getListing().getId(), request.getListing().getListingTitle(),
            request.getBuyer().getId(), request.getBuyer().getFirstName() + " " + request.getBuyer().getLastName(),
            status, request.getRequestedAt(), request.getExpiresAt(), reviewUrl(request), loginUrl, message,
                       request.getBuyer().getProfileImageUrl(), request.getListing().getLister().getProfileImageUrl()
               );
    }
}
