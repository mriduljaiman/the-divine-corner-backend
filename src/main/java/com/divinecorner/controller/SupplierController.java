package com.divinecorner.controller;

import com.divinecorner.entity.Supplier;
import com.divinecorner.exception.BadRequestException;
import com.divinecorner.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Suppliers used by the Stock IN form.
 */
@RestController
@RequestMapping("/suppliers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getActiveSuppliers() {
        return ResponseEntity.ok(supplierRepository.findByActiveTrue().stream()
                .map(this::toMap)
                .toList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSupplier(@RequestBody Map<String, String> request) {
        String name = request.getOrDefault("name", "").trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Supplier name is required");
        }
        Supplier supplier = Supplier.builder()
                .name(name)
                .contactPerson(request.get("contactPerson"))
                .phone(request.get("phone"))
                .email(request.get("email"))
                .gstNumber(request.get("gstNumber"))
                .active(true)
                .build();
        return ResponseEntity.ok(toMap(supplierRepository.save(supplier)));
    }

    private Map<String, Object> toMap(Supplier s) {
        return Map.of(
                "id", s.getId(),
                "name", s.getName(),
                "phone", s.getPhone() != null ? s.getPhone() : "",
                "contactPerson", s.getContactPerson() != null ? s.getContactPerson() : "");
    }
}
