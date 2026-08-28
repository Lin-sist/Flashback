package com.flashback.service.impl;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentMockResponder;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentModelResponse;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.AgentStageMachine;
import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentGuardrailDowngrade;
import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.agent.guardrail.AgentTimeAttributionChecker;
import com.flashback.agent.memory.MemoryCueExtractor;
import com.flashback.agent.reflection.AgentReflectionPolicy;
import com.flashback.agent.memory.MemoryPort;
import com.flashback.agent.safety.AgentSafetyPolicy;
import com.flashback.agent.tool.AgentToolCoordinator;
import com.flashback.agent.tool.AgentToolRegistry;
import com.flashback.agent.tool.AgentToolSchemaFactory;
import com.flashback.agent.trace.AgentTraceSink;
import com.flashback.agent.trace.AgentTraceVersions;
import com.flashback.service.TagService;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentMemorySource;
import com.flashback.domain.AgentMemorySourceKind;
import com.flashback.domain.AgentConversationIntent;
import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.mapper.AgentMemorySourceMapper;
import com.flashback.mapper.AgentMessageMapper;
import com.flashback.mapper.AgentSessionMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.vo.AgentSessionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentChatServiceImplTest {

    private static final Long USER_ID = 5001L;
    private static final Long SESSION_ID = 900L;

    @Mock
    private AgentSessionMapper agentSessionMapper;

    @Mock
    private AgentMessageMapper agentMessageMapper;

    @Mock
    private RecordMapper recordMapper;

    @Mock
    private AgentModelClient modelClient;

    @Mock
    private AgentToolCoordinator toolCoordinator;

    @Mock
    private TagService tagService;

    /**
     * C3 新增依赖。默认不打桩 → 返回空列表，
     * 即「检索无命中」，本类既有 C1/C2/C4 行为断言因此完全不变。
     */
    @Mock
    private MemoryPort memoryPort;

    @Mock
    private AgentMemorySourceMapper agentMemorySourceMapper;

    @Mock
    private RecordTagMapper recordTagMapper;

    /**
     * C5 新增依赖。默认不打桩 → {@code isEnabled()} 返回 false，
     * 即「可观测关闭」，本类既有 C1/C2/C3/C4 行为断言因此完全不变。
     */
    @Mock
    private AgentTraceSink traceSink;

    private AppAgentProperties properties;
    private AgentChatServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        properties.setMaxTurnsPerSession(4);
        properties.setMaxReplyChars(120);
        properties.setMaxUserInputChars(50);
        properties.setContextMessageWindow(12);
        properties.setDraftExcerptChars(300);

        AgentGuardrailRules guardrailRules = new AgentGuardrailRules();
        AgentGuardrailPolicy guardrailPolicy = new AgentGuardrailPolicy(properties, guardrailRules);
        Clock clock = Clock.fixed(Instant.parse("2026-07-27T02:00:00Z"), ZoneId.of("Asia/Shanghai"));

        // C2 新增依赖。本类保持 C1 行为断言不变：
        // 这些 mock 只为满足构造签名，工具语义由 AgentTool* 专项测试覆盖。
        AgentToolRegistry toolRegistry = new AgentToolRegistry();
        // C4 新增依赖：护栏检查层用真实实现（纯逻辑、零外调），
        // 忠实度与内容检查的专项语义由 guardrail 包下的专项测试覆盖。
        AgentFaithfulnessChecker faithfulnessChecker = new AgentFaithfulnessChecker(properties);
        service = new AgentChatServiceImpl(
                agentSessionMapper,
                agentMessageMapper,
                recordMapper,
                new AgentStageMachine(),
                new AgentPromptBuilder(properties, guardrailPolicy, guardrailRules),
                guardrailPolicy,
                modelClient,
                new AgentMockResponder(),
                new AgentToolSchemaFactory(toolRegistry, properties),
                toolCoordinator,
                faithfulnessChecker,
                new AgentContentChecker(properties, faithfulnessChecker),
                new AgentGuardrailDowngrade(),
                // C3 新增依赖：时间归属检查与记忆检索。检索 mock 默认无命中，
                // 因此本类断言的仍是「无记忆层」路径 —— 与 C4 现状等价。
                new AgentTimeAttributionChecker(properties),
                new AgentReflectionPolicy(),
                memoryPort,
                agentMemorySourceMapper,
                new MemoryCueExtractor(properties),
                recordTagMapper,
                tagService,
                // C5 新增依赖：决策轨迹。sink 用 mock（默认 isEnabled()=false → 不采集），
                // 因此本类断言的仍是与 C5 之前完全等价的路径。
                // 轨迹语义由 AgentTrace* 与 AgentObservabilityIntegrationTest 覆盖。
                traceSink,
                new AgentTraceVersions(
                        new AgentPromptBuilder(properties, guardrailPolicy, guardrailRules),
                        guardrailPolicy,
                        guardrailRules,
                        new AgentReflectionPolicy(),
                        properties),
                properties,
                clock);

        when(agentSessionMapper.insert(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, AgentSession.class).setId(SESSION_ID);
            return 1;
        });
        when(modelClient.provider()).thenReturn("mock");
        when(modelClient.isMockProvider()).thenReturn(true);
        when(modelClient.unavailableReason()).thenReturn(null);
        when(agentSessionMapper.updateMemoryAuthorization(any())).thenReturn(1);
        when(agentMemorySourceMapper.insert(any())).thenReturn(1);
    }

    // ---------- 会话开启与恢复 ----------

    @Test
    void shouldOpenSessionWithFirstGuidingQuestion() {
        AgentSessionVO vo = service.startOrResume(USER_ID, new AgentSessionStartRequest());

        assertThat(vo.getStatus()).isEqualTo("SUCCESS");
        assertThat(vo.getStage()).isEqualTo(AgentStage.WITNESS.name());
        assertThat(vo.getSessionStatus()).isEqualTo(AgentSessionStatus.ACTIVE.name());
        assertThat(vo.getMessages()).hasSize(1);
        assertThat(vo.getMessages().get(0).getRole()).isEqualTo(AgentMessageRole.ASSISTANT.name());
        assertThat(vo.getMessages().get(0).getContent()).isNotBlank();
        assertThat(vo.getMaxTurns()).isEqualTo(4);
        assertThat(vo.isCanContinue()).isTrue();
        assertThat(vo.isCrossRecordMemoryEnabled()).isFalse();
    }

    @Test
    void shouldResumeExistingActiveSessionInsteadOfCreatingNew() {
        AgentSession existing = activeSession(AgentStage.CONFUSION, 2);
        existing.setConversationIntent(AgentConversationIntent.UNTANGLE);
        // C3b：查询新增 purpose 谓词；写作引导传 WRITING_GUIDANCE。
        when(agentSessionMapper.selectActiveByUserAndRecord(
                USER_ID, null, AgentSessionPurpose.WRITING_GUIDANCE)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(
                userMessage(1, "工作上有点撑不住"),
                assistantMessage(1, "这种感觉是从什么时候开始的？")));

        AgentSessionVO vo = service.startOrResume(USER_ID, new AgentSessionStartRequest());

        assertThat(vo.getMessages()).hasSize(2);
        assertThat(vo.getStage()).isEqualTo(AgentStage.WITNESS.name());
        assertThat(vo.getConversationIntent()).isEqualTo(AgentConversationIntent.UNTANGLE.name());
        verify(agentSessionMapper, never()).insert(any());
        verify(agentSessionMapper, never()).updateConversationIntent(any());
    }

    @Test
    void shouldApplyExplicitIntentWhenResumingCompletedActiveSession() throws Exception {
        AgentSession existing = activeSession(AgentStage.WITNESS, 1);
        existing.setConversationIntent(AgentConversationIntent.LISTEN);
        when(agentSessionMapper.selectActiveByUserAndRecord(
                USER_ID, null, AgentSessionPurpose.WRITING_GUIDANCE)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(
                assistantMessage(0, "我在这里，你可以按自己的节奏说。"),
                userMessage(1, "最近总觉得事情挤在一起"),
                assistantMessage(1, "我听见了，你可以继续，也可以停在这里。")));
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setConversationIntent(AgentConversationIntent.UNTANGLE);

        AgentSessionVO vo = service.startOrResume(USER_ID, request);

        assertThat(vo.getConversationIntent()).isEqualTo(AgentConversationIntent.UNTANGLE.name());
        assertThat(vo.getTurnCount()).isEqualTo(1);
        assertThat(vo.getStage()).isEqualTo(AgentStage.WITNESS.name());
        verify(agentSessionMapper).updateConversationIntent(existing);
        verify(agentSessionMapper, never()).insert(any());
        verify(modelClient, never()).complete(anyList(), any());
        verify(modelClient, never()).completeWithTools(anyList(), anyList(), anyBoolean(), any());
    }

    @Test
    void shouldRejectStartWhenRecordNotOwned() {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setRecordId(700L);
        when(recordMapper.selectByIdAndUserId(700L, USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.startOrResume(USER_ID, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("记录不存在");
        verify(agentSessionMapper, never()).insert(any());
    }

    @Test
    void shouldExposePendingTurnAsRetryableFailureOnResume() {
        AgentSession existing = activeSession(AgentStage.CONFUSION, 1);
        when(agentSessionMapper.selectActiveByUserAndRecord(
                USER_ID, null, AgentSessionPurpose.WRITING_GUIDANCE)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(
                assistantMessage(0, "今天是什么让你想写下这一刻？"),
                userMessage(1, "工作上有点撑不住")));

        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setConversationIntent(AgentConversationIntent.UNTANGLE);

        AgentSessionVO vo = service.startOrResume(USER_ID, request);

        assertThat(vo.getStatus()).isEqualTo("FAILED");
        assertThat(vo.getMessage()).contains("重试");
        assertThat(vo.getConversationIntent()).isEqualTo(AgentConversationIntent.LISTEN.name());
        verify(agentSessionMapper, never()).insert(any());
        verify(agentSessionMapper, never()).updateConversationIntent(any());
    }

    @Test
    void shouldRetryPendingTurnWithoutDuplicatingUserMessage() throws Exception {
        AgentSession existing = activeSession(AgentStage.CONFUSION, 1);
        AgentMessage pending = userMessage(1, "工作上有点撑不住");
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(
                assistantMessage(0, "今天是什么让你想写下这一刻？"), pending));
        when(modelClient.isMockProvider()).thenReturn(false);
        when(modelClient.provider()).thenReturn("deepseek");
        // C2：Agent 对话路径改走 completeWithTools（原生 function calling）。
        // 本用例断言的是 C1 的重试语义，不涉及工具，故返回无 tool_calls 的响应。
        when(modelClient.completeWithTools(anyList(), anyList(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse("是具体某件事，还是一直压着的感觉？", List.of()));
        List<AgentMessage> stored = trackInserts();

        AgentSessionVO vo = service.sendMessage(USER_ID, SESSION_ID, messageRequest("工作上有点撑不住"));

        assertThat(vo.getStatus()).isEqualTo("SUCCESS");
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getRole()).isEqualTo(AgentMessageRole.ASSISTANT);
        assertThat(stored.get(0).getTurnNo()).isEqualTo(1);
    }

    @Test
    void shouldRejectChangingContentWhilePendingTurnNeedsRetry() {
        AgentSession existing = activeSession(AgentStage.CONFUSION, 1);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(
                assistantMessage(0, "今天是什么让你想写下这一刻？"),
                userMessage(1, "工作上有点撑不住")));

        assertThatThrownBy(() -> service.sendMessage(USER_ID, SESSION_ID, messageRequest("我想换个话题")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("重试原消息");
        verify(agentMessageMapper, never()).insert(any());
    }

    @Test
    void shouldRejectStartWhenRecordIsNotDraft() {
        AgentSessionStartRequest request = new AgentSessionStartRequest();
        request.setRecordId(700L);
        Record sealed = draftRecord();
        sealed.setStatus(RecordStatus.SEALED);
        when(recordMapper.selectByIdAndUserId(700L, USER_ID)).thenReturn(sealed);

        assertThatThrownBy(() -> service.startOrResume(USER_ID, request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只有未过期草稿或已留下记录");
        verify(agentSessionMapper, never()).insert(any());
    }

    // ---------- 归属与会话状态 ----------

    @Test
    void shouldReturnSafeNotFoundForCrossUserSessionAccess() {
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.getSession(USER_ID, SESSION_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("会话不存在");
    }

    @Test
    void shouldRejectAppendingMessageToEndedSession() {
        AgentSession ended = activeSession(AgentStage.ENDED, 3);
        ended.setStatus(AgentSessionStatus.ENDED);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(ended);

        assertThatThrownBy(() -> service.sendMessage(USER_ID, SESSION_ID, messageRequest("还能再聊吗")))
                .isInstanceOf(BizException.class);
        verify(agentMessageMapper, never()).insert(any());
    }

    @Test
    void shouldRejectIntentSwitchWhileFailedTurnIsPending() {
        AgentSession existing = activeSession(AgentStage.WITNESS, 1);
        existing.setConversationIntent(AgentConversationIntent.LISTEN);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(
                assistantMessage(0, "我在这里，你可以按自己的节奏说。"),
                userMessage(1, "工作上有点撑不住")));

        assertThatThrownBy(() -> service.switchConversationIntent(
                USER_ID, SESSION_ID, AgentConversationIntent.UNTANGLE))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("先重试");
        verify(agentSessionMapper, never()).updateConversationIntent(any());
        verifyNoInteractions(modelClient);
    }

    @Test
    void shouldNotCallMemoryPortWhenSessionAuthorizationIsOff() {
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.WITNESS, 0));
        trackInserts();

        service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近老是睡不好，压力挺大的"));

        verifyNoInteractions(memoryPort);
    }

    @Test
    void shouldNotCallMemoryPortWhenConfigIsOffEvenIfSessionIsAuthorized() {
        properties.getMemory().setEnabled(false);
        AgentSession existing = activeSession(AgentStage.WITNESS, 0);
        existing.setCrossRecordMemoryEnabled(true);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        trackInserts();

        service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近又开始担心方向了"));

        verifyNoInteractions(memoryPort);
    }

    @Test
    void shouldSwitchMemoryAuthorizationWithoutCallingProvider() throws Exception {
        AgentSession existing = activeSession(AgentStage.WITNESS, 0);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of());

        AgentSessionVO vo = service.switchMemoryAuthorization(USER_ID, SESSION_ID, true);

        assertThat(vo.isCrossRecordMemoryEnabled()).isTrue();
        verify(agentSessionMapper).updateMemoryAuthorization(existing);
        verify(agentMessageMapper, never()).insert(any());
        verify(modelClient, never()).completeWithTools(anyList(), anyList(), anyBoolean(), any());
    }

    @Test
    void shouldRejectEnablingMemoryAuthorizationWhileFailedTurnIsPending() {
        AgentSession existing = activeSession(AgentStage.WITNESS, 1);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(
                assistantMessage(0, "我在这里，你可以按自己的节奏说。"),
                userMessage(1, "工作上有点撑不住")));

        assertThatThrownBy(() -> service.switchMemoryAuthorization(USER_ID, SESSION_ID, true))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("开启参考过去");
        verify(agentSessionMapper, never()).updateMemoryAuthorization(any());
        verifyNoInteractions(modelClient);
    }

    @Test
    void shouldAllowDisablingMemoryAuthorizationWhileFailedTurnIsPending() throws Exception {
        AgentSession existing = activeSession(AgentStage.WITNESS, 1);
        existing.setCrossRecordMemoryEnabled(true);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(
                assistantMessage(0, "我在这里，你可以按自己的节奏说。"),
                userMessage(1, "工作上有点撑不住")));

        AgentSessionVO vo = service.switchMemoryAuthorization(USER_ID, SESSION_ID, false);

        assertThat(vo.isCrossRecordMemoryEnabled()).isFalse();
        verify(agentSessionMapper).updateMemoryAuthorization(existing);
        verify(modelClient, never()).completeWithTools(anyList(), anyList(), anyBoolean(), any());
    }

    @Test
    void shouldKeepMemoryAuthorizationIdempotentWithoutDatabaseWrite() throws Exception {
        AgentSession existing = activeSession(AgentStage.WITNESS, 0);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of());

        AgentSessionVO vo = service.switchMemoryAuthorization(USER_ID, SESSION_ID, false);

        assertThat(vo.isCrossRecordMemoryEnabled()).isFalse();
        verify(agentSessionMapper, never()).updateMemoryAuthorization(any());
        verify(modelClient, never()).complete(anyList(), any());
        verify(modelClient, never()).completeWithTools(anyList(), anyList(), anyBoolean(), any());
    }

    @Test
    void shouldFailClosedWhenMemoryAuthorizationUpdateLosesActiveSessionRace() {
        AgentSession existing = activeSession(AgentStage.WITNESS, 0);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of());
        when(agentSessionMapper.updateMemoryAuthorization(existing)).thenReturn(0);

        assertThatThrownBy(() -> service.switchMemoryAuthorization(USER_ID, SESSION_ID, true))
                .isInstanceOf(BizException.class)
                .hasMessage("会话状态已变化，请刷新后重试");
        verifyNoInteractions(modelClient);
    }

    @Test
    void shouldPersistOnlyTheActuallyInjectedMemorySource() {
        AgentSession existing = activeSession(AgentStage.WITNESS, 0);
        existing.setCrossRecordMemoryEnabled(true);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(memoryPort.retrieve(any())).thenReturn(List.of(new MemoryFragment(
                701L,
                LocalDateTime.of(2026, 3, 1, 10, 0),
                "2026年3月",
                "那时候也在担心方向")));
        trackInserts();

        service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近又开始担心方向了"));

        verify(agentMemorySourceMapper).insert(argThat(source ->
                source.getUserId().equals(USER_ID)
                        && source.getSessionId().equals(SESSION_ID)
                        && source.getAssistantMessageId() != null
                        && source.getSourceRecordId().equals(701L)
                        && source.getSourceKind() == AgentMemorySourceKind.CROSS_RECORD));
    }

    @Test
    void shouldStopCrossRecordRetrievalOnTheTurnAfterRevocation() {
        AgentSession existing = activeSession(AgentStage.WITNESS, 0);
        existing.setCrossRecordMemoryEnabled(true);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(memoryPort.retrieve(any())).thenReturn(List.of(new MemoryFragment(
                703L,
                LocalDateTime.of(2026, 3, 1, 10, 0),
                "2026年3月",
                "那时候也在担心方向")));
        trackInserts();

        service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近又开始担心方向了"));
        service.switchMemoryAuthorization(USER_ID, SESSION_ID, false);
        service.sendMessage(USER_ID, SESSION_ID, messageRequest("关掉以后继续说这一刻"));

        verify(memoryPort, times(1)).retrieve(any());
        assertThat(existing.isCrossRecordMemoryEnabled()).isFalse();
    }

    @Test
    void shouldResolveOwnedSourceAndHideSealedSourceMetadata() {
        AgentSession existing = activeSession(AgentStage.WITNESS, 1);
        AgentMessage assistant = assistantMessage(1, "我记得那时候的担心。 ");
        assistant.setId(301L);
        AgentMemorySource source = new AgentMemorySource();
        source.setAssistantMessageId(301L);
        source.setSourceRecordId(701L);
        source.setSourceKind(AgentMemorySourceKind.CROSS_RECORD);
        Record sealed = new Record();
        sealed.setId(701L);
        sealed.setUserId(USER_ID);
        sealed.setStatus(RecordStatus.SEALED);
        sealed.setTitle("不可披露标题");
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(assistant));
        when(agentMemorySourceMapper.selectBySessionIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(List.of(source));
        when(recordMapper.selectByIdAndUserId(701L, USER_ID)).thenReturn(sealed);

        AgentSessionVO vo = service.getSession(USER_ID, SESSION_ID);

        assertThat(vo.getMessages().get(0).getMemorySources()).singleElement().satisfies(resolved -> {
            assertThat(resolved.isAvailable()).isFalse();
            assertThat(resolved.getRecordId()).isNull();
            assertThat(resolved.getDisplayTitle()).isNull();
            assertThat(resolved.getOccurredAt()).isNull();
            assertThat(resolved.getContextNote()).isNull();
        });
    }

    @Test
    void shouldResolveOnlyCurrentOwnedMetadataForAvailableSource() {
        AgentSession existing = activeSession(AgentStage.WITNESS, 1);
        AgentMessage assistant = assistantMessage(1, "我记得那时候的担心。");
        assistant.setId(302L);
        AgentMemorySource source = new AgentMemorySource();
        source.setAssistantMessageId(302L);
        source.setSourceRecordId(702L);
        source.setSourceKind(AgentMemorySourceKind.CROSS_RECORD);
        Record available = new Record();
        available.setId(702L);
        available.setUserId(USER_ID);
        available.setStatus(RecordStatus.UNLOCKED);
        available.setTitle("现在的标题");
        available.setCreatedAt(LocalDateTime.of(2026, 2, 3, 10, 0));
        available.setAgentMemoryContextNote("只代表当时");
        available.setContent("不得进入来源 VO 的正文");
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(existing);
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(assistant));
        when(agentMemorySourceMapper.selectBySessionIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(List.of(source));
        when(recordMapper.selectByIdAndUserId(702L, USER_ID)).thenReturn(available);

        AgentSessionVO vo = service.getSession(USER_ID, SESSION_ID);

        assertThat(vo.getMessages().get(0).getMemorySources()).singleElement().satisfies(resolved -> {
            assertThat(resolved.isAvailable()).isTrue();
            assertThat(resolved.getRecordId()).isEqualTo(702L);
            assertThat(resolved.getDisplayTitle()).isEqualTo("现在的标题");
            assertThat(resolved.getOccurredAt()).isEqualTo(LocalDateTime.of(2026, 2, 3, 10, 0));
            assertThat(resolved.getContextNote()).isEqualTo("只代表当时");
        });
    }

    @Test
    void shouldRejectUserInputBeyondConfiguredLimit() {
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EMOTION, 0));

        assertThatThrownBy(() -> service.sendMessage(USER_ID, SESSION_ID, messageRequest("很".repeat(51))))
                .isInstanceOf(BizException.class);
        verify(agentMessageMapper, never()).insert(any());
    }

    // ---------- 多轮推进 ----------

    @Test
    void shouldAdvanceStageAndPersistBothMessages() {
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EMOTION, 0));
        List<AgentMessage> stored = trackInserts();

        AgentSessionVO vo = service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近老是睡不好，压力挺大的"));

        assertThat(vo.getStatus()).isEqualTo("SUCCESS");
        assertThat(vo.getStage()).isEqualTo(AgentStage.WITNESS.name());
        assertThat(vo.getTurnCount()).isEqualTo(1);
        assertThat(stored).hasSize(2);
        assertThat(stored.get(0).getRole()).isEqualTo(AgentMessageRole.USER);
        assertThat(stored.get(1).getRole()).isEqualTo(AgentMessageRole.ASSISTANT);
    }

    @Test
    void shouldReturnLocalSafetyResponseBeforeProviderMemoryToolsOrMaterial() throws Exception {
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EMOTION, 0));
        List<AgentMessage> stored = trackInserts();

        AgentSessionVO vo = service.sendMessage(USER_ID, SESSION_ID, messageRequest("我现在就要去死"));

        assertThat(vo.getStatus()).isEqualTo("SUCCESS");
        assertThat(vo.getSessionStatus()).isEqualTo(AgentSessionStatus.ACTIVE.name());
        assertThat(vo.getMaterialDraft()).isNull();
        assertThat(stored).hasSize(2);
        assertThat(stored.get(1).getContent()).isEqualTo(AgentSafetyPolicy.LOCAL_RESPONSE);
        verify(modelClient, never()).completeWithTools(anyList(), anyList(), anyBoolean(), any());
        verify(modelClient, never()).complete(anyList(), any());
        verifyNoInteractions(memoryPort);
        verifyNoInteractions(toolCoordinator);
        verify(agentMemorySourceMapper, never()).insert(any());
    }

    @Test
    void shouldEndSessionAndReturnMaterialWhenClosing() {
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EXPECTATION, 3));
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(new ArrayList<>(List.of(
                userMessage(1, "工作上有点撑不住"),
                userMessage(2, "主要是不知道先做哪件事"))));

        AgentSessionVO vo = service.sendMessage(USER_ID, SESSION_ID, messageRequest("希望三个月后能踏实一点"));

        assertThat(vo.getSessionStatus()).isEqualTo(AgentSessionStatus.ENDED.name());
        assertThat(vo.isCanContinue()).isFalse();
        assertThat(vo.getMaterialDraft()).contains("工作上有点撑不住");
        // 素材只由用户说过的话组成，不含 Agent 回复
        assertThat(vo.getMaterialDraft()).doesNotContain("你想先说");
    }

    @Test
    void shouldForceClosingWhenTurnLimitReached() {
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EMOTION, 3));

        AgentSessionVO vo = service.sendMessage(USER_ID, SESSION_ID, messageRequest("还有很多话没说完呢"));

        assertThat(vo.getTurnCount()).isEqualTo(4);
        assertThat(vo.getSessionStatus()).isEqualTo(AgentSessionStatus.ENDED.name());
    }

    // ---------- 失败语义 ----------

    @Test
    void shouldReturnUnavailableWithoutFakeReplyWhenProviderNotConfigured() {
        when(modelClient.unavailableReason()).thenReturn("AI服务未配置");
        when(modelClient.isMockProvider()).thenReturn(false);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EMOTION, 0));
        List<AgentMessage> stored = trackInserts();

        AgentSessionVO vo = service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近老是睡不好"));

        assertThat(vo.getStatus()).isEqualTo("UNAVAILABLE");
        assertThat(vo.getMessage()).isEqualTo("刚才写下的这句还在，但现在暂时无法继续。");
        assertThat(vo.getMaterialDraft()).isNull();
        // 用户的话保留，Agent 回复不落库
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getRole()).isEqualTo(AgentMessageRole.USER);
    }

    @Test
    void shouldReturnFailedAndKeepUserMessageWhenProviderCallFails() throws Exception {
        when(modelClient.isMockProvider()).thenReturn(false);
        when(modelClient.provider()).thenReturn("deepseek");
        when(modelClient.completeWithTools(anyList(), anyList(), anyBoolean(), any()))
                .thenThrow(new java.io.IOException("boom"));
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EMOTION, 0));
        List<AgentMessage> stored = trackInserts();

        AgentSessionVO vo = service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近老是睡不好"));

        assertThat(vo.getStatus()).isEqualTo("FAILED");
        assertThat(vo.getMessage()).isEqualTo("刚才写下的这句还在，但现在暂时无法继续。");
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getRole()).isEqualTo(AgentMessageRole.USER);
        verify(agentMemorySourceMapper, never()).insert(any());
    }

    @Test
    void shouldReturnFailedWhenProviderContentIsInvalid() throws Exception {
        when(modelClient.isMockProvider()).thenReturn(false);
        when(modelClient.provider()).thenReturn("deepseek");
        // 既无 content 也无 tool_calls：视为无效响应，返回显式 FAILED。
        when(modelClient.completeWithTools(anyList(), anyList(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse(null, List.of()));
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EMOTION, 0));

        AgentSessionVO vo = service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近老是睡不好"));

        assertThat(vo.getStatus()).isEqualTo("FAILED");
        assertThat(vo.getMessage()).isEqualTo("刚才写下的这句还在，但现在暂时无法继续。");
    }

    @Test
    void shouldTruncateProviderReplyBeyondLengthLimit() throws Exception {
        properties.setMaxReplyChars(12);
        when(modelClient.isMockProvider()).thenReturn(false);
        when(modelClient.provider()).thenReturn("deepseek");
        when(modelClient.completeWithTools(anyList(), anyList(), anyBoolean(), any()))
                .thenReturn(new AgentModelResponse(
                        "听起来不太容易。你要不要先说说其中最让你在意的那一部分呢", List.of()));
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EMOTION, 0));
        List<AgentMessage> stored = trackInserts();

        service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近老是睡不好"));

        AgentMessage assistant = stored.get(1);
        assertThat(assistant.getContent()).isEqualTo("听起来不太容易。");
    }

    // ---------- 主动结束 ----------

    @Test
    void shouldFinishSessionAndReturnMaterialFromUserContentOnly() {
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.CONFUSION, 2));
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(
                assistantMessage(1, "今天是什么让你想写下这一刻？"),
                userMessage(1, "工作上有点撑不住")));

        AgentSessionVO vo = service.finish(USER_ID, SESSION_ID);

        assertThat(vo.getSessionStatus()).isEqualTo(AgentSessionStatus.ENDED.name());
        assertThat(vo.getMaterialDraft()).isEqualTo("工作上有点撑不住");
    }

    @Test
    void shouldNotTouchRecordWriteOperationsDuringConversation() {
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID))
                .thenReturn(activeSession(AgentStage.EMOTION, 0));

        service.sendMessage(USER_ID, SESSION_ID, messageRequest("最近老是睡不好，压力挺大的"));

        // C1 范围约束：Agent 不得触发任何记录写操作
        verify(recordMapper, never()).updateEditableByIdAndUserId(
                anyLong(), anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(recordMapper, never()).sealSavedByIdAndUserId(anyLong(), anyLong(), any(), any());
        verify(recordMapper, never()).deleteDraftByIdAndUserId(anyLong(), anyLong());
        verify(recordMapper, never()).updateLaterReflectionByIdAndUserId(anyLong(), anyLong(), any(), any());
    }

    // ---------- helpers ----------

    private List<AgentMessage> trackInserts() {
        List<AgentMessage> stored = new ArrayList<>();
        when(agentMessageMapper.insert(any())).thenAnswer(invocation -> {
            AgentMessage message = invocation.getArgument(0, AgentMessage.class);
            message.setId(200L + stored.size());
            stored.add(message);
            return 1;
        });
        return stored;
    }

    private AgentMessageRequest messageRequest(String content) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(content);
        return request;
    }

    private AgentSession activeSession(AgentStage stage, int turnCount) {
        AgentSession session = new AgentSession();
        session.setId(SESSION_ID);
        session.setUserId(USER_ID);
        session.setStage(stage);
        session.setStatus(AgentSessionStatus.ACTIVE);
        session.setTurnCount(turnCount);
        session.setStageReaskCount(0);
        session.setCreatedAt(LocalDateTime.of(2026, 7, 27, 10, 0));
        session.setUpdatedAt(LocalDateTime.of(2026, 7, 27, 10, 0));
        session.setLastActiveAt(LocalDateTime.of(2026, 7, 27, 10, 0));
        return session;
    }

    private AgentMessage userMessage(int turnNo, String content) {
        return message(turnNo, AgentMessageRole.USER, content);
    }

    private AgentMessage assistantMessage(int turnNo, String content) {
        return message(turnNo, AgentMessageRole.ASSISTANT, content);
    }

    private AgentMessage message(int turnNo, AgentMessageRole role, String content) {
        AgentMessage message = new AgentMessage();
        message.setSessionId(SESSION_ID);
        message.setUserId(USER_ID);
        message.setTurnNo(turnNo);
        message.setRole(role);
        message.setStage(AgentStage.EMOTION);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.of(2026, 7, 27, 10, 0));
        return message;
    }

    private Record draftRecord() {
        Record record = new Record();
        record.setId(700L);
        record.setUserId(USER_ID);
        record.setStatus(RecordStatus.DRAFT);
        record.setContent("已经写下的一点正文");
        return record;
    }
}
