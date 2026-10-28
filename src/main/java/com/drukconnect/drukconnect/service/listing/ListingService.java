package com.drukconnect.drukconnect.service.listing;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.util.ListingAvailabilityDays;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.listing.*;
import com.drukconnect.drukconnect.entity.listing.*;
import com.drukconnect.drukconnect.entity.authentication.*;
import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.authentication.VouchStatus;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.repository.listing.*;
import com.drukconnect.drukconnect.repository.authentication.*;
import com.drukconnect.drukconnect.service.authentication.AuditService;
import com.drukconnect.drukconnect.service.moderation.ListerRestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.*;

@Service
public class ListingService {

    private final ListingRepository listingRepository;

    private final ListingImageRepository imageRepository;

    private final ListingReviewRepository reviewRepository;

    private final UserRepository userRepository;

    private final VouchRepository vouchRepository;

    private final ListingImageStorageService imageStorageService;

    private final AuditService auditService;

    @Autowired
    private final ListerRestrictionService
            listerRestrictionService;

    public ListingService(
            ListingRepository listingRepository,
            ListingImageRepository imageRepository,
            ListingReviewRepository reviewRepository,
            UserRepository userRepository,
            VouchRepository vouchRepository,
            ListingImageStorageService imageStorageService,
            AuditService auditService, ListerRestrictionService listerRestrictionService
    ) {

        this.listingRepository =
                listingRepository;

        this.imageRepository =
                imageRepository;

        this.reviewRepository =
                reviewRepository;

        this.userRepository =
                userRepository;

        this.vouchRepository =
                vouchRepository;

        this.imageStorageService =
                imageStorageService;

        this.auditService =
                auditService;
        this.listerRestrictionService = listerRestrictionService;
    }

    /*
     * =========================================================
     * CREATE LISTING
     * =========================================================
     */
    @Transactional
    public ListingDetailResponse create(
            UUID currentUserId,
            CreateListingRequest request,
            List<MultipartFile> images,
            RequestMetadata meta
    ) {

        User lister =
                userRepository
                        .findById(
                                currentUserId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User not found"
                                )
                        );


