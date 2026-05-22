package com.divinecorner.service;

import com.divinecorner.dto.*;
import com.divinecorner.dto.CreateCategoryRequest;
import com.divinecorner.dto.UpdateCategoryRequest;
import com.divinecorner.dto.response.CategoryResponse;
import com.divinecorner.dto.response.PageResponse;
import com.divinecorner.entity.Category;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.exception.NotFoundException;
import com.divinecorner.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    // @CacheEvict(value = "categories", allEntries = true) // Temporarily disabled
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Creating category: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with this name already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .icon(request.getIcon())
                .skuPrefix(request.getSkuPrefix())
                .active(true)
                .build();

        category = categoryRepository.save(category);
        log.info("Category created with ID: {}", category.getId());

        return mapToResponse(category);
    }

    @Transactional(readOnly = true)
    // @Cacheable(value = "categories", key = "'all-active'") // Temporarily disabled due to Redis serialization issue
    public List<CategoryResponse> getAllActiveCategories() {
        log.info("Fetching all active categories from DB");

        return categoryRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAllCategories(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(pageable);
        return mapToPageResponse(page);
    }

    @Transactional(readOnly = true)
    // @Cacheable(value = "categories", key = "#id") // Temporarily disabled
    public CategoryResponse getCategoryById(UUID id) {
        log.info("Fetching category by ID: {}", id);

        Category category = findCategoryById(id);
        return mapToResponse(category);
    }

    @Transactional
    // @CacheEvict(value = "categories", allEntries = true) // Temporarily disabled
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        log.info("Updating category: {}", id);

        Category category = findCategoryById(id);

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            if (categoryRepository.existsByName(request.getName())) {
                throw new BadRequestException("Category with this name already exists");
            }
            category.setName(request.getName());
        }

        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getImageUrl() != null) category.setImageUrl(request.getImageUrl());
        if (request.getIcon() != null) category.setIcon(request.getIcon());
        if (request.getSkuPrefix() != null) category.setSkuPrefix(request.getSkuPrefix());
        if (request.getActive() != null) category.setActive(request.getActive());

        category = categoryRepository.save(category);
        log.info("Category updated: {}", id);

        return mapToResponse(category);
    }

    @Transactional
    // @CacheEvict(value = "categories", allEntries = true) // Temporarily disabled
    public void deleteCategory(UUID id) {
        log.info("Deleting category: {}", id);

        Category category = findCategoryById(id);
        categoryRepository.delete(category);

        log.info("Category deleted: {}", id);
    }

    public Category findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .icon(category.getIcon())
                .skuPrefix(category.getSkuPrefix())
                .active(category.getActive())
                .build();
    }

    private PageResponse<CategoryResponse> mapToPageResponse(Page<Category> page) {
        return PageResponse.<CategoryResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}