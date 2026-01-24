package com.divinecorner.repository;

import com.divinecorner.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);
    List<Category> findByActiveTrue();
    Page<Category> findByActiveTrue(Pageable pageable);
    boolean existsByName(String name);
}