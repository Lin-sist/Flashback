package com.flashback.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class P5SchemaContractTest {

    @Test
    void shouldKeepChapterSchemaAlignedAcrossFullTestAndIncrementalFiles() throws IOException {
        String fullSchema = Files.readString(Path.of("sql/mysql/schema.mysql.sql"));
        String testSchema = Files.readString(Path.of("src/test/resources/schema.sql"));
        String migration = Files.readString(Path.of("sql/mysql/p5-time-chapter-foundation.sql"));

        for (String schema : new String[]{fullSchema, testSchema}) {
            assertThat(schema).contains("CREATE TABLE `time_chapter`")
                    .contains("`name` VARCHAR(100) NOT NULL")
                    .contains("`note` VARCHAR(1000) DEFAULT NULL")
                    .contains("`status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'")
                    .contains("`ended_at`")
                    .contains("`version` BIGINT NOT NULL DEFAULT 0")
                    .contains("uk_record_id_user_id")
                    .contains("uk_time_chapter_id_user_id")
                    .contains("uk_time_chapter_record_record_id")
                    .contains("idx_time_chapter_user_created")
                    .contains("idx_time_chapter_record_user_chapter_added")
                    .contains("FOREIGN KEY (`chapter_id`, `user_id`)")
                    .contains("FOREIGN KEY (`record_id`, `user_id`)")
                    .contains("ON DELETE CASCADE")
                    .contains("ck_time_chapter_status_ended_at");
        }

        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS `time_chapter`")
                .contains("CREATE TABLE IF NOT EXISTS `time_chapter_record`")
                .contains("PRIMARY KEY (`chapter_id`, `record_id`)")
                .contains("UNIQUE KEY `uk_time_chapter_record_record_id` (`record_id`)")
                .contains("KEY `idx_time_chapter_user_created` (`user_id`, `created_at`, `id`)")
                .contains("uk_record_id_user_id")
                .contains("UNIQUE KEY `uk_time_chapter_id_user_id` (`id`, `user_id`)")
                .contains("CHECK ((`status` = 'ACTIVE' AND `ended_at` IS NULL)")
                .contains("FOREIGN KEY (`chapter_id`, `user_id`) REFERENCES `time_chapter` (`id`, `user_id`) ON DELETE CASCADE")
                .contains("FOREIGN KEY (`record_id`, `user_id`) REFERENCES `record` (`id`, `user_id`) ON DELETE CASCADE");
    }
}
