package com.drukconnect.drukconnect.service.profile;

import com.drukconnect.drukconnect.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileImageStorageService {
    private final Path root;

    public ProfileImageStorageService(@Value("${app.storage.profile-images-dir:uploads/profiles}") String directory) throws IOException {
        root = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String store(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024
                || !Set.of("image/jpeg", "image/png").contains(String.valueOf(file.getContentType()))) {
            throw invalid("Choose a JPEG or PNG image up to 5 MB.");
        }
        Path destination = root.resolve(userId + "/" + UUID.randomUUID() + ".png");
        try (var input = file.getInputStream(); var imageInput = ImageIO.createImageInputStream(input)) {
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw invalid("The file is not a readable image.");
            var reader = readers.next();
            BufferedImage source;
            try {
                reader.setInput(imageInput);
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!Set.of("jpeg", "jpg", "png").contains(format)) throw invalid("Only JPEG and PNG images are supported.");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 16_000_000) throw invalid("Image dimensions must not exceed 16 megapixels.");
                source = reader.read(0);
            } finally { reader.dispose(); }
            double scale = Math.min(1, 1024.0 / Math.max(source.getWidth(), source.getHeight()));
            BufferedImage output = new BufferedImage(Math.max(1, (int) (source.getWidth() * scale)),
                    Math.max(1, (int) (source.getHeight() * scale)), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = output.createGraphics();
            try { graphics.drawImage(source, 0, 0, output.getWidth(), output.getHeight(), null); }
            finally { graphics.dispose(); }
            Files.createDirectories(destination.getParent());
            // Re-encoding validates the content and discards uploaded metadata, including GPS data.
            if (!ImageIO.write(output, "png", destination.toFile())) throw new IOException("PNG writer unavailable");
            return root.relativize(destination).toString().replace('\\', '/');
        } catch (ApiException exception) { throw exception; }
        catch (IOException | RuntimeException exception) {
            try { Files.deleteIfExists(destination); } catch (IOException ignored) { }
            throw invalid("Unable to read or save the profile image.");
        }
    }

    public Resource load(String relativePath) {
        try {
            Path file = resolve(relativePath);
            if (!Files.isRegularFile(file)) throw new IOException("Image missing");
            return new UrlResource(file.toUri());
        } catch (IOException exception) { throw new ApiException(HttpStatus.NOT_FOUND, "PROFILE_IMAGE_NOT_FOUND", "Profile image not found"); }
    }

    public void delete(String relativePath) {
        if (relativePath == null) return;
        try { Files.deleteIfExists(resolve(relativePath)); }
        catch (IOException exception) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not clean up profile image", exception); }
    }

    private Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) throw invalid("Invalid image path.");
        return path;
    }
    private ApiException invalid(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PROFILE_IMAGE", message); }
}
