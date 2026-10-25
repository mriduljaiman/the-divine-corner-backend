package com.divinecorner.repository;

import com.divinecorner.entity.DueReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DueReminderRepository extends JpaRepository<DueReminder, UUID> {
    Optional<DueReminder> findByCustomerPhone(String customerPhone);
    List<DueReminder> findByCustomerPhoneIn(Collection<String> phones);
}
