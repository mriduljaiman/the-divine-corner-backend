package com.divinecorner.controller;

import com.divinecorner.dto.response.ApiResponse;
import com.divinecorner.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Upload a single file
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        String filename = fileStorageService.storeFile(file);
        String fileUrl = baseUrl + "/uploads/" + filename;

        Map<String, String> response = new HashMap<>();
        response.put("filename", filename);
        response.put("url", fileUrl);

        return ResponseEntity.ok(response);
    }

    /**
     * Upload multiple files
     */
    @PostMapping("/upload-multiple")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<Map<String, String>> uploadedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            String filename = fileStorageService.storeFile(file);
            String fileUrl = baseUrl + "/uploads/" + filename;

            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("filename", filename);
            fileInfo.put("url", fileUrl);
            uploadedFiles.add(fileInfo);
        }

        return ResponseEntity.ok(uploadedFiles);
    }

    /**
     * Delete a file
     */
    @DeleteMapping("/{filename}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteFile(@PathVariable String filename) {
        fileStorageService.deleteFile(filename);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("File deleted successfully")
                .build());
    }
}
