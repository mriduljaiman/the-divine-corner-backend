package com.divinecorner.repository;

import com.divinecorner.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate >= :from AND e.expenseDate <= :to")
    BigDecimal total(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.expenseDate >= :from AND e.expenseDate <= :to " +
            "GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> byCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
