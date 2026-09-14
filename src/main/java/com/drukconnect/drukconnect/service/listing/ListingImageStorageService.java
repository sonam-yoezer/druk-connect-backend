package com.drukconnect.drukconnect.service.listing;

import com.drukconnect.drukconnect.common.ApiException;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class ListingImageStorageService {

    private static final Set<String>
            ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final Path rootDirectory;

    public ListingImageStorageService(
            @Value(
                    "${app.storage.listing-images-dir:uploads/listings}"
            )
            String storageDirectory
    ) throws IOException {

        this.rootDirectory =
                Paths.get(
                                storageDirectory
                        )
                        .toAbsolutePath()
                        .normalize();

        Files.createDirectories(
                rootDirectory
        );
    }

    public String store(
            UUID listingId,
            MultipartFile file
    ) {

        if (
                file == null
                        ||
                        file.isEmpty()
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "EMPTY_IMAGE",
                    "Listing image cannot be empty"
            );
        }

        String contentType =
                file.getContentType();

        if (
                contentType == null
                        ||
                        !ALLOWED_CONTENT_TYPES
                                .contains(
                                        contentType
                                )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_IMAGE_TYPE",
                    "Only JPEG, PNG and WEBP images are allowed"
            );
        }

        String extension =
                switch (
                        contentType
                        ) {

                    case "image/jpeg"
                            -> ".jpg";

                    case "image/png"
                            -> ".png";

                    case "image/webp"
                            -> ".webp";

                    default
                            -> "";
                };

        String fileName =
                UUID.randomUUID()
                        + extension;

        Path listingDirectory =
                rootDirectory
                        .resolve(
                                listingId.toString()
                        )
                        .normalize();

        Path destination =
                listingDirectory
                        .resolve(
                                fileName
                        )
                        .normalize();

        if (
                !destination.startsWith(
                        rootDirectory
                )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FILE_PATH",
                    "Invalid image path"
            );
        }

        try {

            Files.createDirectories(
                    listingDirectory
            );

            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException ex) {

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "IMAGE_UPLOAD_FAILED",
                    "Unable to save listing image"
            );
        }

        return rootDirectory
                .relativize(
                        destination
                )
                .toString()
                .replace(
                        "\\",
                        "/"
                );
    }

    public Resource load(
            String relativePath
    ) {

        try {

            Path file =
                    rootDirectory
                            .resolve(
                                    relativePath
                            )
                            .normalize();

            if (
                    !file.startsWith(
                            rootDirectory
                    )
            ) {

                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_IMAGE_PATH",
                        "Invalid image path"
                );
            }

            Resource resource =
                    new UrlResource(
                            file.toUri()
                    );

            if (
                    !resource.exists()
                            ||
                            !resource.isReadable()
            ) {

                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "IMAGE_NOT_FOUND",
                        "Listing image not found"
                );
            }

            return resource;

        } catch (Exception ex) {

            if (
                    ex instanceof ApiException api
            ) {
                throw api;
            }

            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "IMAGE_NOT_FOUND",
                    "Listing image not found"
            );
        }
    }
}
