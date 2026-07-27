package com.flashback.agent.tool;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentRawToolCall;
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
        coordinator = new AgentToolCoordinator(
                agentToolCallMapper,
                new AgentToolValidator(new AgentToolRegistry(), agentProperties, clock),
                executor,
                new AgentModelClient(aiProperties, agentProperties),
                new AgentGuardrailPolicy(agentProperties),
                clock);

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
                new AgentRawToolCall("add_record_tags", "{\"tagIds\":[1],\"askText\":\"加标签？\"}")));

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
                new AgentRawToolCall("append_record_content", "{\"text\":\"撑不住\",\"askText\":\"放进正文？\"}")));

        verifyNoInteractions(executor);
    }

    @Test
    void shouldRecordGuardRejectionForUnknownTool() {
        AgentToolCall accepted = coordinator.handleProposals(session(), 1, List.of(
                new AgentRawToolCall("seal_record", "{\"askText\":\"帮你封存？\"}")));

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
        assertThat(coordinator.handleProposals(session(), 1, List.of())).isNull();
        assertThat(coordinator.handleProposals(session(), 1, null)).isNull();
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
                new AgentRawToolCall("append_record_content", "{\"text\":\"撑不住\",\"askText\":\"放进正文？\"}")));

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
                new AgentRawToolCall("append_record_content", "not-a-json")));

        assertThat(accepted).isNull();
        verifyNoInteractions(executor);
    }

    @Test
    void shouldPersistDigestWithoutRawText() {
        coordinator.handleProposals(session(), 1, List.of(
                new AgentRawToolCall("append_record_content",
                        "{\"text\":\"我最近真的撑不住了\",\"askText\":\"放进正文？\"}")));

        ArgumentCaptor<AgentToolCall> captor = ArgumentCaptor.forClass(AgentToolCall.class);
        verify(agentToolCallMapper).insert(captor.capture());

        AgentToolCall stored = captor.getValue();
        // 审计摘要不含原文；原文只在瞬态 pendingArgs 中，终结即清。
        assertThat(stored.getArgsDigest()).doesNotContain("我最近真的撑不住了");
        assertThat(stored.getArgsDigest()).contains("len=", "sha256=");
        assertThat(stored.getPendingArgs()).isNotNull();
    }

    private AgentSession session() {
        AgentSession session = new AgentSession();
        session.setId(SESSION_ID);
        session.setUserId(USER_ID);
        session.setRecordId(RECORD_ID);
        return session;
    }
}
