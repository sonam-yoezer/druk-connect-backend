package com.drukconnect.drukconnect.service.feedback;

import com.drukconnect.drukconnect.common.ApiException;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.util.Set;
import java.util.UUID;

@Service
public class FeedbackScreenshotStorageService {

    private static final long MAX_SIZE =
            5L * 1024L * 1024L;


    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );


    private final Path uploadDirectory;


    public FeedbackScreenshotStorageService(

            @Value("${app.feedback.upload-dir:uploads/feedback}")
            String uploadDirectory

    ) {

        this.uploadDirectory =
                Path.of(
                        uploadDirectory
                );
    }


    public String store(

            UUID feedbackId,

            MultipartFile file

    ) {

        if (
                file == null
                        ||
                        file.isEmpty()
        ) {

            return null;
        }


        if (
                file.getSize()
                        > MAX_SIZE
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "FEEDBACK_SCREENSHOT_TOO_LARGE",
                    "Feedback screenshot cannot exceed 5 MB"
            );
        }


        if (
                file.getContentType() == null
                        ||
                        !ALLOWED_TYPES.contains(
                                file.getContentType()
                        )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FEEDBACK_SCREENSHOT_TYPE",
                    "Feedback screenshot must be JPEG, PNG or WEBP"
            );
        }


        String extension =
                switch (
                        file.getContentType()
                        ) {

                    case "image/jpeg" -> ".jpg";

                    case "image/png" -> ".png";

                    case "image/webp" -> ".webp";

                    default -> "";
                };


        try {

            Files.createDirectories(
                    uploadDirectory
            );


            String filename =
                    feedbackId
                            + "-"
                            + UUID.randomUUID()
                            + extension;


            Path destination =
                    uploadDirectory
                            .resolve(
                                    filename
                            );


            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );


            return destination
                    .toString();

        } catch (IOException ex) {

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "FEEDBACK_SCREENSHOT_STORAGE_FAILED",
                    "Unable to store feedback screenshot"
            );
        }
    }
}