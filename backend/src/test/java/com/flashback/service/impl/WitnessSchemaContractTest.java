package com.flashback.service.impl;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WitnessSchemaContractTest {

    @Test
    void mysqlH2AndMapperMustShareIntentAndWitnessContract() throws Exception {
        String mysql = Files.readString(Path.of("sql/mysql/schema.mysql.sql"));
        String h2 = Files.readString(Path.of("src/test/resources/schema.sql"));
        String mapper = Files.readString(Path.of("src/main/resources/mapper/AgentSessionMapper.xml"));

        for (String source : new String[] { mysql, h2 }) {
            assertThat(source).contains(
                    "`conversation_intent` VARCHAR(24) DEFAULT NULL",
                    "`stage` VARCHAR(30) NOT NULL DEFAULT 'WITNESS'");
        }
        assertThat(mapper).contains(
                "property=\"conversationIntent\" column=\"conversation_intent\"",
                "conversation_intent = #{conversationIntent}",
                "purpose = 'WRITING_GUIDANCE'",
                "status = 'ACTIVE'");
    }

    @Test
    void migrationMustNormalizeOnlyCurrentSessionFacts() throws Exception {
        String migration = Files.readString(Path.of("sql/mysql/p41-witness-agent-alignment.sql"));

        assertThat(migration).contains(
                "ADD COLUMN `conversation_intent` VARCHAR(24) NULL",
                "`conversation_intent` = 'LISTEN'",
                "`stage` = 'WITNESS'",
                "`stage_reask_count` = 0",
                "`purpose` = 'REVIEW_CHAT'");
        assertThat(migration).doesNotContain(
                "UPDATE `agent_message`",
                "UPDATE `agent_turn_trace`");
    }
}
