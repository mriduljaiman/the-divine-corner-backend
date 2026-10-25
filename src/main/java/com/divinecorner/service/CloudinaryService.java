package com.divinecorner.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.divinecorner.dto.response.CloudinaryUploadResponse;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.utils.ImageCompressor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String folder;

    private final List<String> allowedContentTypes = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final long maxFileSize = 10 * 1024 * 1024; // 10MB

    /**
     * Upload a single image to Cloudinary
     */
    public CloudinaryUploadResponse uploadImage(MultipartFile file) {
        validateFile(file);

        try {
            byte[] jpeg = compressed(file);
            String publicId = folder + "/" + UUID.randomUUID().toString();

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    jpeg,
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image",
                            "folder", folder,
                            "format", "jpg"
                    )
            );
            ensureStoredUnderLimit(uploadResult);

            log.info("Image uploaded: {} ({} bytes, original {} bytes)", uploadResult.get("public_id"), jpeg.length, file.getSize());

            return CloudinaryUploadResponse.builder()
                    .publicId((String) uploadResult.get("public_id"))
                    .url((String) uploadResult.get("secure_url"))
                    .format((String) uploadResult.get("format"))
                    .width(((Number) uploadResult.get("width")).intValue())
                    .height(((Number) uploadResult.get("height")).intValue())
                    .bytes(((Number) uploadResult.get("bytes")).longValue())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Upload image with custom transformations (for thumbnails)
     */
    public CloudinaryUploadResponse uploadImageWithTransformation(
            MultipartFile file, int width, int height) {
        validateFile(file);

        try {
            byte[] jpeg = compressed(file);
            String publicId = folder + "/" + UUID.randomUUID().toString();

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    jpeg,
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "image",
                            "folder", folder,
                            "eager", ObjectUtils.asArray(
                                    ObjectUtils.asMap(
                                            "width", width,
                                            "height", height,
                                            "crop", "fill",
                                            "gravity", "auto"
                                    )
                            )
                    )
            );
            ensureStoredUnderLimit(uploadResult);

            return CloudinaryUploadResponse.builder()
                    .publicId((String) uploadResult.get("public_id"))
                    .url((String) uploadResult.get("secure_url"))
                    .format((String) uploadResult.get("format"))
                    .width(((Number) uploadResult.get("width")).intValue())
                    .height(((Number) uploadResult.get("height")).intValue())
                    .bytes(((Number) uploadResult.get("bytes")).longValue())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    /**
     * Delete an image from Cloudinary
     */
    public boolean deleteImage(String publicId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "image")
            );

            String deleteResult = (String) result.get("result");
            boolean success = "ok".equals(deleteResult);

            if (success) {
                log.info("Image deleted successfully: {}", publicId);
            } else {
                log.warn("Image deletion returned: {} for publicId: {}", deleteResult, publicId);
            }

            return success;
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", publicId, e);
            return false;
        }
    }

    /**
     * Generate optimized URL with transformations
     */
    public String getOptimizedUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new com.cloudinary.Transformation()
                        .width(width)
                        .height(height)
                        .crop("fill")
                        .gravity("auto")
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }

    /** Every stored image must be at most 20KB: re-encode on the server whatever the client sent. */
    private byte[] compressed(MultipartFile file) throws IOException {
        return ImageCompressor.toMaxSizeJpeg(file.getBytes());
    }

    /** Belt and braces: if Cloudinary reports a bigger asset, remove it and fail the upload. */
    private void ensureStoredUnderLimit(Map<String, Object> uploadResult) {
        Object bytes = uploadResult.get("bytes");
        if (bytes instanceof Number n && n.longValue() > ImageCompressor.MAX_BYTES) {
            deleteImage((String) uploadResult.get("public_id"));
            throw new BadRequestException("Image could not be stored under 20KB, please try another photo");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException("File size exceeds maximum limit of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid file type. Allowed types: JPEG, PNG, GIF, WebP"
            );
        }
    }
}
