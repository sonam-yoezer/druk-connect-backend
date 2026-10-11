package com.drukconnect.drukconnect.service.listing;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.*;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.enums.listing.ListingStatus;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.listing.*;
import com.drukconnect.drukconnect.service.authentication.AuditService;
import com.drukconnect.drukconnect.util.ReviewRequestEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingReviewRequestServiceTest {
    @Mock ListingRepository listings;
    @Mock UserRepository users;
    @Mock ListingReviewRepository reviews;
    @Mock ListingReviewContactRepository contacts;
    @Mock ListingReviewRequestRepository requests;
    @Mock ReviewRequestEmailSender email;
    @Mock AuditService audit;
    @Mock ListingServiceAvailmentRepository availments;
    ListingReviewRequestService service;
    User buyer, lister;
    Listing listing;

    @BeforeEach void setup() {
        service = new ListingReviewRequestService(listings, users, reviews, contacts, requests, email, audit, availments, "http://localhost:3000");
        buyer = user(AccessTypeEnum.BUYER); lister = user(AccessTypeEnum.LISTER);
        listing = new Listing(); listing.setId(UUID.randomUUID()); listing.setLister(lister);
        listing.setStatus(ListingStatus.ACTIVE); listing.setListingTitle("Home Tour");
    }
    User user(AccessTypeEnum type) {
        User user = new User(); user.setId(UUID.randomUUID()); user.setStatus(UserStatus.ACTIVE);
        user.setAccessType(type); user.setFirstName("Sonam"); user.setLastName("Dorji");
        user.setEmail("test@example.com"); user.setPhoneNumber("+61412345678"); return user;
    }
    void readSetup() {
        when(users.findById(buyer.getId())).thenReturn(Optional.of(buyer));
        when(listings.findByIdAndStatus(listing.getId(), ListingStatus.ACTIVE)).thenReturn(Optional.of(listing));
    }
    void requestSetup() {
        when(listings.findForReviewUpdate(listing.getId())).thenReturn(Optional.of(listing));
        when(users.findById(lister.getId())).thenReturn(Optional.of(lister));
        when(users.findById(buyer.getId())).thenReturn(Optional.of(buyer));
    }
    ListingReviewRequest invitation() {
        var request = new ListingReviewRequest(); request.setId(UUID.randomUUID()); request.setListing(listing);
        request.setBuyer(buyer); request.setStatus("PENDING"); request.setRequestedAt(Instant.now());
        request.setEmailSentAt(Instant.now()); request.setExpiresAt(Instant.now().plusSeconds(3600)); return request;
    }
    @Test void noContactOrInvitationCannotReview() {
        readSetup(); assertFalse(service.eligibility(buyer.getId(), listing.getId()).canReview());
    }
    @Test void whatsappContactGrantsEligibility() {
        readSetup(); when(contacts.existsByListingIdAndBuyerId(listing.getId(), buyer.getId())).thenReturn(true);
        assertTrue(service.eligibility(buyer.getId(), listing.getId()).canReview());
    }
    @Test void emailedInvitationGrantsEligibilityWithoutWhatsapp() {
        readSetup(); when(requests.findByListingIdAndBuyerId(listing.getId(), buyer.getId())).thenReturn(Optional.of(invitation()));
        var result = service.eligibility(buyer.getId(), listing.getId()); assertTrue(result.canReview()); assertFalse(result.whatsappContacted());
    }
    @Test void expiredOrUndeliveredInvitationCannotGrantEligibility() {
        readSetup(); var request = invitation(); request.setExpiresAt(Instant.now().minusSeconds(1));
        when(requests.findByListingIdAndBuyerId(listing.getId(), buyer.getId())).thenReturn(Optional.of(request));
        assertFalse(service.eligibility(buyer.getId(), listing.getId()).canReview());
        request.setExpiresAt(Instant.now().plusSeconds(3600)); request.setEmailSentAt(null);
        assertFalse(service.eligibility(buyer.getId(), listing.getId()).canReview());
    }
    @Test void existingReviewBlocksEligibility() {
        readSetup(); when(reviews.existsByListingIdAndReviewerIdAndStatusIn(eq(listing.getId()), eq(buyer.getId()), any())).thenReturn(true);
        assertFalse(service.eligibility(buyer.getId(), listing.getId()).canReview());
    }
    @Test void ownerRequestsReviewAndEmailContainsCorrectListingRoute() {
        requestSetup(); when(requests.saveAndFlush(any())).thenAnswer(call -> { ListingReviewRequest request = call.getArgument(0); request.setId(UUID.randomUUID()); return request; });
        var result = service.requestReview(lister.getId(), listing.getId(), buyer.getId(), null);
        assertEquals("PENDING", result.status()); assertEquals(buyer.getId(), result.buyerUserId());
        assertTrue(result.reviewUrl().contains("/buyer-dashboard/services/" + listing.getId()));
        assertTrue(result.reviewUrl().contains("reviewRequest=" + result.requestId()));
        verify(email).send(eq(buyer), eq(lister), eq(listing), eq(result.reviewUrl()));
    }
    @Test void duplicateRequestDoesNotSendAnotherEmail() {
        requestSetup(); when(requests.findByListingIdAndBuyerId(listing.getId(), buyer.getId())).thenReturn(Optional.of(invitation()));
        var error = assertThrows(ApiException.class, () -> service.requestReview(lister.getId(), listing.getId(), buyer.getId(), null));
        assertEquals("REVIEW_REQUEST_PENDING", error.getCode()); verifyNoInteractions(email);
    }
    @Test void anotherListerCannotRequestReviewsForThisListing() {
        User stranger = user(AccessTypeEnum.LISTER);
        when(listings.findForReviewUpdate(listing.getId())).thenReturn(Optional.of(listing));
        when(users.findById(stranger.getId())).thenReturn(Optional.of(stranger));
        assertThrows(ApiException.class, () -> service.requestReview(stranger.getId(), listing.getId(), buyer.getId(), null));
        verifyNoInteractions(email);
    }
    @Test void mailFailureReturnsRetryableError() {
        requestSetup(); when(requests.saveAndFlush(any())).thenAnswer(call -> { ListingReviewRequest request = call.getArgument(0); request.setId(UUID.randomUUID()); return request; });
        doThrow(new IllegalStateException("SMTP unavailable")).when(email).send(any(), any(), any(), anyString());
        var error = assertThrows(ApiException.class, () -> service.requestReview(lister.getId(), listing.getId(), buyer.getId(), null));
        assertEquals("REVIEW_REQUEST_EMAIL_FAILED", error.getCode());
    }
    @Test void emailRequestCannotBeOpenedByAnotherBuyer() {
        var request = invitation(); User stranger = user(AccessTypeEnum.BUYER);
        when(users.findById(stranger.getId())).thenReturn(Optional.of(stranger));
        when(requests.findById(request.getId())).thenReturn(Optional.of(request));
        assertThrows(ApiException.class, () -> service.getForBuyer(stranger.getId(), request.getId()));
    }
    @Test void submissionCompletesInvitation() {
        var request = invitation(); when(requests.findByListingIdAndBuyerId(listing.getId(), buyer.getId())).thenReturn(Optional.of(request));
        service.markCompleted(buyer.getId(), listing.getId()); assertEquals("COMPLETED", request.getStatus()); assertNotNull(request.getCompletedAt());
    }
    @Test void searchAcceptsPhoneEmailAndNameAndIncludesEligibilityFlags() {
        when(users.findById(lister.getId())).thenReturn(Optional.of(lister));
        when(listings.findByIdAndStatus(listing.getId(), ListingStatus.ACTIVE)).thenReturn(Optional.of(listing));
        when(users.searchActiveBuyers(eq(AccessTypeEnum.BUYER), eq(UserStatus.ACTIVE), anyString(), any())).thenReturn(new PageImpl<>(List.of(buyer)));
        for (String query : List.of("+614123", "test@example.com", "Sonam Dorji")) {
            var result = service.search(lister.getId(), listing.getId(), query, 1, 10);
            assertEquals(buyer.getId(), result.buyers().getFirst().userId());
        }
    }
}
