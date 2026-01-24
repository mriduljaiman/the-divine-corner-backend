package com.divinecorner.service;

import com.divinecorner.dto.request.CreateReviewRequest;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.dto.response.ReviewResponse;
import com.divinecorner.entity.Product;
import com.divinecorner.entity.Review;
import com.divinecorner.entity.User;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.repository.ReviewRepository;
import com.divinecorner.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, User user) {
        // Check if user already reviewed this product
        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), user.getId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        Product product = productService.findProductById(request.getProductId());

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .images(request.getImages() != null ? request.getImages() : new ArrayList<>())
                .build();

        review = reviewRepository.save(review);

        // Update product rating
        updateProductRating(request.getProductId());

        return mapToResponse(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(UUID productId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByProductIdAndActiveTrue(productId, pageable);
        return PageResponse.<ReviewResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public void updateProductRating(UUID productId) {
        Double avgRating = reviewRepository.getAverageRatingByProductId(productId);
        long reviewCount = reviewRepository.countByProductIdAndActiveTrue(productId);

        Product product = productService.findProductById(productId);
        product.setRating(avgRating != null ? avgRating : 0.0);
        product.setReviewCount((int) reviewCount);
        productRepository.save(product);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .user(ReviewResponse.UserBasicInfo.builder()
                        .id(review.getUser().getId())
                        .firstName(review.getUser().getFirstName())
                        .lastName(review.getUser().getLastName())
                        .build())
                .rating(review.getRating())
                .comment(review.getComment())
                .images(review.getImages())
                .verified(review.getVerified())
                .helpfulCount(review.getHelpfulCount())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
