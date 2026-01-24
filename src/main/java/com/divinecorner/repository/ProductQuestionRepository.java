package com.divinecorner.repository;

import com.divinecorner.entity.ProductQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, UUID> {
    Page<ProductQuestion> findByProductIdAndActiveTrue(UUID productId, Pageable pageable);

    List<ProductQuestion> findByProductIdAndActiveTrue(UUID productId);

    List<ProductQuestion> findByAnswerIsNullAndActiveTrue();

    long countByProductIdAndActiveTrue(UUID productId);
}
