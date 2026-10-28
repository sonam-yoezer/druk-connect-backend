package com.drukconnect.drukconnect.service.profile;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.profile.*;
import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.entity.listing.*;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.repository.listing.*;
import com.drukconnect.drukconnect.repository.authentication.*;
import com.drukconnect.drukconnect.service.authentication.AuditService;
import com.drukconnect.drukconnect.service.authentication.OtpDispatchService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:profile_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.mail.enabled=true", "app.sms.enabled=true"
})
class UserProfileIntegrationTest {
    static Path root;
    static { try { root = Files.createTempDirectory("druk-profile-test-"); } catch (Exception e) { throw new RuntimeException(e); } }
    @DynamicPropertySource static void storage(DynamicPropertyRegistry registry) { registry.add("app.storage.profile-images-dir", () -> root.toString()); }
    @Autowired UserProfileService service;
    @Autowired ProfileContactChangeService contacts;
    @Autowired UserRepository users;
    @Autowired ProfileContactChangeRepository changes;
    @Autowired PasswordEncoder passwords;
    @Autowired ListingRepository listings;
    @Autowired ListingReviewRepository reviews;
    @Autowired VouchRepository vouches;
    @Autowired VouchRequestRepository vouchRequests;
    @Autowired WebApplicationContext context;
    @MockitoBean OtpDispatchService delivery;
    @MockitoBean AuditService audit;
    MockMvc mvc;
    User user;
    static final String PASSWORD = "ProfileTestPassword123!";

