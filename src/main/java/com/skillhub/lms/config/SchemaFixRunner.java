package com.skillhub.lms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Adds missing columns to existing tables so the app can start against DBs created
 * before schema changes (e.g. updated_at on courses, users, sections).
 */
@Component
@Order(-200)
@RequiredArgsConstructor
@Slf4j
public class SchemaFixRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        addColumnIfMissing("courses", "updated_at", "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)");
        addColumnIfMissing("courses", "slug", "VARCHAR(255) NULL");
        addColumnIfMissing("courses", "is_published", "BIT NOT NULL DEFAULT 0");
        publishAllExistingCourses();
        addColumnIfMissing("users", "updated_at", "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)");
        addColumnIfMissing("sections", "created_at", "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)");
        addColumnIfMissing("sections", "updated_at", "DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    table, column
            );
            if (rows.isEmpty()) {
                String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
                jdbcTemplate.execute(sql);
                log.info("Added column {}.{}", table, column);
            }
        } catch (Exception e) {
            log.warn("Could not add column {}.{}: {}", table, column, e.getMessage());
        }
    }

    /** Ensure all existing courses are published so they appear in the course list. */
    private void publishAllExistingCourses() {
        try {
            int updated = jdbcTemplate.update("UPDATE courses SET is_published = 1 WHERE is_published = 0 OR is_published IS NULL");
            if (updated > 0) {
                log.info("Published {} existing course(s) so they appear in the UI.", updated);
            }
        } catch (Exception e) {
            log.warn("Could not publish existing courses: {}", e.getMessage());
        }
    }
}
