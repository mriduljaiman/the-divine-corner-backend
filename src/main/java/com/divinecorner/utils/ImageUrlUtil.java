package com.divinecorner.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ImageUrlUtil {

    @Value("${app.image.base-url:}")
    private String baseUrl;

    /**
     * Convert relative image paths to absolute URLs
     */
    public String toAbsoluteUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        // If already absolute URL, return as is
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }

        // If baseUrl is configured, prepend it
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl + (imagePath.startsWith("/") ? imagePath : "/" + imagePath);
        }

        // Return as is if no base URL configured
        return imagePath;
    }

    /**
     * Convert list of image paths to absolute URLs
     */
    public List<String> toAbsoluteUrls(List<String> imagePaths) {
        if (imagePaths == null) {
            return List.of();
        }

        return imagePaths.stream()
                .map(this::toAbsoluteUrl)
                .filter(url -> url != null && !url.isEmpty())
                .collect(Collectors.toList());
    }
}