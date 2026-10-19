package com.drukconnect.drukconnect.service.listing;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.listing.*;
import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.*;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.listing.*;
import com.drukconnect.drukconnect.service.authentication.AuditService;
import com.drukconnect.drukconnect.service.moderation.ListerRestrictionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.HttpMethod;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:listing_edit_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "app.mail.enabled=false", "app.sms.enabled=false"})
class ListingEditingIntegrationTest {
    static Path imageRoot;
    static { try { imageRoot = Files.createTempDirectory("druk-listing-edit-test-"); } catch (Exception e) { throw new RuntimeException(e); } }
    @DynamicPropertySource static void storage(DynamicPropertyRegistry registry) { registry.add("app.storage.listing-images-dir", () -> imageRoot.toString()); }
    @Autowired ListingService service;
    @Autowired ListingRepository listings;
    @Autowired ListingImageRepository images;
    @Autowired UserRepository users;
    @Autowired WebApplicationContext context;
    @MockitoBean AuditService audit;
    @MockitoBean ListerRestrictionService restrictions;
    User lister; MockMvc mvc;

    @BeforeEach void setup() {
        lister = new User(); lister.setFirstName("Listing"); lister.setLastName("Owner");
        lister.setEmail(UUID.randomUUID()+"@example.com"); lister.setPhoneNumber(UUID.randomUUID().toString().substring(0,18));
        lister.setPasswordHash("test-only"); lister.setStatus(UserStatus.ACTIVE); lister.setAccessType(AccessTypeEnum.LISTER); users.saveAndFlush(lister);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    MockMultipartFile file(String name) { return new MockMultipartFile("images", name, "image/png", new byte[]{1,2,3}); }
    ListingDetailResponse create(String days) { return service.create(lister.getId(), new CreateListingRequest("Test listing","Catering","Suburb","State","1000","Description","Cooking",Set.of(),days,ListingPricingType.FREE,null),List.of(file("old.png"),file("keep.png")),null); }
    @Test void createStoresSingleColumnAndMultipleDays() {
        var listing = create("SUNDAY,MONDAY,WEDNESDAY");
        assertEquals("MONDAY,WEDNESDAY,SUNDAY", listing.availability());
        assertEquals(listing.availability(), listings.findById(listing.id()).orElseThrow().getAvailability());
    }
    @Test void pricingCanSwitchPaidToFreeAndKeepImages() {
        var listing = create("MONDAY");
        var paid = service.updateListing(lister.getId(),listing.id(),new UpdateListingRequest(ListingPricingType.PAID,new BigDecimal("25.50"),"WEDNESDAY,SUNDAY",null),null);
        assertEquals(new BigDecimal("25.50"),paid.rateAmount()); assertEquals(2,paid.images().size());
        var free = service.updateListing(lister.getId(),listing.id(),new UpdateListingRequest(ListingPricingType.FREE,null,null,null),null);
        assertNull(free.rateAmount()); assertEquals("WEDNESDAY,SUNDAY",free.availability());
        assertThrows(ApiException.class,()->service.updateListing(lister.getId(),listing.id(),new UpdateListingRequest(ListingPricingType.PAID,null,null,null),null));
    }
    @Test void multipartReplacementKeepsSelectedImageAndDeletesRemovedFile() throws Exception {
        var listing=create("MONDAY"); var old=images.findById(listing.images().getFirst().imageId()).orElseThrow();
        UUID kept=listing.images().getLast().imageId();
        mvc.perform(multipart(HttpMethod.PATCH,"/api/v1/listings/"+listing.id()).file(file("replacement.png"))
            .param("data","{\"pricingType\":\"FREE\",\"availability\":\"MONDAY,SUNDAY\",\"retainedImageIds\":[\""+kept+"\"]}")
            .with(jwt().jwt(jwt->jwt.subject(lister.getId().toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.images.length()").value(2));
        assertFalse(images.existsById(old.getId())); assertTrue(images.existsById(kept));
        assertFalse(Files.exists(imageRoot.resolve(old.getImagePath())));
        assertEquals("MONDAY,SUNDAY",listings.findById(listing.id()).orElseThrow().getAvailability());
    }
    @Test void rollbackPreservesOldImageAndCleansUpNewFiles() throws Exception {
        var listing=create("MONDAY"); var old=images.findById(listing.images().getFirst().imageId()).orElseThrow();
        try(var paths=Files.walk(imageRoot)){ long count=paths.filter(Files::isRegularFile).count();
            assertThrows(ApiException.class,()->service.updateListing(lister.getId(),listing.id(),new UpdateListingRequest(null,null,null,List.of(listing.images().getLast().imageId())),
                List.of(file("new.png"),new MockMultipartFile("images","bad.txt","text/plain",new byte[]{4})),null));
            try(var after=Files.walk(imageRoot)){assertEquals(count,after.filter(Files::isRegularFile).count());}
        }
        assertTrue(images.existsById(old.getId())); assertTrue(Files.exists(imageRoot.resolve(old.getImagePath())));
    }
    @Test void foreignImagesAndRemovingAllImagesAreRejected() {
        var listing=create("MONDAY");
        assertThrows(ApiException.class,()->service.updateListing(lister.getId(),listing.id(),new UpdateListingRequest(null,null,null,List.of(UUID.randomUUID())),null));
        assertThrows(ApiException.class,()->service.updateListing(lister.getId(),listing.id(),new UpdateListingRequest(null,null,null,List.of()),null));
        assertEquals(2,images.findByListingIdOrderBySortOrderAsc(listing.id()).size());
    }
    @Test void jsonPatchRemainsSupportedAndBlankAvailabilityRejected() throws Exception {
        var listing=create("MONDAY");
        mvc.perform(patch("/api/v1/listings/"+listing.id()).contentType("application/json")
            .content("{\"availability\":\"TUESDAY,FRIDAY\"}").with(jwt().jwt(jwt->jwt.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.availability").value("TUESDAY,FRIDAY"));
        mvc.perform(patch("/api/v1/listings/"+listing.id()).contentType("application/json")
            .content("{\"availability\":\"\"}").with(jwt().jwt(jwt->jwt.subject(lister.getId().toString())))).andExpect(status().isBadRequest());
    }

    @Test void createMultipartAcceptsDaysAndRejectsMissingRequiredFields() throws Exception {
        String data = "{\"listingTitle\":\"New listing\",\"listingCategory\":\"Catering\",\"suburb\":\"Suburb\",\"state\":\"State\",\"postcode\":\"1000\",\"description\":\"Description\",\"serviceType\":\"Cooking\",\"availability\":\"MONDAY,WEDNESDAY,SUNDAY\",\"pricingType\":\"FREE\"}";
        mvc.perform(multipart("/api/v1/listings").file(file("new.png")).param("data",data)
            .with(jwt().jwt(jwt->jwt.subject(lister.getId().toString()))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.availability").value("MONDAY,WEDNESDAY,SUNDAY"));
        mvc.perform(multipart("/api/v1/listings").file(file("new.png")).param("data","{}")
            .with(jwt().jwt(jwt->jwt.subject(lister.getId().toString())))).andExpect(status().isBadRequest());
    }
    @Test void moreThanFiveImagesAndOtherOwnersCannotEdit() {
        var listing=create("MONDAY");
        assertThrows(ApiException.class,()->service.updateListing(lister.getId(),listing.id(),new UpdateListingRequest(null,null,null,null),
            List.of(file("1.png"),file("2.png"),file("3.png"),file("4.png")),null));
        User other = new User(); other.setFirstName("Other"); other.setLastName("Owner");
        other.setEmail(UUID.randomUUID()+"@example.com"); other.setPhoneNumber(UUID.randomUUID().toString().substring(0,18));
        other.setPasswordHash("test-only"); other.setStatus(UserStatus.ACTIVE); other.setAccessType(AccessTypeEnum.LISTER); users.saveAndFlush(other);
        assertThrows(ApiException.class,()->service.updateListing(other.getId(),listing.id(),new UpdateListingRequest(null,null,"SUNDAY",null),null));
        assertEquals("MONDAY",listings.findById(listing.id()).orElseThrow().getAvailability());
    }
    @AfterAll static void cleanup() throws Exception {
        try(var paths=Files.walk(imageRoot)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
    }
}
