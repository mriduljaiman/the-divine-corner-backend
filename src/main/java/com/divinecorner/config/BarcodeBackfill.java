package com.divinecorner.config;

import com.divinecorner.entity.Product;
import com.divinecorner.repository.ProductRepository;
import com.divinecorner.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gives every existing product a scannable barcode (new products get one on creation).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BarcodeBackfill implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductService productService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> missing = productRepository.findByBarcodeIsNull();
        if (missing.isEmpty()) return;
        for (Product p : missing) {
            p.setBarcode(productService.generateBarcode());
            productRepository.save(p);
        }
        log.info("Assigned barcodes to {} existing products", missing.size());
    }
}
