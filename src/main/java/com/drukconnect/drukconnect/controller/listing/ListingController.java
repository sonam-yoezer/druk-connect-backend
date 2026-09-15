package com.drukconnect.drukconnect.controller.listing;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.listing.*;
import com.drukconnect.drukconnect.entity.listing.ListingImage;
import com.drukconnect.drukconnect.repository.listing.ListingImageRepository;
import com.drukconnect.drukconnect.service.listing.ListingImageStorageService;
import com.drukconnect.drukconnect.service.listing.ListingService;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {

    private final ListingService listingService;

    private final ListingImageRepository imageRepository;

    private final ListingImageStorageService imageStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    public ListingController(
            ListingService listingService,
            ListingImageRepository imageRepository,
            ListingImageStorageService imageStorageService
    ) {

        this.listingService =
                listingService;

        this.imageRepository =
                imageRepository;

        this.imageStorageService =
                imageStorageService;
    }

    /*
     * =========================================================
     * LISTER CREATE
     * =========================================================
     */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ListingDetailResponse create(
            @AuthenticationPrincipal Jwt jwt,

            @RequestParam("data")
            String data,

            @RequestParam("images")
            List<MultipartFile> images,

            HttpServletRequest httpRequest
    ) {

        UUID userId =
                UUID.fromString(
                        jwt.getSubject()
                );

        CreateListingRequest request;

        try {

            request =
                    objectMapper.readValue(
                            data,
                            CreateListingRequest.class
                    );

        } catch (Exception ex) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_LISTING_DATA",
                    "Invalid listing data format"
            );
        }

        return listingService.create(
                userId,
                request,
                images,
                RequestMetadata.from(
                        httpRequest
                )
        );
    }

    /*
     * =========================================================
     * PUBLIC GET ALL
     * NO JWT REQUIRED
     * =========================================================
     */
    @GetMapping
    public PagedListingResponse getAll(

            @RequestParam(
                    defaultValue = "1"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size
    ) {

        return listingService.getAll(
                page,
                size
        );
    }

    /*
     * =========================================================
     * PUBLIC GET DETAIL
     *
     * INCREASES VIEW COUNT.
     * =========================================================
     */
    @GetMapping("/{listingId}")
    public ListingDetailResponse getById(
            @PathVariable
            UUID listingId,

            HttpServletRequest request
    ) {

        return listingService.getById(
                listingId,
                RequestMetadata.from(
                        request
                )
        );
    }

    /*
     * =========================================================
     * PUBLIC IMAGE
     * =========================================================
     */
    @GetMapping(
            "/images/{imageId}"
    )
    public ResponseEntity<Resource> getImage(
            @PathVariable
            UUID imageId
    ) {

        ListingImage image =
                imageRepository
                        .findById(
                                imageId
                        )
                        .orElseThrow();

        Resource resource =
                imageStorageService
                        .load(
                                image.getImagePath()
                        );

        MediaType mediaType;

        try {

            mediaType =
                    MediaType.parseMediaType(
                            image.getContentType()
                    );

        } catch (Exception ex) {

            mediaType =
                    MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity
                .ok()
                .contentType(
                        mediaType
                )
                .cacheControl(
                        CacheControl.maxAge(
                                java.time.Duration
                                        .ofHours(1)
                        )
                )
                .body(
                        resource
                );
    }

    @GetMapping("/getMyListings")
    public PagedMyListingResponse getMyListings(

            @AuthenticationPrincipal
            Jwt jwt,

            @RequestParam(
                    defaultValue = "1"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size
    ) {

        UUID userId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return listingService
                .getMyListings(
                        userId,
                        page,
                        size
                );
    }


    @PatchMapping("/{listingId}")
    public ListingDetailResponse updateListing(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID listingId,

            @Valid
            @RequestBody
            UpdateListingRequest request,

            HttpServletRequest httpRequest
    ) {

        UUID userId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return listingService
                .updateListing(
                        userId,
                        listingId,
                        request,
                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(
            HttpStatus.NO_CONTENT
    )
    public void deleteListing(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID listingId,

            HttpServletRequest httpRequest
    ) {

        UUID userId =
                UUID.fromString(
                        jwt.getSubject()
                );

        listingService.deleteListing(
                userId,
                listingId,
                RequestMetadata.from(
                        httpRequest
                )
        );
    }

    @GetMapping("/search")
    public PagedListingResponse searchListings(

            @RequestParam(
                    required = false
            )
            String category,

            @RequestParam(
                    required = false
            )
            String city,

            @RequestParam(
                    name = "q",
                    required = false
            )
            String query,

            @RequestParam(
                    defaultValue = "1"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size
    ) {

        return listingService
                .searchListings(

                        category,

                        city,

                        query,

                        page,

                        size
                );
    }
}