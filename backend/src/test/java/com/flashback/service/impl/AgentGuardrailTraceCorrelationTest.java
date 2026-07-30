package com.flashback.service.impl;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentMockResponder;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.AgentStageMachine;
import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentGuardrailDowngrade;
import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentGuardrailViolation;
import com.flashback.agent.guardrail.AgentTimeAttributionChecker;
import com.flashback.agent.memory.MemoryCueExtractor;
import com.flashback.agent.memory.MemoryPort;
import com.flashback.agent.tool.AgentToolCoordinator;
import com.flashback.agent.tool.AgentToolRegistry;
import com.flashback.agent.tool.AgentToolSchemaFactory;
import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.agent.trace.AgentTraceSink;
import com.flashback.agent.trace.AgentTraceVersions;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.mapper.AgentMessageMapper;
import com.flashback.mapper.AgentSessionMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.service.TagService;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 护栏降级痕迹的会话关联（C5，V4 / V5 补齐）。
 *
 * <h3>本类为什么存在</h3>
 * C5 之前 {@code AgentGuardrailDowngrade.trace(path, sessionId, turnNo, verdict)}
 * 的
 * 形参里就有 sessionId 与 turnNo，但两个调用点
 * （{@code applyReplyGuardrail} / {@code applyMaterialGuardrail}）**全部传 null**——
 * 降级痕迹关联不到任何一轮对话。这正是 C5 要解决的问题本身。
 *
 * 用 Mockito 而不是集成测试的理由：mock provider 的回复是合规的，
 * 在集成层制造「一句违规回复」不现实。这里把内容检查器换成一个必定判违规的替身，
 * 就能直接验证「降级发生时痕迹带的是真实 sessionId / turnNo」。
 *
 * 替身只影响判定结果，不改判定语义——护栏本身的正确性归 C4 的用例集。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentGuardrailTraceCorrelationTest {

    private static final Long USER_ID = 6101L;
    private static final Long SESSION_ID = 971L;
    private static final Long RECORD_ID = 6201L;

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

    @Mock
    private MemoryPort memoryPort;

    @Mock
    private RecordTagMapper recordTagMapper;

    @Mock
    private AgentTraceSink traceSink;

    @Mock
    private AgentGuardrailDowngrade guardrailDowngrade;

    @Mock
    private AgentContentChecker contentChecker;

    @Mock
    private AgentTimeAttributionChecker timeAttributionChecker;

    private AgentChatServiceImpl service;
    private final List<AgentMessage> persisted = new ArrayList<>();

    @BeforeEach
    void setUp() {
        AppAgentProperties properties = new AppAgentProperties();
        properties.setMaxTurnsPerSession(8);
        properties.setMaxReplyChars(120);
        properties.setMaxUserInputChars(500);
        properties.setContextMessageWindow(12);
        properties.setDraftExcerptChars(300);

        AgentGuardrailRules guardrailRules = new AgentGuardrailRules();
        AgentGuardrailPolicy guardrailPolicy = new AgentGuardrailPolicy(properties, guardrailRules);
        AgentFaithfulnessChecker faithfulnessChecker = new AgentFaithfulnessChecker(properties);
        Clock clock = Clock.fixed(Instant.parse("2026-07-30T02:00:00Z"), ZoneId.of("Asia/Shanghai"));

        service = new AgentChatServiceImpl(
                agentSessionMapper,
                agentMessageMapper,
                recordMapper,
                new AgentStageMachine(),
                new AgentPromptBuilder(properties, guardrailPolicy, guardrailRules),
                guardrailPolicy,
                modelClient,
                new AgentMockResponder(),
                new AgentToolSchemaFactory(new AgentToolRegistry(), properties),
                toolCoordinator,
                faithfulnessChecker,
                contentChecker,
                guardrailDowngrade,
                timeAttributionChecker,
                memoryPort,
                new MemoryCueExtractor(properties),
                recordTagMapper,
                tagService,
                traceSink,
                new AgentTraceVersions(
                        new AgentPromptBuilder(properties, guardrailPolicy, guardrailRules),
                        guardrailPolicy,
                        guardrailRules),
                properties,
                clock);

        when(modelClient.provider()).thenReturn("mock");
        when(modelClient.model()).thenReturn("mock");
        when(modelClient.isMockProvider()).thenReturn(true);
        when(modelClient.unavailableReason()).thenReturn(null);
        when(guardrailDowngrade.safeFallbackReply()).thenReturn("这些听起来挺不容易的。你想再多说一点吗？");

        // 可观测开启，且尝试序号照常推导。
        when(traceSink.isEnabled()).thenReturn(true);
        when(traceSink.nextAttemptNo(anyLong(), anyInt(), eq(false))).thenReturn(1);

        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(activeSession());
        when(agentMessageMapper.selectBySessionId(SESSION_ID)).thenReturn(List.of(openingMessage()));
        when(agentMessageMapper.insert(any())).thenAnswer(invocation -> {
            AgentMessage message = invocation.getArgument(0, AgentMessage.class);
            message.setId((long) (persisted.size() + 100));
            persisted.add(message);
            return 1;
        });
        when(recordMapper.selectByIdAndUserId(RECORD_ID, USER_ID)).thenReturn(draftRecord());

        // 时间归属默认通过；由各测试决定内容检查是否判违规。
        when(timeAttributionChecker.check(any(), any())).thenReturn(AgentGuardrailVerdict.pass());
    }

    /**
     * 回复被诊断/代决检查拦下时，降级痕迹必须带真实 sessionId 与 turnNo。
     *
     * 这是 V4 的核心断言：C5 之前这两个参数恒为 null。
     */
    @Test
    void replyDowngradeTraceMustCarryRealSessionAndTurn() {
        when(contentChecker.check(any(), any()))
                .thenReturn(AgentGuardrailVerdict.violation(AgentGuardrailViolation.DIAGNOSTIC, 0.1d, 12, 40));

        service.sendMessage(USER_ID, SESSION_ID, message("最近总觉得胸口发闷"));

        ArgumentCaptor<Long> sessionIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> turnNoCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(guardrailDowngrade).trace(
                eq("reply:turn"), sessionIdCaptor.capture(), turnNoCaptor.capture(), any());

        assertThat(sessionIdCaptor.getValue())
                .as("V4 补齐：C5 之前这里恒为 null，降级痕迹关联不到任何一轮对话")
                .isEqualTo(SESSION_ID);
        assertThat(turnNoCaptor.getValue()).isEqualTo(1);
    }

    /**
     * 时间归属检查拦下时同理。
     */
    @Test
    void attributionDowngradeTraceMustCarryRealSessionAndTurn() {
        when(contentChecker.check(any(), any())).thenReturn(AgentGuardrailVerdict.pass());
        when(timeAttributionChecker.check(any(), any())).thenReturn(
                AgentGuardrailVerdict.violation(AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION, 0.3d, 15, 50));

        service.sendMessage(USER_ID, SESSION_ID, message("其实我一直放不下那件事"));

        verify(guardrailDowngrade).trace(
                eq("reply-attribution:turn"), eq(SESSION_ID), eq(1), any());
    }

    /**
     * 降级时轨迹的 outcome 必须是 DOWNGRADED，且能看出兜底来自本地。
     *
     * 对用户来说这是一次「成功返回」（他确实收到了回复），但排查时必须一眼看出
     * 这句话不是 provider 的正常产出——沿用 C4 已接受的条款。
     */
    @Test
    void downgradedTurnMustBeDistinguishableFromNormalSuccess() {
        when(contentChecker.check(any(), any()))
                .thenReturn(AgentGuardrailVerdict.violation(AgentGuardrailViolation.FAKE_ACTION, 0.2d, 20, 60));

        service.sendMessage(USER_ID, SESSION_ID, message("你帮我把它封存了吗"));

        ArgumentCaptor<AgentTraceCollector> captor = ArgumentCaptor.forClass(AgentTraceCollector.class);
        verify(traceSink).persist(captor.capture());
        AgentTraceCollector trace = captor.getValue();

        assertThat(trace).isNotNull();
        assertThat(trace.outcome().name()).isEqualTo("DOWNGRADED");
        assertThat(trace.downgradePath()).isEqualTo("reply-content");
        assertThat(trace.violation()).isEqualTo("fake-action");
        assertThat(trace.sessionId()).isEqualTo(SESSION_ID);
        assertThat(trace.turnNo()).isEqualTo(1);
    }

    /**
     * V5：护栏判定自身异常（fail-closed，CHECK_ERROR）时轨迹可关联到会话。
     *
     * 实现期发现不需要改 checker 签名：CHECK_ERROR 本来就以 verdict 返回给调用方，
     * 而调用方现在会把 verdict 记进轨迹，关联因此天然成立。
     */
    @Test
    void checkErrorMustBeCorrelatableToSession() {
        when(contentChecker.check(any(), any()))
                .thenReturn(AgentGuardrailVerdict.violation(AgentGuardrailViolation.CHECK_ERROR));

        service.sendMessage(USER_ID, SESSION_ID, message("说不上来的感觉"));

        ArgumentCaptor<AgentTraceCollector> captor = ArgumentCaptor.forClass(AgentTraceCollector.class);
        verify(traceSink).persist(captor.capture());
        AgentTraceCollector trace = captor.getValue();

        assertThat(trace.violation()).isEqualTo("check-error");
        assertThat(trace.sessionId()).isEqualTo(SESSION_ID);
        assertThat(trace.turnNo()).isEqualTo(1);
    }

    /**
     * 可观测关闭时不创建收集器，落库出口收到 null。
     *
     * 关键是**对话行为不变**：降级照常发生，只是不留轨迹。
     */
    @Test
    void disabledObservabilityMustStillDowngradeButLeaveNoTrace() {
        when(traceSink.isEnabled()).thenReturn(false);
        when(contentChecker.check(any(), any()))
                .thenReturn(AgentGuardrailVerdict.violation(AgentGuardrailViolation.DIAGNOSTIC, 0.1d, 12, 40));

        service.sendMessage(USER_ID, SESSION_ID, message("最近总觉得胸口发闷"));

        verify(traceSink).traceDisabled(SESSION_ID);
        verify(traceSink).persist(null);
        // 降级仍然发生，只是痕迹的 sessionId 退回 null（没有轨迹可取值）。
        verify(guardrailDowngrade).trace(eq("reply:turn"), eq(null), eq(null), any());
    }

    // ---------- fixture ----------

    private AgentSession activeSession() {
        AgentSession session = new AgentSession();
        session.setId(SESSION_ID);
        session.setUserId(USER_ID);
        session.setRecordId(RECORD_ID);
        session.setPurpose(AgentSessionPurpose.WRITING_GUIDANCE);
        session.setStage(AgentStage.EMOTION);
        session.setStatus(AgentSessionStatus.ACTIVE);
        session.setTurnCount(0);
        session.setStageReaskCount(0);
        return session;
    }

    private AgentMessage openingMessage() {
        AgentMessage message = new AgentMessage();
        message.setId(1L);
        message.setSessionId(SESSION_ID);
        message.setUserId(USER_ID);
        message.setRole(AgentMessageRole.ASSISTANT);
        message.setTurnNo(0);
        message.setStage(AgentStage.OPENING);
        message.setContent("今天是什么让你想写下这一刻？");
        message.setCreatedAt(LocalDateTime.of(2026, 7, 30, 10, 0));
        return message;
    }

    private Record draftRecord() {
        Record record = new Record();
        record.setId(RECORD_ID);
        record.setUserId(USER_ID);
        record.setContent("先记一点");
        record.setStatus(RecordStatus.DRAFT);
        return record;
    }

    private AgentMessageRequest message(String content) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(content);
        return request;
    }
}
