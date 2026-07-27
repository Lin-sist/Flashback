package com.flashback.agent.tool;

import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 提议校验测试（C2）。
 *
 * 覆盖重点：白名单边界、strict mode 无法表达的代码层边界、排除项拒绝。
 */
class AgentToolValidatorTest {

    private static final boolean HAS_DRAFT = true;

    private AppAgentProperties properties;
    private AgentToolValidator validator;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        properties.setMaxToolContentChars(20);
        properties.setMaxToolTagIds(3);
        properties.setMaxReplyChars(120);
        Clock clock = Clock.fixed(Instant.parse("2026-07-27T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        validator = new AgentToolValidator(new AgentToolRegistry(), properties, clock);
    }

    // ---------- 白名单 ----------

    @Test
    void shouldAcceptWhitelistedAppendContent() {
        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("要不要放进正文？", "撑不住", null, null), HAS_DRAFT);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.proposal().tool()).isEqualTo(AgentToolName.APPEND_RECORD_CONTENT);
        assertThat(result.proposal().text()).isEqualTo("撑不住");
        assertThat(result.proposal().askText()).isEqualTo("要不要放进正文？");
    }

    @Test
    void shouldRejectUnknownToolName() {
        AgentToolValidationResult result = validator.validate(
                "delete_everything", args("删掉吧", "x", null, null), HAS_DRAFT);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_NOT_ALLOWLISTED);
    }

    /**
     * 不可逆操作绝不可达：即便模型提议，也在白名单层被拒。
     */
    @Test
    void shouldRejectIrreversibleOperations() {
        List<String> forbidden = List.of(
                "seal", "seal_record", "delete_record", "unlock_record",
                "update_location", "update_cover", "add_attachment",
                "update_later_reflection", "create_tag");

        for (String wireName : forbidden) {
            AgentToolValidationResult result = validator.validate(
                    wireName, args("好吗？", "x", null, null), HAS_DRAFT);
            assertThat(result.isAccepted())
                    .as("工具 %s 不应可达", wireName)
                    .isFalse();
            assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_NOT_ALLOWLISTED);
        }
    }

    /**
     * 读工具由后端预注入，不可被模型提议。
     */
    @Test
    void shouldRejectReadToolsAsProposal() {
        assertThat(validator.validate("list_available_tags", args("看看标签", null, null, null), HAS_DRAFT)
                .rejectReason()).isEqualTo(AgentToolValidationResult.REASON_NOT_ALLOWLISTED);
        assertThat(validator.validate("read_draft_snapshot", args("看看草稿", null, null, null), HAS_DRAFT)
                .rejectReason()).isEqualTo(AgentToolValidationResult.REASON_NOT_ALLOWLISTED);
    }

    // ---------- 参数 ----------

    @Test
    void shouldRejectMissingAskText() {
        AgentToolValidationResult result = validator.validate(
                "append_record_content", args(null, "撑不住", null, null), HAS_DRAFT);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    @Test
    void shouldRejectMissingText() {
        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("好吗？", "   ", null, null), HAS_DRAFT);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    /**
     * strict mode 不支持 maxLength，长度上限必须由代码层拦下。
     */
    @Test
    void shouldRejectTextBeyondCodeLevelLimit() {
        String tooLong = "字".repeat(properties.getMaxToolContentChars() + 1);

        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("好吗？", tooLong, null, null), HAS_DRAFT);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
    }

    /**
     * strict mode 不支持 maxItems，数量上限必须由代码层拦下。
     */
    @Test
    void shouldRejectTagIdsBeyondCodeLevelLimit() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags", args("加标签？", null, List.of(1L, 2L, 3L, 4L), null), HAS_DRAFT);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
    }

    @Test
    void shouldDeduplicateTagIds() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags", args("加标签？", null, List.of(7L, 7L, 8L), null), HAS_DRAFT);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.proposal().tagIds()).containsExactly(7L, 8L);
    }

    @Test
    void shouldRejectEmptyTagIds() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags", args("加标签？", null, List.of(), null), HAS_DRAFT);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    @Test
    void shouldRejectNonPositiveTagId() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags", args("加标签？", null, List.of(0L), null), HAS_DRAFT);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    // ---------- unlockAt ----------

    @Test
    void shouldAcceptFutureUnlockAt() {
        AgentToolValidationResult result = validator.validate(
                "propose_unlock_at", args("留到明年？", null, null, "2027-01-01T09:00:00"), HAS_DRAFT);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.proposal().unlockAt()).isEqualTo("2027-01-01T09:00");
    }

    /**
     * 「晚于当前时间」是业务边界，strict 的 pattern 只能校验形状。
     */
    @Test
    void shouldRejectPastUnlockAt() {
        AgentToolValidationResult result = validator.validate(
                "propose_unlock_at", args("留到去年？", null, null, "2020-01-01T09:00:00"), HAS_DRAFT);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
    }

    @Test
    void shouldRejectMalformedUnlockAt() {
        AgentToolValidationResult result = validator.validate(
                "propose_unlock_at", args("留到某天？", null, null, "明年今天"), HAS_DRAFT);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    // ---------- 草稿上下文 ----------

    @Test
    void shouldRejectWhenSessionHasNoDraft() {
        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("好吗？", "撑不住", null, null), false);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_NO_DRAFT_CONTEXT);
    }

    @Test
    void shouldRejectNullArguments() {
        AgentToolValidationResult result = validator.validate("append_record_content", null, HAS_DRAFT);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    private AgentToolRawArguments args(String askText, String text, List<Long> tagIds, String unlockAt) {
        return new AgentToolRawArguments(askText, text, tagIds, unlockAt);
    }
}
