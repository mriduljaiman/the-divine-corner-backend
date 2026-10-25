package com.divinecorner.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Small, idempotent schema changes that Hibernate's ddl-auto=update cannot make
 * (it never relaxes NOT NULL). Each step checks first, so it runs once per database.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SchemaFixups implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection c = dataSource.getConnection()) {
            if (!c.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql")) return;
        } catch (Exception e) {
            log.warn("Schema fixups skipped: {}", e.getMessage());
            return;
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // Manual due entries may contain items that are not inventory products.
        // A failure here must not stop the shop from running, so it is only logged.
        try {
            String nullable = jdbc.queryForObject(
                    "SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() " +
                            "AND TABLE_NAME = 'offline_bill_items' AND COLUMN_NAME = 'product_id'", String.class);
            if ("NO".equalsIgnoreCase(nullable)) {
                jdbc.execute("ALTER TABLE offline_bill_items MODIFY product_id BINARY(16) NULL");
                log.info("Schema fixup: offline_bill_items.product_id is now nullable");
            }
        } catch (Exception e) {
            log.error("Schema fixup for offline_bill_items.product_id failed: {}", e.getMessage());
        }
    }
}
