package com.flashback.agent.tool;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentRawToolCall;
import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.agent.guardrail.AgentSourceCorpus;
import com.flashback.config.AppAgentProperties;
import com.flashback.config.AppAiProperties;
import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentToolCall;
import com.flashback.mapper.AgentToolCallMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 提议编排测试（C2）。
 *
 * 覆盖 design 决策 10（单轮至多一个提议）与决策 2/9
 * （提议阶段绝不执行——本类断言 executor 完全未被触碰）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentToolCoordinatorTest {

    private static final Long USER_ID = 3001L;
    private static final Long SESSION_ID = 3002L;
    private static final Long RECORD_ID = 3003L;

    @Mock
    private AgentToolCallMapper agentToolCallMapper;

    @Mock
    private AgentToolExecutor executor;

    private AgentToolCoordinator coordinator;
    private AgentSourceCorpus corpus;

    @BeforeEach
    void setUp() {
        AppAgentProperties agentProperties = new AppAgentProperties();
        agentProperties.setMaxToolContentChars(300);
        agentProperties.setMaxToolTagIds(5);
        agentProperties.setMaxReplyChars(120);

        AppAiProperties aiProperties = new AppAiProperties();
        aiProperties.setProvider("mock");
        aiProperties.setRealModeMockEnabled(true);

        Clock clock = Clock.fixed(Instant.parse("2026-07-27T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        AgentFaithfulnessChecker faithfulnessChecker = new AgentFaithfulnessChecker(agentProperties);
        AgentContentChecker contentChecker = new AgentContentChecker(agentProperties, faithfulnessChecker);
        AgentGuardrailRules guardrailRules = new AgentGuardrailRules();
        coordinator = new AgentToolCoordinator(
                agentToolCallMapper,
                new AgentToolValidator(
                        new AgentToolRegistry(), agentProperties, faithfulnessChecker, contentChecker, clock),
                executor,
                new AgentModelClient(aiProperties, agentProperties),
                new AgentGuardrailPolicy(agentProperties, guardrailRules),
                clock);

        // C4：本类用例中的素材文本即用户原话，故语料需包含它们，
        // 否则会被忠实度闸拦下而无法验证本类关注的编排语义。
        corpus = AgentSourceCorpus.ofTexts(
                List.of("撑不住", "我最近真的撑不住了"),
                agentProperties.getGuardrail().getFaithfulnessNgramSize());

        when(agentToolCallMapper.insert(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, AgentToolCall.class).setId(555L);
            return 1;
        });
    }

    /**
     * 决策 10：多个 tool_calls 只保留第一个合法提议，其余记审计。
     */
    @Test
    void shouldKeepOnlyFirstValidProposal() {
        AgentToolCall accepted = coordinator.handleProposals(session(), 1, List.of(
                new AgentRawToolCall("append_record_content", "{\"text\":\"撑不住\",\"askText\":\"放进正文？\"}"),
                new AgentRawToolCall("add_record_tags", "{\"tagIds\":[1],\"askText\":\"加标签？\"}")), corpus);

        assertThat(accepted).isNotNull();
        assertThat(accepted.getToolName()).isEqualTo("append_record_content");

        ArgumentCaptor<AgentToolCall> captor = ArgumentCaptor.forClass(AgentToolCall.class);
        verify(agentToolCallMapper, org.mockito.Mockito.times(2)).insert(captor.capture());

        List<AgentToolCall> inserted = captor.getAllValues();
        assertThat(inserted.get(0).getStatus()).isEqualTo(AgentToolCallStatus.PROPOSED);
        // 第二个被丢弃但留痕，便于观察模型是否倾向批量提议。
        assertThat(inserted.get(1).getStatus()).isEqualTo(AgentToolCallStatus.REJECTED_BY_GUARD);
        assertThat(inserted.get(1).getFailureType()).isEqualTo(AgentToolValidationResult.REASON_SUPERSEDED);
    }

    /**
     * 决策 2、9：提议阶段绝不执行。
     */
    @Test
    void shouldNeverExecuteWhileHandlingProposals() {
        coordinator.handleProposals(session(), 1, List.of(
                new AgentRawToolCall("append_record_content", "{\"text\":\"撑不住\",\"askText\":\"放进正文？\"}")),
                corpus);

        verifyNoInteractions(executor);
    }

    @Test
    void shouldRecordGuardRejectionForUnknownTool() {
        AgentToolCall accepted = coordinator.handleProposals(session(), 1, List.of(
                new AgentRawToolCall("seal_record", "{\"askText\":\"帮你封存？\"}")), corpus);

        assertThat(accepted).isNull();

        ArgumentCaptor<AgentToolCall> captor = ArgumentCaptor.forClass(AgentToolCall.class);
        verify(agentToolCallMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentToolCallStatus.REJECTED_BY_GUARD);
        assertThat(captor.getValue().getFailureType())
                .isEqualTo(AgentToolValidationResult.REASON_NOT_ALLOWLISTED);
        verifyNoInteractions(executor);
    }

    @Test
    void shouldReturnNullWhenNoToolCalls() {
        assertThat(coordinator.handleProposals(session(), 1, List.of(), corpus)).isNull();
        assertThat(coordinator.handleProposals(session(), 1, null, corpus)).isNull();
        verifyNoInteractions(agentToolCallMapper);
    }

    /**
     * 会话未绑定草稿时写工具无作用对象，提议一律被拒。
     */
    @Test
    void shouldRejectProposalWhenSessionHasNoDraft() {
        AgentSession session = session();
        session.setRecordId(null);

        AgentToolCall accepted = coordinator.handleProposals(session, 1, List.of(
                new AgentRawToolCall("append_record_content", "{\"text\":\"撑不住\",\"askText\":\"放进正文？\"}")),
                corpus);

        assertThat(accepted).isNull();
        ArgumentCaptor<AgentToolCall> captor = ArgumentCaptor.forClass(AgentToolCall.class);
        verify(agentToolCallMapper).insert(captor.capture());
        assertThat(captor.getValue().getFailureType())
                .isEqualTo(AgentToolValidationResult.REASON_NO_DRAFT_CONTEXT);
    }

    /**
     * arguments 无法解析时按无提议处理，不猜测、不补全。
     */
    @Test
    void shouldRejectProposalWithUnparsableArguments() {
        AgentToolCall accepted = coordinator.handleProposals(session(), 1, List.of(
                new AgentRawToolCall("append_record_content", "not-a-json")), corpus);

        assertThat(accepted).isNull();
        verifyNoInteractions(executor);
    }

    @Test
    void shouldPersistDigestWithoutRawText() {
        coordinator.handleProposals(session(), 1, List.of(
                new AgentRawToolCall("append_record_content",
                        "{\"text\":\"我最近真的撑不住了\",\"askText\":\"放进正文？\"}")),
                corpus);

        ArgumentCaptor<AgentToolCall> captor = ArgumentCaptor.forClass(AgentToolCall.class);
        verify(agentToolCallMapper).insert(captor.capture());

        AgentToolCall stored = captor.getValue();
        // 审计摘要不含原文；原文只在瞬态 pendingArgs 中，终结即清。
        assertThat(stored.getArgsDigest()).doesNotContain("我最近真的撑不住了");
        assertThat(stored.getArgsDigest()).contains("len=", "sha256=");
        assertThat(stored.getPendingArgs()).isNotNull();
    }

    /**
     * C4：内容不忠实的提议走既有 REJECTED_BY_GUARD 通道，
     * 且**不下发确认条**——用户根本看不到这条建议。
     */
    @Test
    void shouldRecordGuardRejectionForUnfaithfulContent() {
        AgentToolCall accepted = coordinator.handleProposals(session(), 1, List.of(
                new AgentRawToolCall("append_record_content",
                        "{\"text\":\"我最近心里有点空，不知道该不该继续走下去，方向是不是对的\","
                                + "\"askText\":\"我帮你整理了一下，放进正文？\"}")),
                corpus);

        assertThat(accepted).as("不忠实提议不得成为待确认提议").isNull();

        ArgumentCaptor<AgentToolCall> captor = ArgumentCaptor.forClass(AgentToolCall.class);
        verify(agentToolCallMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentToolCallStatus.REJECTED_BY_GUARD);
        assertThat(captor.getValue().getFailureType())
                .isEqualTo(AgentToolValidationResult.REASON_UNFAITHFUL_ARGS);
        // 提议阶段绝不执行，忠实度拒绝同样不触碰执行层。
        verifyNoInteractions(executor);
    }

    private AgentSession session() {
        AgentSession session = new AgentSession();
        session.setId(SESSION_ID);
        session.setUserId(USER_ID);
        session.setRecordId(RECORD_ID);
        return session;
    }
}
