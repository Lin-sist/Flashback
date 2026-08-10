package com.flashback.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class P31SchemaContractTest {

    @Test
    void shouldKeepFullAndIncrementalSchemasAlignedWithPresentMomentLifecycle() throws IOException {
        String fullSchema = Files.readString(Path.of("sql/mysql/schema.mysql.sql"));
        String testSchema = Files.readString(Path.of("src/test/resources/schema.sql"));
        String migration = Files.readString(Path.of("sql/mysql/p31-present-moment-capture.sql"));

        for (String schema : new String[]{fullSchema, testSchema}) {
            assertThat(schema).contains("`record_type` VARCHAR(30) NOT NULL DEFAULT 'MOMENT'");
            assertThat(schema).contains("`draft_expires_at` DATETIME DEFAULT NULL");
            assertThat(schema).contains("idx_record_status_draft_expires");
        }

        assertThat(migration)
                .contains("ADD COLUMN `draft_expires_at` DATETIME NULL")
                .contains("ALTER COLUMN `record_type` SET DEFAULT 'MOMENT'")
                .contains("ra.status = 'AVAILABLE'")
                .contains("ra.type IN ('IMAGE', 'VOICE')")
                .contains("SET status = 'SAVED'")
                .contains("SET draft_expires_at = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY)")
                .contains("GROUP BY status");
    }
}
