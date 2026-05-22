package com.divinecorner.controller;

import com.divinecorner.dto.response.ApiResponse;
import com.divinecorner.dto.response.CloudinaryUploadResponse;
import com.divinecorner.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/cloudinary")
@RequiredArgsConstructor
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    /**
     * Upload a single image
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CloudinaryUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file) {
        CloudinaryUploadResponse response = cloudinaryService.uploadImage(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload multiple images
     */
    @PostMapping("/upload-multiple")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CloudinaryUploadResponse>> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files) {
        List<CloudinaryUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            responses.add(cloudinaryService.uploadImage(file));
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * Delete an image
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteImage(@RequestParam String publicId) {
        boolean deleted = cloudinaryService.deleteImage(publicId);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(deleted)
                .message(deleted ? "Image deleted successfully" : "Failed to delete image")
                .build());
    }
}
