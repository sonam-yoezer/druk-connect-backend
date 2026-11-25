package com.drukconnect.drukconnect.repository.listing;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.Listing;
import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;
import com.drukconnect.drukconnect.enums.listing.ListingPricingType;
import com.drukconnect.drukconnect.enums.listing.ListingStatus;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.service.listing.ListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Set TEST_POSTGRES_URL/USERNAME/PASSWORD to run the same regressions on PostgreSQL.
 * PostgreSQL uses schema validation only; all fixture writes roll back after each test. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:listing_search_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false", "app.mail.enabled=false", "app.sms.enabled=false"
})
@Transactional
class ListingSearchIntegrationTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_POSTGRES_URL");
        if (url != null && !url.isBlank()) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.datasource.username", () -> System.getenv("TEST_POSTGRES_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv("TEST_POSTGRES_PASSWORD"));
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        }
    }

    @Autowired ListingRepository listings;
    @Autowired UserRepository users;
    @Autowired ListingService service;
    @Autowired WebApplicationContext context;
    String marker;
    User owner;
    Listing older, newer, inactive, deleted;

    @BeforeEach
    void setup() {
        marker = "Search" + UUID.randomUUID().toString().replace("-", "");
        owner = new User();
        owner.setFirstName(marker + "First"); owner.setLastName("Last");
        owner.setEmail(marker + "@example.com"); owner.setPhoneNumber(marker.substring(0, 18));
        owner.setPasswordHash("test-only"); owner.setStatus(UserStatus.ACTIVE);
        owner.setAccessType(AccessTypeEnum.LISTER); users.saveAndFlush(owner);
        older = listing(ListingStatus.ACTIVE, Instant.now().minusSeconds(30));
        newer = listing(ListingStatus.ACTIVE, Instant.now().minusSeconds(10));
        inactive = listing(ListingStatus.INACTIVE, Instant.now());
        deleted = listing(ListingStatus.DELETED, Instant.now());
    }

    private Listing listing(ListingStatus status, Instant createdAt) {
        Listing item = new Listing(); item.setLister(owner);
        item.setListingTitle(marker + " Title"); item.setListingCategory(marker + "Category");
        item.setDescription(marker + "Description"); item.setServiceType(marker + "Service");
        item.setSuburb(marker + "Suburb"); item.setState(marker + "State"); item.setPostcode("98765");
        item.setAvailability("MONDAY"); item.setPricingType(ListingPricingType.FREE); item.setStatus(status);
        listings.saveAndFlush(item); item.setCreatedAt(createdAt); return listings.saveAndFlush(item);
    }

    @Test
    void omittedFiltersDoNotBindNullBinaryParameters() {
        var result = listings.searchListings(ListingStatus.ACTIVE, null, null, null, null, null,
                PageRequest.of(0, 1000));
        var ids = result.map(Listing::getId).getContent();
        assertTrue(ids.containsAll(List.of(older.getId(), newer.getId())));
        assertFalse(ids.contains(inactive.getId())); assertFalse(ids.contains(deleted.getId()));
    }

    @Test
    void allOptionalFilterCombinationsRemainValid() {
        for (int mask = 0; mask < 32; mask++) {
            var result = listings.searchListings(ListingStatus.ACTIVE,
                    (mask & 1) == 0 ? null : (marker + "Category").toUpperCase(Locale.ROOT),
                    (mask & 2) == 0 ? null : (marker + "Suburb").toUpperCase(Locale.ROOT),
                    (mask & 4) == 0 ? null : (marker + "State").toUpperCase(Locale.ROOT),
                    (mask & 8) == 0 ? null : "98765",
                    (mask & 16) == 0 ? null : marker.toUpperCase(Locale.ROOT), PageRequest.of(0, 1000));
            var ids = result.map(Listing::getId).getContent();
            assertTrue(ids.containsAll(List.of(older.getId(), newer.getId())), "mask=" + mask);
            assertFalse(ids.contains(inactive.getId())); assertFalse(ids.contains(deleted.getId()));
        }
    }

    @Test
    void textSearchCoversEveryExistingSearchFieldAndFullOwnerName() {
        for (String query : List.of(marker + " Title", marker + "Category", marker + "Description",
                marker + "Service", marker + "Suburb", marker + "State", "98765",
                owner.getFirstName(), owner.getLastName(), owner.getFirstName() + " " + owner.getLastName())) {
            var result = listings.searchListings(ListingStatus.ACTIVE, marker + "Category", null, null, null,
                    query.toUpperCase(Locale.ROOT), PageRequest.of(0, 10));
            assertEquals(2, result.getTotalElements(), query);
        }
    }

    @Test
    void paginationUsesMatchingCountAndNewestFirst() {
        var first = listings.searchListings(ListingStatus.ACTIVE, marker + "Category", null, null, null,
                null, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
        var second = listings.searchListings(ListingStatus.ACTIVE, marker + "Category", null, null, null,
                null, PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertEquals(2, first.getTotalElements()); assertEquals(2, first.getTotalPages());
        assertEquals(newer.getId(), first.getContent().getFirst().getId());
        assertEquals(older.getId(), second.getContent().getFirst().getId());
    }

    @Test
    void nonmatchingFiltersExcludeResults() {
        assertEquals(0, listings.searchListings(ListingStatus.ACTIVE, marker + "Category", "no match",
                null, null, null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, listings.searchListings(ListingStatus.ACTIVE, marker + "Category", null,
                null, null, "no match", PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void serviceTrimsFiltersAndIgnoresBlanks() {
        var result = service.searchListings("  " + marker + "Category  ", "  ", "", null, "  ", 1, 1);
        assertEquals(2, result.totalElements()); assertEquals(newer.getId(), result.listings().getFirst().id());
    }

    @Test
    void publicEndpointAcceptsMissingAndBlankFilters() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/api/v1/listings/search")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/listings/search").param("category", "").param("suburb", " ")
                .param("state", "").param("postcode", "").param("q", " ")).andExpect(status().isOk());
    }

    @Test
    void otherTextSearchQueriesArePostgresqlCompatible() {
        assertTrue(users.searchVouchCandidates(UUID.randomUUID(), marker.toUpperCase(Locale.ROOT),
                UserStatus.ACTIVE, AccessTypeEnum.LISTER, PageRequest.of(0, 10)).stream()
                .anyMatch(user -> user.getId().equals(owner.getId())));
        assertTrue(users.searchActiveBuyers(AccessTypeEnum.LISTER, UserStatus.ACTIVE,
                marker.toUpperCase(Locale.ROOT), PageRequest.of(0, 10)).stream()
                .anyMatch(user -> user.getId().equals(owner.getId())));
        assertDoesNotThrow(() -> users.searchVouchCandidates(UUID.randomUUID(), null,
                UserStatus.ACTIVE, AccessTypeEnum.LISTER, PageRequest.of(0, 10)));
        assertDoesNotThrow(() -> users.searchActiveBuyers(AccessTypeEnum.LISTER, UserStatus.ACTIVE,
                null, PageRequest.of(0, 10)));
    }
}
