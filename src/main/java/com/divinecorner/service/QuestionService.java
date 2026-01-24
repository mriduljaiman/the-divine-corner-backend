package com.divinecorner.service;

import com.divinecorner.dto.request.AnswerQuestionRequest;
import com.divinecorner.dto.request.CreateQuestionRequest;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.dto.response.QuestionResponse;
import com.divinecorner.entity.Product;
import com.divinecorner.entity.ProductQuestion;
import com.divinecorner.entity.User;
import com.divinecorner.exception.NotFoundException;
import com.divinecorner.repository.ProductQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final ProductQuestionRepository questionRepository;
    private final ProductService productService;

    @Transactional
    public QuestionResponse askQuestion(CreateQuestionRequest request, User user) {
        Product product = productService.findProductById(request.getProductId());

        ProductQuestion question = ProductQuestion.builder()
                .product(product)
                .user(user)
                .question(request.getQuestion())
                .build();

        question = questionRepository.save(question);
        return mapToResponse(question);
    }

    @Transactional
    public QuestionResponse answerQuestion(UUID questionId, AnswerQuestionRequest request, User admin) {
        ProductQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("Question not found"));

        question.setAnswer(request.getAnswer());
        question.setAnsweredBy(admin);
        question.setAnsweredAt(LocalDateTime.now());

        question = questionRepository.save(question);
        return mapToResponse(question);
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> getProductQuestions(UUID productId, Pageable pageable) {
        Page<ProductQuestion> page = questionRepository.findByProductIdAndActiveTrue(productId, pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getUnansweredQuestions() {
        return questionRepository.findByAnswerIsNullAndActiveTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    private QuestionResponse mapToResponse(ProductQuestion q) {
        return QuestionResponse.builder()
                .id(q.getId())
                .productId(q.getProduct().getId())
                .productName(q.getProduct().getName())
                .askedBy(QuestionResponse.UserBasicInfo.builder()
                        .id(q.getUser().getId())
                        .firstName(q.getUser().getFirstName())
                        .lastName(q.getUser().getLastName())
                        .build())
                .question(q.getQuestion())
                .answer(q.getAnswer())
                .answeredBy(q.getAnsweredBy() != null ? QuestionResponse.UserBasicInfo.builder()
                        .id(q.getAnsweredBy().getId())
                        .firstName(q.getAnsweredBy().getFirstName())
                        .lastName(q.getAnsweredBy().getLastName())
                        .build() : null)
                .answeredAt(q.getAnsweredAt())
                .helpfulCount(q.getHelpfulCount())
                .createdAt(q.getCreatedAt())
                .build();
    }

    private PageResponse<QuestionResponse> mapToPageResponse(Page<ProductQuestion> page) {
        return PageResponse.<QuestionResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
