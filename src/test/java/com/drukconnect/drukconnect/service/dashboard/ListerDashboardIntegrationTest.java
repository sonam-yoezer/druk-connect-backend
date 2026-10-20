package com.drukconnect.drukconnect.service.dashboard;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.Listing;
import com.drukconnect.drukconnect.entity.listing.ListingReview;
import com.drukconnect.drukconnect.entity.listing.ListingServiceAvailment;
import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.listing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:lister_dashboard_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.mail.enabled=false", "app.sms.enabled=false"
})
@Transactional
class ListerDashboardIntegrationTest {
    @Autowired ListerDashboardService service;
    @Autowired UserRepository users;
    @Autowired ListingRepository listings;
    @Autowired ListingReviewRepository reviews;
    @Autowired ListingServiceAvailmentRepository availments;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    User lister;

    @BeforeEach void setup() {
        lister = user(AccessTypeEnum.LISTER);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void emptyDashboardReturnsZeroCountsAndUnrated() throws Exception {
        mvc.perform(get("/api/v1/lister/dashboard").with(jwt().jwt(jwt -> jwt.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.totalListings").value(0)).andExpect(jsonPath("$.activeListings").value(0))
            .andExpect(jsonPath("$.inactiveListings").value(0)).andExpect(jsonPath("$.totalListingViews").value(0))
            .andExpect(jsonPath("$.averageRating").isEmpty()).andExpect(jsonPath("$.approvedReviewCount").value(0))
            .andExpect(jsonPath("$.servicesAvailedCount").value(0)).andExpect(jsonPath("$.uniqueBuyersServed").value(0))
            .andExpect(jsonPath("$.servicesAvailedLast30Days").value(0)).andExpect(jsonPath("$.activeVouchCount").value(0))
            .andExpect(jsonPath("$.generatedAt").isNotEmpty());
    }

    @Test void aggregatesOnlyOwnNonDeletedListingsAndApprovedReviews() throws Exception {
        Listing active = listing(lister, ListingStatus.ACTIVE, 10);
        Listing inactive = listing(lister, ListingStatus.INACTIVE, 5);
        Listing deleted = listing(lister, ListingStatus.DELETED, 99);
        User otherLister = user(AccessTypeEnum.LISTER);
        Listing other = listing(otherLister, ListingStatus.ACTIVE, 100);
        User firstBuyer = user(AccessTypeEnum.BUYER);
        User secondBuyer = user(AccessTypeEnum.BUYER);
        review(active, firstBuyer, 5, ReviewStatus.APPROVED);
        review(active, secondBuyer, 5, ReviewStatus.APPROVED);
        review(active, firstBuyer, 2, ReviewStatus.APPROVED);
        review(inactive, firstBuyer, 2, ReviewStatus.APPROVED);
        review(active, firstBuyer, 1, ReviewStatus.PENDING);
        review(active, firstBuyer, 1, ReviewStatus.REJECTED);
        review(deleted, firstBuyer, 1, ReviewStatus.APPROVED);
        review(other, firstBuyer, 1, ReviewStatus.APPROVED);
        availed(active, firstBuyer, -31);
        availed(inactive, firstBuyer, -1);
        availed(active, secondBuyer, -2);
        availed(deleted, firstBuyer, -1);
        availed(other, firstBuyer, -1);
        availed(inactive, secondBuyer, 1); // A future timestamp must not count as a completed service.

        // No caller-supplied userId can change the authenticated scope.
        mvc.perform(get("/api/v1/lister/dashboard").param("userId", otherLister.getId().toString())
                .with(jwt().jwt(jwt -> jwt.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalListings").value(2))
            .andExpect(jsonPath("$.activeListings").value(1)).andExpect(jsonPath("$.inactiveListings").value(1))
            .andExpect(jsonPath("$.totalListingViews").value(15)).andExpect(jsonPath("$.averageRating").value(3.5))
            .andExpect(jsonPath("$.approvedReviewCount").value(4)).andExpect(jsonPath("$.servicesAvailedCount").value(3))
            .andExpect(jsonPath("$.uniqueBuyersServed").value(2)).andExpect(jsonPath("$.servicesAvailedLast30Days").value(2));
    }

    @Test void authenticationAndActiveListerAccountAreRequired() throws Exception {
        mvc.perform(get("/api/v1/lister/dashboard")).andExpect(status().isUnauthorized());
        User buyer = user(AccessTypeEnum.BUYER);
        mvc.perform(get("/api/v1/lister/dashboard").with(jwt().jwt(jwt -> jwt.subject(buyer.getId().toString()))))
            .andExpect(status().isForbidden());
        lister.setStatus(UserStatus.SUSPENDED);
        users.saveAndFlush(lister);
        mvc.perform(get("/api/v1/lister/dashboard").with(jwt().jwt(jwt -> jwt.subject(lister.getId().toString()))))
            .andExpect(status().isForbidden());
    }

    @Test void ratingRoundsToOneDecimalAcrossIndividualReviews() {
        Listing listing = listing(lister, ListingStatus.ACTIVE, 0);
        User buyer = user(AccessTypeEnum.BUYER);
        review(listing, buyer, 5, ReviewStatus.APPROVED);
        review(listing, buyer, 4, ReviewStatus.APPROVED);
        review(listing, buyer, 4, ReviewStatus.APPROVED);
        assertEquals("4.3", service.getDashboard(lister.getId()).averageRating().toPlainString());
    }

    private User user(AccessTypeEnum type) {
        User user = new User();
        user.setFirstName("Dashboard"); user.setLastName("Test");
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPhoneNumber(UUID.randomUUID().toString().substring(0, 18));
        user.setPasswordHash("test-only"); user.setStatus(UserStatus.ACTIVE); user.setAccessType(type);
        return users.saveAndFlush(user);
    }
    private Listing listing(User owner, ListingStatus status, long views) {
        Listing listing = new Listing(); listing.setLister(owner); listing.setListingTitle("Test service");
        listing.setListingCategory("Catering"); listing.setDescription("Test"); listing.setServiceType("Cooking");
        listing.setSuburb("Suburb"); listing.setState("State"); listing.setPostcode("1000");
        listing.setAvailability("MONDAY"); listing.setPricingType(ListingPricingType.FREE);
        listing.setStatus(status); listing.setViews(views);
        return listings.saveAndFlush(listing);
    }
    private void review(Listing listing, User buyer, int rating, ReviewStatus status) {
        ListingReview review = new ListingReview(); review.setListing(listing); review.setReviewer(buyer);
        review.setRating(rating); review.setComment("Test"); review.setStatus(status); reviews.saveAndFlush(review);
    }
    private void availed(Listing listing, User buyer, int dayOffset) {
        ListingServiceAvailment record = new ListingServiceAvailment(); record.setListing(listing); record.setBuyer(buyer);
        record.setAvailedAt(Instant.now().plus(dayOffset, ChronoUnit.DAYS)); availments.saveAndFlush(record);
    }
}
