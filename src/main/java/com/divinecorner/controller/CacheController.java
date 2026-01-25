package com.divinecorner.controller;

import com.divinecorner.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheManager cacheManager;

    /**
     * Clear all caches - ADMIN only
     */
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("All caches cleared successfully")
                .build());
    }

    /**
     * Clear specific cache - ADMIN only
     */
    @DeleteMapping("/clear/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> clearCache(@PathVariable String cacheName) {
        var cache = cacheManager.getCache(cacheName);

        if (cache != null) {
            cache.clear();
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Cache '" + cacheName + "' cleared successfully")
                    .build());
        }

        return ResponseEntity.badRequest().body(ApiResponse.builder()
                .success(false)
                .message("Cache '" + cacheName + "' not found")
                .build());
    }

    /**
     * List all cache names - ADMIN only
     */
    @GetMapping("/names")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCacheNames() {
        return ResponseEntity.ok(cacheManager.getCacheNames());
    }
}
