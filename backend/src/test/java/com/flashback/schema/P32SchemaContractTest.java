package com.flashback.schema;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class P32SchemaContractTest {

    @Test
    void shouldKeepOwnershipOperationSchemasAligned() throws IOException {
        String fullSchema = Files.readString(Path.of("sql/mysql/schema.mysql.sql"));
        String testSchema = Files.readString(Path.of("src/test/resources/schema.sql"));
        String migration = Files.readString(Path.of("sql/mysql/p32-data-ownership-foundation.sql"));

        for (String schema : new String[]{fullSchema, testSchema}) {
            assertThat(schema).contains("CREATE TABLE `data_operation`");
            assertThat(schema).contains("CREATE TABLE `data_operation_record`");
            assertThat(schema).contains("idx_data_operation_user_status");
            assertThat(schema).contains("uk_data_operation_user_active");
            assertThat(schema).contains("idx_data_operation_record_operation_status");
            assertThat(schema).contains("ON DELETE SET NULL");
        }

        assertThat(migration)
                .contains("CREATE TABLE IF NOT EXISTS `data_operation`")
                .contains("CREATE TABLE IF NOT EXISTS `data_operation_record`")
                .contains("confirmation_nonce_hash")
                .contains("artifact_expires_at")
                .contains("GROUP BY operation_type, status");
    }
}
