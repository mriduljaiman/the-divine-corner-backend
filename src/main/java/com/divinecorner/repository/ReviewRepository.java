package com.divinecorner.repository;

import com.divinecorner.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductIdAndActiveTrue(UUID productId, Pageable pageable);

    List<Review> findByProductIdAndActiveTrue(UUID productId);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.active = true")
    Double getAverageRatingByProductId(@Param("productId") UUID productId);

    long countByProductIdAndActiveTrue(UUID productId);
}