    @BeforeEach void setup() {
        user = account();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    User account() {
        User value = new User(); value.setFirstName("Profile"); value.setLastName("User");
        value.setEmail(UUID.randomUUID()+"@example.com"); value.setPhoneNumber("+61"+Math.abs(UUID.randomUUID().getLeastSignificantBits()%1_000_000_000L));
        value.setEmailVerifiedAt(Instant.now()); value.setPhoneVerifiedAt(Instant.now());
        value.setPasswordHash(passwords.encode(PASSWORD)); value.setAccessType(AccessTypeEnum.BUYER); value.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(value);
    }
    MockMultipartFile image() throws Exception {
        var bytes = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(32,32,BufferedImage.TYPE_INT_RGB),"png",bytes);
        return new MockMultipartFile("image","avatar.png","image/png",bytes.toByteArray());
    }
    @Test void profileIsPrivateAndDetailsAreEditableWithoutExposingSecrets() throws Exception {
        mvc.perform(get("/api/v1/users/me/profile")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/users/me/profile").contentType("application/json")
            .content("{\"firstName\":\"Sonam\",\"lastName\":\"Dorji\",\"bio\":\"Community member\",\"location\":\"Melbourne\"}")
            .with(jwt().jwt(jwt->jwt.subject(user.getId().toString())))).andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Sonam")).andExpect(jsonPath("$.bio").value("Community member"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.profileImagePath").doesNotExist());
        assertEquals(user.getEmail(),users.findById(user.getId()).orElseThrow().getEmail());
    }
    @Test void validPhotoIsPublicReplacementDeletesOldAndRemovalReturnsFallback() throws Exception {
        var first = service.upload(user.getId(),image(),null);
        String oldPath = users.findById(user.getId()).orElseThrow().getProfileImagePath();
        mvc.perform(get(first.profileImageUrl())).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        var second = service.upload(user.getId(),image(),null);
        assertNotEquals(first.profileImageUrl(),second.profileImageUrl()); assertFalse(Files.exists(root.resolve(oldPath)));
        assertNull(service.removeImage(user.getId(),null).profileImageUrl());
        mvc.perform(get(second.profileImageUrl())).andExpect(status().isNotFound());
    }
    @Test void fakePhotoIsRejectedAndExistingPhotoPreserved() throws Exception {
        var existing = service.upload(user.getId(),image(),null);
        assertThrows(ApiException.class,()->service.upload(user.getId(),new MockMultipartFile("image","fake.png","image/png",new byte[]{1,2,3}),null));
        assertEquals(existing.profileImageUrl(),service.get(user.getId()).profileImageUrl());
        mvc.perform(put("/api/v1/users/me/profile-image")).andExpect(status().isUnauthorized());
    }
    @Test void emailChangesOnlyAfterValidCodeAndCodeCannotBeReplayed() {
        String destination=UUID.randomUUID()+"@example.com";
        var challenge=contacts.start(user.getId(),new StartContactChangeRequest(OtpChannel.EMAIL,destination,PASSWORD),null);
        ArgumentCaptor<String> otp=ArgumentCaptor.forClass(String.class);
        verify(delivery).send(eq(OtpChannel.EMAIL),eq(destination),otp.capture());
        assertEquals(user.getEmail(),service.get(user.getId()).email());
        var result=contacts.verify(user.getId(),new VerifyContactChangeRequest(challenge.challengeId(),otp.getValue()),null);
        assertEquals(destination,result.email()); assertNotNull(result.emailVerifiedAt());
        assertThrows(ApiException.class,()->contacts.verify(user.getId(),new VerifyContactChangeRequest(challenge.challengeId(),otp.getValue()),null));
    }
    @Test void phoneCodeIsBoundToAuthenticatedAccountAndResendHasCooldown() {
        String destination="+614"+String.format("%08d",Math.abs(UUID.randomUUID().getLeastSignificantBits()%100_000_000L));
        var challenge=contacts.start(user.getId(),new StartContactChangeRequest(OtpChannel.PHONE,destination,PASSWORD),null);
        ArgumentCaptor<String> otp=ArgumentCaptor.forClass(String.class); verify(delivery).send(eq(OtpChannel.PHONE),eq(destination),otp.capture());
        User other=account();
        assertThrows(ApiException.class,()->contacts.verify(other.getId(),new VerifyContactChangeRequest(challenge.challengeId(),otp.getValue()),null));
        assertThrows(ApiException.class,()->contacts.start(user.getId(),new StartContactChangeRequest(OtpChannel.PHONE,destination,PASSWORD),null));
        assertEquals(destination,contacts.verify(user.getId(),new VerifyContactChangeRequest(challenge.challengeId(),otp.getValue()),null).phoneNumber());
    }
    @Test void invalidAttemptsPersistAndEventuallyBlockTheCode() {
        String destination=UUID.randomUUID()+"@example.com";
        var challenge=contacts.start(user.getId(),new StartContactChangeRequest(OtpChannel.EMAIL,destination,PASSWORD),null);
        ArgumentCaptor<String> otp=ArgumentCaptor.forClass(String.class); verify(delivery).send(any(),anyString(),otp.capture());
        String wrong=otp.getValue().equals("000000")?"111111":"000000";
        for(int attempt=0;attempt<5;attempt++) assertThrows(ApiException.class,()->contacts.verify(user.getId(),new VerifyContactChangeRequest(challenge.challengeId(),wrong),null));
        assertEquals(5,changes.findById(challenge.challengeId()).orElseThrow().getAttempts());
        var failure=assertThrows(ApiException.class,()->contacts.verify(user.getId(),new VerifyContactChangeRequest(challenge.challengeId(),otp.getValue()),null));
        assertEquals("OTP_ATTEMPTS_EXCEEDED",failure.getCode()); assertEquals(user.getEmail(),service.get(user.getId()).email());
    }
    @Test void wrongPasswordDuplicateContactAndDeliveryFailureDoNotChangeIdentity() {
        User other=account();
        assertThrows(ApiException.class,()->contacts.start(user.getId(),new StartContactChangeRequest(OtpChannel.EMAIL,other.getEmail(),"wrong"),null));
        assertThrows(ApiException.class,()->contacts.start(user.getId(),new StartContactChangeRequest(OtpChannel.EMAIL,other.getEmail(),PASSWORD),null));
        doThrow(new IllegalStateException("Mail unavailable")).when(delivery).send(any(),anyString(),anyString());
        assertThrows(ApiException.class,()->contacts.start(user.getId(),new StartContactChangeRequest(OtpChannel.EMAIL,UUID.randomUUID()+"@example.com",PASSWORD),null));
        assertTrue(changes.findByUserIdAndChannel(user.getId(),OtpChannel.EMAIL).isEmpty());
        assertEquals(user.getEmail(),service.get(user.getId()).email());
    }
    @Test void expiredChallengeDoesNotChangeContact() {
        var challenge=contacts.start(user.getId(),new StartContactChangeRequest(OtpChannel.EMAIL,UUID.randomUUID()+"@example.com",PASSWORD),null);
        var stored=changes.findById(challenge.challengeId()).orElseThrow(); stored.setExpiresAt(Instant.now().minusSeconds(1)); changes.saveAndFlush(stored);
        assertThrows(ApiException.class,()->contacts.verify(user.getId(),new VerifyContactChangeRequest(challenge.challengeId(),"123456"),null));
        assertEquals(user.getEmail(),service.get(user.getId()).email());
    }

    @Test void imageUrlsReachListingReviewsVouchesSearchAndLogin() throws Exception {
        var buyerPhoto = service.upload(user.getId(), image(), null);
        User owner = account(); owner.setAccessType(AccessTypeEnum.LISTER); users.saveAndFlush(owner);
        var ownerPhoto = service.upload(owner.getId(), image(), null);
        Listing listing = new Listing(); listing.setLister(owner); listing.setListingTitle("Profile listing " + UUID.randomUUID());
        listing.setListingCategory("Catering"); listing.setDescription("Test"); listing.setServiceType("Cooking");
        listing.setSuburb("Suburb"); listing.setState("State"); listing.setPostcode("1000");
        listing.setAvailability("MONDAY"); listing.setPricingType(ListingPricingType.FREE); listings.saveAndFlush(listing);
        var review = new ListingReview(); review.setListing(listing); review.setReviewer(user); review.setRating(5);
        review.setComment("Test"); review.setStatus(ReviewStatus.APPROVED); reviews.saveAndFlush(review);
        var request = new com.drukconnect.drukconnect.entity.authentication.VouchRequestEntity();
        request.setRequester(owner); request.setTarget(user); request.setMessage("Please vouch");
        request.setStatus(VouchRequestStatus.ACCEPTED); vouchRequests.saveAndFlush(request);
        var vouch = new com.drukconnect.drukconnect.entity.authentication.Vouch(); vouch.setVoucherUser(user);
        vouch.setVouchedUser(owner); vouch.setRequest(request); vouch.setStatus(VouchStatus.ACTIVE); vouches.saveAndFlush(vouch);
        mvc.perform(get("/api/v1/listings/" + listing.getId())).andExpect(status().isOk())
            .andExpect(jsonPath("$.lister.profileImageUrl").value(ownerPhoto.profileImageUrl()))
            .andExpect(jsonPath("$.vouches").doesNotExist())
            .andExpect(jsonPath("$.lister.vouches[0].profileImageUrl").value(buyerPhoto.profileImageUrl()))
            .andExpect(jsonPath("$.reviews[0].profileImageUrl").value(buyerPhoto.profileImageUrl()));
        mvc.perform(get("/api/v1/listings/search").param("q", listing.getListingTitle())).andExpect(status().isOk())
            .andExpect(jsonPath("$.listings[0].listerProfileImageUrl").value(ownerPhoto.profileImageUrl()));
        mvc.perform(get("/api/v1/vouch-requests/users/" + owner.getId() + "/search").param("query",user.getEmail()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].profileImageUrl").value(buyerPhoto.profileImageUrl()));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
            .content("{\"identifier\":\""+user.getEmail()+"\",\"password\":\""+PASSWORD+"\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tokens.user.profileImageUrl").value(buyerPhoto.profileImageUrl()));
    }
    @AfterAll static void cleanup() throws Exception {
        try(var paths=Files.walk(root)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
    }
}
