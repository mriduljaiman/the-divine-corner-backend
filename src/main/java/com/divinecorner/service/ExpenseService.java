package com.divinecorner.service;

import com.divinecorner.dto.pos.PosDtos.ExpenseRequest;
import com.divinecorner.dto.pos.PosDtos.ExpenseResponse;
import com.divinecorner.entity.Expense;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.exception.NotFoundException;
import com.divinecorner.repository.ExpenseRepository;
import com.divinecorner.repository.UserRepository;
import com.divinecorner.utils.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : TimeUtil.shopToday().withDayOfMonth(1);
        LocalDate end = to != null ? to : TimeUtil.shopToday();
        if (end.isBefore(start)) {
            throw new BadRequestException("'to' date must be after 'from' date");
        }
        return expenseRepository.findByExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExpenseResponse create(UUID adminId, ExpenseRequest request) {
        Expense expense = new Expense();
        apply(expense, request);
        expense.setCreatedBy(userRepository.findById(adminId).orElse(null));
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(UUID id, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(id).orElseThrow(() -> new NotFoundException("Expense not found"));
        apply(expense, request);
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(UUID id) {
        if (!expenseRepository.existsById(id)) {
            throw new NotFoundException("Expense not found");
        }
        expenseRepository.deleteById(id);
    }

    private void apply(Expense e, ExpenseRequest r) {
        LocalDate date = r.expenseDate() != null ? r.expenseDate() : TimeUtil.shopToday();
        if (date.isAfter(TimeUtil.shopToday())) {
            throw new BadRequestException("Expense date cannot be in the future");
        }
        e.setExpenseDate(date);
        e.setCategory(r.category().trim());
        e.setAmount(r.amount().setScale(2, RoundingMode.HALF_UP));
        e.setPaymentMethod(r.paymentMethod() == null || r.paymentMethod().isBlank() ? "CASH" : r.paymentMethod().trim().toUpperCase());
        e.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
    }

    private ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(e.getId(), e.getExpenseDate(), e.getCategory(), e.getAmount(), e.getPaymentMethod(),
                e.getNote(), TimeUtil.format(e.getCreatedAt()));
    }

    BigDecimal total(LocalDate from, LocalDate to) {
        return expenseRepository.total(from, to);
    }
}
