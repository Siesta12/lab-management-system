package com.nlt.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationSlotSchemaCompatibilityRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        dropLegacyUniqueIndex("uk_lab_reservation_slot_lab_date_period_active");
        dropLegacyUniqueIndex("uk_lab_reservation_slot_lab_date_period");
        dropLegacyColumn("active_flag");
        ensureIndex(
            "idx_lab_reservation_slot_lab_date_period_status",
            "alter table lab_reservation_slot "
                + "add index idx_lab_reservation_slot_lab_date_period_status "
                + "(lab_id, reservation_date, period_id, slot_status)"
        );
    }

    private void dropLegacyUniqueIndex(String indexName) {
        if (!indexExists(indexName)) {
            return;
        }
        jdbcTemplate.execute("alter table lab_reservation_slot drop index " + indexName);
        log.info("Dropped legacy reservation slot index: {}", indexName);
    }

    private void dropLegacyColumn(String columnName) {
        if (!columnExists(columnName)) {
            return;
        }
        jdbcTemplate.execute("alter table lab_reservation_slot drop column " + columnName);
        log.info("Dropped legacy reservation slot column: {}", columnName);
    }

    private void ensureIndex(String indexName, String ddl) {
        if (indexExists(indexName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("Created reservation slot compatibility index: {}", indexName);
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
            "select count(1) from information_schema.statistics "
                + "where table_schema = database() "
                + "and table_name = 'lab_reservation_slot' "
                + "and index_name = ?",
            Integer.class,
            indexName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "select count(1) from information_schema.columns "
                + "where table_schema = database() "
                + "and table_name = 'lab_reservation_slot' "
                + "and column_name = ?",
            Integer.class,
            columnName
        );
        return count != null && count > 0;
    }
}