        /*
         * =========================================================
         * ONLY LISTER CAN CREATE LISTING
         * =========================================================
         */
        if (
                lister.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "LISTER_REQUIRED",
                    "Only Lister accounts can create listings"
            );
        }


        /*
         * =========================================================
         * SCAM REPORT RESTRICTION CHECK
         * =========================================================
         *
         * User remains ACTIVE.
         * User can login normally.
         *
         * But if an ADMIN approved a scam report against
         * this LISTER, listing creation is blocked.
         */
        listerRestrictionService
                .assertCanCreateListing(
                        currentUserId
                );


        /*
         * =========================================================
         * REQUIRE 1 - 5 IMAGES
         * =========================================================
         */
        if (
                images == null
                        ||
                        images.isEmpty()
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "LISTING_IMAGE_REQUIRED",
                    "At least one listing image is required"
            );
        }


        if (
                images.size() > 5
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "MAX_IMAGES_EXCEEDED",
                    "A listing can contain a maximum of 5 images"
            );
        }


        /*
         * =========================================================
         * PRICING VALIDATION
         * =========================================================
         */
        if (
                request.pricingType()
                        == ListingPricingType.PAID

                        &&

                        (
                                request.rateAmount()
                                        == null

                                        ||

                                        request.rateAmount()
                                                .compareTo(
                                                        BigDecimal.ZERO
                                                )
                                                <= 0
                        )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "RATE_REQUIRED",
                    "Rate amount is required for paid listings"
            );
        }


        /*
         * =========================================================
         * CREATE LISTING
         * =========================================================
         */
        Listing listing =
                new Listing();


        listing.setLister(
                lister
        );


        listing.setListingTitle(
                request.listingTitle()
                        .trim()
        );


        listing.setListingCategory(
                request.listingCategory()
                        .trim()
        );


        listing.setSuburb(
                request.suburb()
                        .trim()
        );


        listing.setState(
                request.state()
                        .trim()
        );


        listing.setPostcode(
                request.postcode()
                        .trim()
        );


        listing.setDescription(
                request.description()
                        .trim()
        );


        listing.setServiceType(
                request.serviceType()
                        .trim()
        );


        listing.setDietaryOptions(
                request.dietaryOptions()
                        == null
                        ? new HashSet<>()
                        : new HashSet<>(
                        request.dietaryOptions()
                )
        );


        listing.setAvailability(
                ListingAvailabilityDays.normalize(request.availability())
        );


        listing.setPricingType(
                request.pricingType()
        );


        if (
                request.pricingType()
                        == ListingPricingType.FREE
        ) {

            listing.setRateAmount(
                    null
            );

        } else {

            listing.setRateAmount(
                    request.rateAmount()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
            );
        }


        listing.setCurrencyCode(
                "AUD"
        );


        listing.setViews(
                0L
        );


        listing.setStatus(
                ListingStatus.ACTIVE
        );


        listing =
                listingRepository
                        .saveAndFlush(
                                listing
                        );


        /*
         * =========================================================
         * SAVE IMAGES
         * =========================================================
         */
        List<String> createdImagePaths = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) createdImagePaths.forEach(ListingService.this::deleteImageQuietly);
            }
        });
        for (
                int index = 0;
                index < images.size();
                index++
        ) {

            MultipartFile file =
                    images.get(
                            index
                    );


            String path =
                    imageStorageService
                            .store(
                                    listing.getId(),
                                    file
                            );


            createdImagePaths.add(path);

            ListingImage image =
                    new ListingImage();


            image.setListing(
                    listing
            );


            image.setImagePath(
                    path
            );


            image.setOriginalFilename(
                    file.getOriginalFilename()
            );


            image.setContentType(
                    file.getContentType()
            );


            image.setSortOrder(
                    index + 1
            );


            imageRepository
                    .save(
                            image
                    );
        }


        imageRepository.flush();


        /*
         * =========================================================
         * AUDIT
         * =========================================================
         */
        auditService.log(
                "LISTING_CREATED",
                currentUserId,
                null,
                null,
                null,
                meta,
                "{\"listingId\":\""
                        + listing.getId()
                        + "\"}"
        );


        return buildDetail(
                listing
        );
    }

    /*
     * =========================================================
     * PUBLIC GET ALL
     * =========================================================
     */
    @Transactional(readOnly = true)
    public PagedListingResponse getAll(
            int page,
            int size
    ) {

        int safePage =
                Math.max(
                        page,
                        1
                );

        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        50
                );

        Pageable pageable =
                PageRequest.of(
                        safePage - 1,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Page<Listing> result =
                listingRepository
                        .findByStatus(
                                ListingStatus.ACTIVE,
                                pageable
                        );

        List<ListingSummaryResponse> listings =
                result
                        .getContent()
                        .stream()
                        .map(
                                this::buildSummary
                        )
                        .toList();

        return new PagedListingResponse(
                listings,
                result.getNumber() + 1,
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    /*
     * =========================================================
     * PUBLIC GET DETAIL
     *
     * EACH CALL INCREASES VIEWS.
     * =========================================================
     */
    @Transactional
    public ListingDetailResponse getById(
            UUID listingId,
            RequestMetadata meta
    ) {

        int updated =
                listingRepository
                        .incrementViews(
                                listingId,
                                ListingStatus.ACTIVE
                        );

        if (
                updated == 0
        ) {

            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "LISTING_NOT_FOUND",
                    "Listing not found"
            );
        }

        Listing listing =
                listingRepository
                        .findByIdAndStatus(
                                listingId,
                                ListingStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "LISTING_NOT_FOUND",
                                        "Listing not found"
                                )
                        );

        /*
         * Public view audit.
         *
         * This can later be disabled if traffic gets high.
         */
        auditService.log(
                "LISTING_VIEWED",
                null,
                null,
                listing.getLister()
                        .getId(),
                null,
                meta,
                "{\"listingId\":\""
                        + listingId
                        + "\"}"
        );

        return buildDetail(
                listing
        );
    }

    private ListingSummaryResponse buildSummary(
            Listing listing
    ) {

        RatingSummary rating =
                ratingSummary(
                        listing.getId()
                );

        return new ListingSummaryResponse(

                listing.getId(),

                listing.getListingTitle(),

                listing.getListingCategory(),

                listing.getSuburb(),

                listing.getState(),

                listing.getPostcode(),

                listing.getServiceType(),

                listing.getAvailability(),

                listing.getPricingType(),

                listing.getRateAmount(),

                listing.getCurrencyCode(),

                listing.getViews(),

                imageResponses(
                        listing.getId()
                ),

                rating.totalReviews(),

                rating.averageRating(),

                rating.averageRatingStar(),

                fullName(
                        listing.getLister()
                ),
                       listing.getLister().getProfileImageUrl()
               );
    }

    private ListingDetailResponse buildDetail(
            Listing listing
    ) {

        RatingSummary rating =
                ratingSummary(
                        listing.getId()
                );

        /*
         * =========================================================
         * APPROVED REVIEWS
         * =========================================================
         */
        List<ListingReviewResponse> reviews =
                reviewRepository
                        .findByListingIdAndStatusOrderBySubmittedAtDesc(
                                listing.getId(),
                                ReviewStatus.APPROVED
                        )
                        .stream()
                        .map(
                                review ->
                                        new ListingReviewResponse(

                                                review.getId(),

                                                review.getReviewer()
                                                        .getId(),

                                                fullName(
                                                        review.getReviewer()
                                                ),

                                                review.getRating(),

                                                review.getComment(),

                                                new LinkedHashSet<>(
                                                        review.getTags()
                                                ),

                                                review.getSubmittedAt(),
                                                review.getReviewer().getProfileImageUrl()
                                        )
                        )
                        .toList();

        /*
         * =========================================================
         * LISTER
         * =========================================================
         */
        User lister =
                listing.getLister();

        /*
         * =========================================================
         * ACTIVE VOUCHES
         * =========================================================
         */
        List<Vouch> activeVouches =
                vouchRepository
                        .findByVouchedUserIdAndStatusOrderByVouchedAtDesc(
                                lister.getId(),
                                VouchStatus.ACTIVE
                        );

        List<ListingVouchResponse> vouches =
                activeVouches
                        .stream()
                        .map(
                                vouch ->
                                        new ListingVouchResponse(

                                                vouch.getId(),

                                                vouch.getVoucherUser()
                                                        .getId(),

                                                fullName(
                                                        vouch.getVoucherUser()
                                                ),

                                                vouch.getVouchedAt(),
                                                vouch.getVoucherUser().getProfileImageUrl()
                                        )
                        )
                        .toList();

        /*
         * =========================================================
         * CONTACT LINKS
         * =========================================================
         */
        String whatsapp =
                whatsappLink(
                        lister.getPhoneNumber()
                );

        ListingOwnerResponse owner =
                new ListingOwnerResponse(

                        lister.getId(),

                        fullName(
                                lister
                        ),

                        lister.getEmail(),

                        "mailto:"
                                + lister.getEmail(),

                        lister.getPhoneNumber(),

                        whatsapp,

                        activeVouches.size(),

                        vouches,
                        lister.getProfileImageUrl()
                );

        /*
         * =========================================================
         * DIETARY OPTIONS
         *
         * IMPORTANT:
         * Force lazy collection to load while transaction
         * is still active and convert it to a normal Set.
         * =========================================================
         */
        Set<String> dietaryOptions =
                new LinkedHashSet<>(
                        listing.getDietaryOptions()
                );

        return new ListingDetailResponse(

                listing.getId(),

                listing.getListingTitle(),

                listing.getListingCategory(),

                listing.getSuburb(),

                listing.getState(),

                listing.getPostcode(),

                listing.getDescription(),

                listing.getServiceType(),

                dietaryOptions,

                listing.getAvailability(),

                listing.getPricingType(),

                listing.getRateAmount(),

                listing.getCurrencyCode(),

                listing.getViews(),

                imageResponses(
                        listing.getId()
                ),

                rating.totalReviews(),

                rating.averageRating(),

                rating.averageRatingStar(),

                owner,

                reviews,

                listing.getCreatedAt()
        );
    }

    private List<ListingImageResponse> imageResponses(
            UUID listingId
    ) {

        return imageRepository
                .findByListingIdOrderBySortOrderAsc(
                        listingId
                )
                .stream()
                .map(
                        image ->
                                new ListingImageResponse(

                                        image.getId(),

                                        "/api/v1/listings/images/"
                                                + image.getId(),

                                        image.getSortOrder()
                                )
                )
                .toList();
    }

    private RatingSummary ratingSummary(
            UUID listingId
    ) {

        long total =
                reviewRepository
                        .countByListingIdAndStatus(
                                listingId,
                                ReviewStatus.APPROVED
                        );

        Double average =
                reviewRepository
                        .getAverageRating(
                                listingId,
                                ReviewStatus.APPROVED
                        );

        if (
                average == null
        ) {

            return new RatingSummary(
                    0,
                    BigDecimal.ZERO
                            .setScale(2),
                    0
            );
        }

        BigDecimal averageRating =
                BigDecimal
                        .valueOf(
                                average
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        int star =
                (int)
                        Math.round(
                                average
                        );

        return new RatingSummary(
                total,
                averageRating,
                star
        );
    }

    private String fullName(
            User user
    ) {

        return (
                user.getFirstName()
                        + " "
                        + user.getLastName()
        ).trim();
    }

    private String whatsappLink(
            String phone
    ) {

        if (
                phone == null
                        ||
                        phone.isBlank()
        ) {
            return null;
        }

        String digits =
                phone.replaceAll(
                        "\\D",
                        ""
                );

        if (
                digits.isBlank()
        ) {
            return null;
        }

        return "https://wa.me/"
                + digits;
    }

    private record RatingSummary(

            long totalReviews,

            BigDecimal averageRating,

            int averageRatingStar

    ) {
    }

    private User requireLister(
            UUID userId
    ) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User not found"
                                )
                        );

        if (
                user.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "LISTER_REQUIRED",
                    "Only Lister accounts can manage listings"
            );
        }

        return user;
    }


    @Transactional(readOnly = true)
    public PagedMyListingResponse getMyListings(
            UUID currentUserId,
            int page,
            int size
    ) {

        requireLister(
                currentUserId
        );

        int safePage =
                Math.max(
                        page,
                        1
                );

        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        50
                );

        Pageable pageable =
                PageRequest.of(
                        safePage - 1,
                        safeSize,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        Page<Listing> result =
                listingRepository
                        .findByListerIdAndStatusNot(
                                currentUserId,
                                ListingStatus.DELETED,
                                pageable
                        );

        List<MyListingResponse> listings =
                result.getContent()
                        .stream()
                        .map(
                                this::buildMyListingResponse
                        )
                        .toList();

        return new PagedMyListingResponse(

                listings,

                result.getNumber() + 1,

                result.getSize(),

                result.getTotalElements(),

                result.getTotalPages(),

                result.hasNext(),

                result.hasPrevious()
        );
    }

    private MyListingResponse buildMyListingResponse(
            Listing listing
    ) {

        RatingSummary rating =
                ratingSummary(
                        listing.getId()
                );

        return new MyListingResponse(

                listing.getId(),

                listing.getListingTitle(),

                listing.getListingCategory(),

                listing.getSuburb(),

                listing.getState(),

                listing.getPostcode(),

                listing.getServiceType(),

                listing.getAvailability(),

                listing.getPricingType(),

                listing.getRateAmount(),

                listing.getCurrencyCode(),

                listing.getViews(),

                listing.getStatus(),

                imageResponses(
                        listing.getId()
                ),

                rating.totalReviews(),

                rating.averageRating(),

                rating.averageRatingStar(),

                listing.getCreatedAt(),

                listing.getUpdatedAt()
        );
    }

    @Transactional
    public ListingDetailResponse updateListing(
            UUID currentUserId,
            UUID listingId,
            UpdateListingRequest request,
            RequestMetadata meta
    ) {

        return updateListing(currentUserId, listingId, request, List.of(), meta);
    }

    @Transactional
    public ListingDetailResponse updateListing(UUID currentUserId, UUID listingId,
            UpdateListingRequest request, List<MultipartFile> images, RequestMetadata meta) {
        requireLister(
                currentUserId
        );

        Listing listing =
                listingRepository
                        .findByIdAndListerIdAndStatusNot(
                                listingId,
                                currentUserId,
                                ListingStatus.DELETED
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "LISTING_NOT_FOUND",
                                        "Listing not found"
                                )
                        );

        /*
         * At least one field must be supplied.
         */
        if (
                request.pricingType() == null
                        &&
                        request.rateAmount() == null
                        &&
                        request.availability() == null
                        && request.retainedImageIds() == null
                        && (images == null || images.isEmpty())
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "NO_CHANGES_PROVIDED",
                    "Provide pricing, availability or images to update"
            );
        }

        /*
         * =========================================================
         * AVAILABILITY
         * =========================================================
         */
        if (
                request.availability() != null
        ) {

            listing.setAvailability(
                    ListingAvailabilityDays.normalize(request.availability())
            );
        }

        /*
         * =========================================================
         * PRICING
         * =========================================================
         */

        ListingPricingType targetPricingType =
                request.pricingType() != null
                        ? request.pricingType()
                        : listing.getPricingType();

        /*
         * FREE
         */
        if (
                targetPricingType
                        == ListingPricingType.FREE
        ) {

            /*
             * Do not allow:
             *
             * pricingType = FREE
             * rateAmount = 35
             */
            if (
                    request.rateAmount() != null
                            &&
                            request.rateAmount()
                                    .compareTo(
                                            BigDecimal.ZERO
                                    ) > 0
            ) {

                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "RATE_NOT_ALLOWED_FOR_FREE_LISTING",
                        "Rate amount must not be provided when the listing is free"
                );
            }

            listing.setPricingType(
                    ListingPricingType.FREE
            );

            listing.setRateAmount(
                    null
            );
        }

        /*
         * PAID
         */
        if (
                targetPricingType
                        == ListingPricingType.PAID
        ) {

            BigDecimal rateAmount =
                    request.rateAmount() != null
                            ? request.rateAmount()
                            : listing.getRateAmount();

            if (
                    rateAmount == null
                            ||
                            rateAmount.compareTo(
                                    BigDecimal.ZERO
                            ) <= 0
            ) {

                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "RATE_REQUIRED",
                        "A rate amount greater than zero is required for paid listings"
                );
            }

            listing.setPricingType(
                    ListingPricingType.PAID
            );

            listing.setRateAmount(
                    rateAmount.setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
            );
        }

        if (request.retainedImageIds() != null || (images != null && !images.isEmpty())) {
            updateImages(listing, request.retainedImageIds(), images == null ? List.of() : images);
        }

        listing =
                listingRepository
                        .saveAndFlush(
                                listing
                        );

        auditService.log(
                "LISTING_UPDATED",
                currentUserId,
                null,
                null,
                null,
                meta,
                "{"
                        + "\"listingId\":\""
                        + listingId
                        + "\","
                        + "\"pricingType\":\""
                        + listing.getPricingType()
                        + "\","
                        + "\"rateAmount\":"
                        + (
                        listing.getRateAmount() == null
                                ? "null"
                                : listing.getRateAmount()
                )
                        + ","
                        + "\"availability\":\""
                        + listing.getAvailability()
                        + "\""
                        + "}"
        );

        return buildDetail(
                listing
        );
    }

    private void updateImages(Listing listing, List<UUID> retainedIds, List<MultipartFile> uploads) {
        List<ListingImage> existing = imageRepository.findByListingIdOrderBySortOrderAsc(listing.getId());
        List<UUID> keep = retainedIds == null ? existing.stream().map(ListingImage::getId).toList() : retainedIds;
        Set<UUID> ownIds = new HashSet<>(existing.stream().map(ListingImage::getId).toList());
        if (new HashSet<>(keep).size() != keep.size() || !ownIds.containsAll(keep)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LISTING_IMAGES", "Retained images must be unique images belonging to this listing.");
        }
        int count = keep.size() + uploads.size();
        if (count < 1 || count > 5) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_COUNT", "A listing must contain between 1 and 5 images.");
        List<String> createdPaths = new ArrayList<>();
        List<ListingImage> removed = existing.stream().filter(image -> !keep.contains(image.getId())).toList();
        // Files and database changes must agree even if an upload or transaction fails.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) createdPaths.forEach(ListingService.this::deleteImageQuietly);
            }
            @Override public void afterCommit() {
                removed.forEach(image -> deleteImageQuietly(image.getImagePath()));
            }
        });
        int order = 0;
        for (UUID id : keep) {
            ListingImage image = existing.stream().filter(item -> item.getId().equals(id)).findFirst().orElseThrow();
            image.setSortOrder(order++);
        }
        imageRepository.deleteAll(removed);
        for (MultipartFile file : uploads) {
            String path = imageStorageService.store(listing.getId(), file);
            createdPaths.add(path);
            ListingImage image = new ListingImage();
            image.setListing(listing); image.setImagePath(path); image.setOriginalFilename(file.getOriginalFilename());
            image.setContentType(file.getContentType()); image.setSortOrder(order++);
            imageRepository.save(image);
        }
        imageRepository.flush();
    }

    private void deleteImageQuietly(String path) {
        try { imageStorageService.delete(path); }
        catch (RuntimeException exception) {
            org.slf4j.LoggerFactory.getLogger(ListingService.class).warn("Unable to clean up listing image {}", path, exception);
        }
    }

    @Transactional
    public void deleteListing(
            UUID currentUserId,
            UUID listingId,
            RequestMetadata meta
    ) {

        requireLister(
                currentUserId
        );

        Listing listing =
                listingRepository
                        .findByIdAndListerIdAndStatusNot(
                                listingId,
                                currentUserId,
                                ListingStatus.DELETED
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "LISTING_NOT_FOUND",
                                        "Listing not found"
                                )
                        );

        /*
         * Soft delete.
         *
         * Keep DB history, reviews,
         * images and audit records.
         */
        listing.setStatus(
                ListingStatus.DELETED
        );

        listingRepository
                .saveAndFlush(
                        listing
                );

        auditService.log(
                "LISTING_DELETED",
                currentUserId,
                null,
                null,
                null,
                meta,
                "{\"listingId\":\""
                        + listingId
                        + "\"}"
        );
    }

    @Transactional(readOnly = true)
    public PagedListingResponse searchListings(

            String category,

            String suburb,

            String state,

            String postcode,

            String query,

            int page,

            int size
    ) {

        /*
         * =========================================================
         * PAGINATION
         * =========================================================
         */

        int safePage =
                Math.max(
                        page,
                        1
                );

        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        50
                );


        /*
         * =========================================================
         * NORMALIZE FILTERS
         * =========================================================
         *
         * Empty values become null so the repository
         * ignores that filter.
         *
         * Examples:
         *
         * category=""
         * becomes null
         *
         * city=""
         * becomes null
         *
         * q=""
         * becomes null
         *
         * =========================================================
         */

        String normalizedCategory =
                normalizeSearchValue(
                        category
                );

        String normalizedSuburb =
                normalizeSearchValue(
                        suburb
                );

        String normalizedState =
                normalizeSearchValue(
                        state
                );

        String normalizedPostcode =
                normalizeSearchValue(
                        postcode
                );

        String normalizedQuery =
                normalizeSearchValue(
                        query
                );


        Pageable pageable =
                PageRequest.of(

                        safePage - 1,

                        safeSize,

                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );


        /*
         * =========================================================
         * DATABASE SEARCH
         * =========================================================
         */

        Page<Listing> result =
                listingRepository
                        .searchListings(

                                ListingStatus.ACTIVE,

                                normalizedCategory,

                                normalizedSuburb,

                                normalizedState,

                                normalizedPostcode,

                                normalizedQuery,

                                pageable
                        );

        /*
         * =========================================================
         * USE SAME RESPONSE AS GET ALL
         * =========================================================
         */

        List<ListingSummaryResponse> listings =
                result
                        .getContent()
                        .stream()
                        .map(
                                this::buildSummary
                        )
                        .toList();


        return new PagedListingResponse(

                listings,

                result.getNumber() + 1,

                result.getSize(),

                result.getTotalElements(),

                result.getTotalPages(),

                result.hasNext(),

                result.hasPrevious()
        );
    }

    private String normalizeSearchValue(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return null;
        }

        return value.trim();
    }
}
