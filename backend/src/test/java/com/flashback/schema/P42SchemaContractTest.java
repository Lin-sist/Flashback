package com.flashback.schema;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class P42SchemaContractTest {

    @Test
    void mysqlH2AndMappersMustShareMemoryAgencyContract() throws Exception {
        String mysql = Files.readString(Path.of("sql/mysql/schema.mysql.sql"));
        String h2 = Files.readString(Path.of("src/test/resources/schema.sql"));
        String sessionMapper = Files.readString(Path.of("src/main/resources/mapper/AgentSessionMapper.xml"));
        String recordMapper = Files.readString(Path.of("src/main/resources/mapper/RecordMapper.xml"));
        String sourceMapper = Files.readString(Path.of("src/main/resources/mapper/AgentMemorySourceMapper.xml"));

        for (String source : new String[] { mysql, h2 }) {
            assertThat(source).contains("`cross_record_memory_enabled` TINYINT(1) NOT NULL DEFAULT 0");
            assertThat(source).contains("`agent_memory_excluded` TINYINT(1) NOT NULL DEFAULT 0");
            assertThat(source).contains("`agent_memory_context_note` VARCHAR(255)");
            assertThat(source).contains("CREATE TABLE `agent_memory_source`");
            assertThat(source).contains("ON DELETE SET NULL");
        }
        assertThat(sessionMapper).contains(
                "property=\"crossRecordMemoryEnabled\" column=\"cross_record_memory_enabled\"",
                "cross_record_memory_enabled = #{crossRecordMemoryEnabled}");
        assertThat(recordMapper).contains(
                "agent_memory_excluded = 0",
                "agent_memory_excluded = #{excluded}");
        assertThat(sourceMapper).contains(
                "source_kind",
                "assistant_message_id");
        assertThat(sourceMapper).doesNotContain(
                "content",
                "summary",
                "context_note",
                "keyword",
                "score",
                "prompt",
                "reply");
    }

    @Test
    void migrationMustBeIdempotentAndMustNotBackfillMessageSources() throws Exception {
        String migration = Files.readString(Path.of("sql/mysql/p42-memory-agency.sql"));

        assertThat(migration).contains(
                "ADD COLUMN `cross_record_memory_enabled` TINYINT(1) NOT NULL DEFAULT 0",
                "ADD COLUMN `agent_memory_excluded` TINYINT(1) NOT NULL DEFAULT 0",
                "CREATE TABLE IF NOT EXISTS `agent_memory_source`",
                "ON DELETE SET NULL");
        assertThat(migration).doesNotContain(
                "UPDATE `agent_message`",
                "UPDATE `agent_turn_trace`",
                "INSERT INTO `agent_memory_source`");
    }
}
