package com.drukconnect.drukconnect.service.listing;

import com.drukconnect.drukconnect.common.ApiException;
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

    public ListingService(
            ListingRepository listingRepository,
            ListingImageRepository imageRepository,
            ListingReviewRepository reviewRepository,
            UserRepository userRepository,
            VouchRepository vouchRepository,
            ListingImageStorageService imageStorageService,
            AuditService auditService
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
         * Require 1 - 5 images.
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
         * Pricing validation.
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

        Listing listing =
                new Listing();

        listing.setLister(
                lister
        );

        listing.setListingTitle(
                request.listingTitle()
                        .trim()
        );

        listing.setCity(
                request.city()
                        .trim()
        );

        listing.setDescription(
                request.description()
                        .trim()
        );

        listing.setCuisine(
                request.cuisine()
                        .trim()
        );

        listing.setServiceType(
                request.serviceType()
                        .trim()
        );

        listing.setMinimumOrder(
                request.minimumOrder()
        );

        listing.setServes(
                request.serves()
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
                request.availability()
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
         * Save images.
         */
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

                listing.getCity(),

                listing.getCuisine(),

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
                )
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

                                                /*
                                                 * IMPORTANT:
                                                 * Copy lazy Hibernate collection
                                                 * into a normal Java Set.
                                                 */
                                                new LinkedHashSet<>(
                                                        review.getTags()
                                                ),

                                                review.getSubmittedAt()
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

                                                vouch.getVouchedAt()
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

                        vouches
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

                listing.getCity(),

                listing.getDescription(),

                listing.getCuisine(),

                listing.getServiceType(),

                listing.getMinimumOrder(),

                listing.getServes(),

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
}
