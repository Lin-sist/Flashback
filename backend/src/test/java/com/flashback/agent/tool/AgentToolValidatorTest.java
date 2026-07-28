package com.flashback.agent.tool;

import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentSourceCorpus;
import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 提议校验测试（C2 + C4）。
 *
 * 覆盖重点：白名单边界、strict mode 无法表达的代码层边界、排除项拒绝，
 * 以及 C4 新增的内容忠实度维度。
 *
 * C4 说明：validate 新增来源集合参数。**刻意不提供无来源集合的重载**——
 * 那会造出一条「绕过忠实度检查即可产生提议」的路径，与 design 决策 2
 * 「校验点必须唯一」相悖。因此本测试统一传入语料。
 */
class AgentToolValidatorTest {

    private static final boolean HAS_DRAFT = true;

    private AppAgentProperties properties;
    private AgentToolValidator validator;
    /** 用户原话语料：包含下方用例里出现的素材文本。 */
    private AgentSourceCorpus corpus;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        properties.setMaxToolContentChars(20);
        properties.setMaxToolTagIds(3);
        properties.setMaxReplyChars(120);
        Clock clock = Clock.fixed(Instant.parse("2026-07-27T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        AgentFaithfulnessChecker faithfulnessChecker = new AgentFaithfulnessChecker(properties);
        AgentContentChecker contentChecker = new AgentContentChecker(properties, faithfulnessChecker);
        validator = new AgentToolValidator(
                new AgentToolRegistry(), properties, faithfulnessChecker, contentChecker, clock);
        corpus = AgentSourceCorpus.ofTexts(
                List.of("撑不住"), properties.getGuardrail().getFaithfulnessNgramSize());
    }

    // ---------- 白名单 ----------

    @Test
    void shouldAcceptWhitelistedAppendContent() {
        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("要不要放进正文？", "撑不住", null, null), HAS_DRAFT, corpus);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.proposal().tool()).isEqualTo(AgentToolName.APPEND_RECORD_CONTENT);
        assertThat(result.proposal().text()).isEqualTo("撑不住");
        assertThat(result.proposal().askText()).isEqualTo("要不要放进正文？");
    }

    @Test
    void shouldRejectUnknownToolName() {
        AgentToolValidationResult result = validator.validate(
                "delete_everything", args("删掉吧", "x", null, null), HAS_DRAFT, corpus);

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
                    wireName, args("好吗？", "x", null, null), HAS_DRAFT, corpus);
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
        assertThat(validator.validate(
                "list_available_tags", args("看看标签", null, null, null), HAS_DRAFT, corpus)
                .rejectReason()).isEqualTo(AgentToolValidationResult.REASON_NOT_ALLOWLISTED);
        assertThat(validator.validate(
                "read_draft_snapshot", args("看看草稿", null, null, null), HAS_DRAFT, corpus)
                .rejectReason()).isEqualTo(AgentToolValidationResult.REASON_NOT_ALLOWLISTED);
    }

    // ---------- 参数 ----------

    @Test
    void shouldRejectMissingAskText() {
        AgentToolValidationResult result = validator.validate(
                "append_record_content", args(null, "撑不住", null, null), HAS_DRAFT, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    @Test
    void shouldRejectMissingText() {
        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("好吗？", "   ", null, null), HAS_DRAFT, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    /**
     * strict mode 不支持 maxLength，长度上限必须由代码层拦下。
     */
    @Test
    void shouldRejectTextBeyondCodeLevelLimit() {
        String tooLong = "字".repeat(properties.getMaxToolContentChars() + 1);

        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("好吗？", tooLong, null, null), HAS_DRAFT, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
    }

    /**
     * strict mode 不支持 maxItems，数量上限必须由代码层拦下。
     */
    @Test
    void shouldRejectTagIdsBeyondCodeLevelLimit() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags", args("加标签？", null, List.of(1L, 2L, 3L, 4L), null), HAS_DRAFT, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
    }

    @Test
    void shouldDeduplicateTagIds() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags", args("加标签？", null, List.of(7L, 7L, 8L), null), HAS_DRAFT, corpus);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.proposal().tagIds()).containsExactly(7L, 8L);
    }

    @Test
    void shouldRejectEmptyTagIds() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags", args("加标签？", null, List.of(), null), HAS_DRAFT, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    @Test
    void shouldRejectNonPositiveTagId() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags", args("加标签？", null, List.of(0L), null), HAS_DRAFT, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    // ---------- unlockAt ----------

    @Test
    void shouldAcceptFutureUnlockAt() {
        AgentToolValidationResult result = validator.validate(
                "propose_unlock_at", args("留到明年？", null, null, "2027-01-01T09:00:00"), HAS_DRAFT, corpus);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.proposal().unlockAt()).isEqualTo("2027-01-01T09:00");
    }

    /**
     * 「晚于当前时间」是业务边界，strict 的 pattern 只能校验形状。
     */
    @Test
    void shouldRejectPastUnlockAt() {
        AgentToolValidationResult result = validator.validate(
                "propose_unlock_at", args("留到去年？", null, null, "2020-01-01T09:00:00"), HAS_DRAFT, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_OUT_OF_BOUNDS);
    }

    @Test
    void shouldRejectMalformedUnlockAt() {
        AgentToolValidationResult result = validator.validate(
                "propose_unlock_at", args("留到某天？", null, null, "明年今天"), HAS_DRAFT, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    // ---------- 草稿上下文 ----------

    @Test
    void shouldRejectWhenSessionHasNoDraft() {
        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("好吗？", "撑不住", null, null), false, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_NO_DRAFT_CONTEXT);
    }

    @Test
    void shouldRejectNullArguments() {
        AgentToolValidationResult result = validator.validate(
                "append_record_content", null, HAS_DRAFT, corpus);

        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_INVALID_ARGUMENT);
    }

    // ---------- C4：内容忠实度 ----------

    /**
     * C4 的核心场景（R1）：参数内容合法、长度合法、工具在白名单内，
     * 但正文里增写了用户从未说过的话。C2 的三道校验全部放行，忠实度闸必须拦下。
     */
    @Test
    void shouldRejectAppendContentThatFabricatesUserWords() {
        properties.setMaxToolContentChars(300);
        AgentSourceCorpus r1 = AgentSourceCorpus.ofTexts(
                List.of("我学的是软件工程，一直想做后端", "刚才说的这些我觉得挺重要的，想留下来"),
                properties.getGuardrail().getFaithfulnessNgramSize());
        String fabricated = "我学的是软件工程，一直想做后端。"
                + "但最近心里有点空，不知道该不该继续沿着这条路走下去，方向是不是对的，自己也说不清楚。";

        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("我帮你整理了一下，放进正文？", fabricated, null, null),
                HAS_DRAFT, r1);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_UNFAITHFUL_ARGS);
    }

    /**
     * 只是整理语序、去掉标点的素材必须放行——否则护栏会把正常能力也拦死。
     */
    @Test
    void shouldAcceptAppendContentThatOnlyReorganizes() {
        properties.setMaxToolContentChars(300);
        AgentSourceCorpus source = AgentSourceCorpus.ofTexts(
                List.of("工作上有点撑不住", "主要是排期太紧"),
                properties.getGuardrail().getFaithfulnessNgramSize());

        AgentToolValidationResult result = validator.validate(
                "append_record_content", args("放进正文？", "工作上有点撑不住，主要是排期太紧", null, null),
                HAS_DRAFT, source);

        assertThat(result.isAccepted()).isTrue();
    }

    /**
     * askText 是唯一显示在确认条上的文本：其中引号包裹的伪引用必须被拦。
     */
    @Test
    void shouldRejectFabricatedQuoteInAskText() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags",
                args("你刚才说“我觉得自己撑不下去了，想放弃”，要不要加个标签？", null, List.of(3L), null),
                HAS_DRAFT, corpus);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_FABRICATED_QUOTE);
    }

    /**
     * askText 中的诊断性表述同样被拦——确认条上不该出现病症判断。
     */
    @Test
    void shouldRejectDiagnosticAskText() {
        AgentToolValidationResult result = validator.validate(
                "add_record_tags", args("你这是典型的焦虑症表现，要不要加个标签？", null, List.of(3L), null),
                HAS_DRAFT, corpus);

        assertThat(result.isAccepted()).isFalse();
        assertThat(result.rejectReason()).isEqualTo(AgentToolValidationResult.REASON_ASK_TEXT_VIOLATION);
    }

    private AgentToolRawArguments args(String askText, String text, List<Long> tagIds, String unlockAt) {
        return new AgentToolRawArguments(askText, text, tagIds, unlockAt);
    }
}
