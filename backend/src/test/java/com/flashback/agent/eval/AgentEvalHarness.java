package com.flashback.agent.eval;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentMockResponder;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.AgentStageMachine;
import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentGuardrailDowngrade;
import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.agent.guardrail.AgentTimeAttributionChecker;
import com.flashback.agent.memory.MemoryCueExtractor;
import com.flashback.agent.reflection.AgentReflectionPolicy;
import com.flashback.agent.memory.MySqlMemoryPort;
import com.flashback.agent.tool.AgentToolCoordinator;
import com.flashback.agent.tool.AgentToolExecutor;
import com.flashback.agent.tool.AgentToolRegistry;
import com.flashback.agent.tool.AgentToolSchemaFactory;
import com.flashback.agent.tool.AgentToolValidator;
import com.flashback.agent.trace.AgentTraceVersions;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
import com.flashback.domain.AgentToolCall;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.mapper.AgentMessageMapper;
import com.flashback.mapper.AgentSessionMapper;
import com.flashback.mapper.AgentToolCallMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.service.RecordService;
import com.flashback.service.TagService;
import com.flashback.service.impl.AgentChatServiceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 评测执行基座（C6，T-03）。
 *
 * <h3>它做什么</h3>
 * 装配一个可驱动的 {@link AgentChatServiceImpl}，跑若干轮 {@code sendMessage}，
 * 把每轮的 {@link com.flashback.agent.trace.AgentTraceCollector} 收在内存里供断言。
 *
 * <h3>为什么这样装配（design 决策 1）</h3>
 * 范式不是新发明的：{@code AgentGuardrailTraceCorrelationTest} 早就用纯 Mockito
 * 驱动完整 {@code sendMessage}。本类把那套装配抽成可复用的东西，并做两处加强：
 *
 * <ol>
 * <li><b>护栏、状态机、prompt 组装、记忆检索、工具校验全部用真实实现。</b>
 * 替身只在两个地方：数据库边界（mapper）与 provider 边界（{@link ScriptedAgentModelClient}）。
 * 这一点是本类的核心取舍——替身替掉的东西越多，测到的生产代码越少。
 * 譬如「注入预算」这条不变量：若用假的 MemoryPort 返回我自己写的 3 条片段，
 * 断言的就是我的 stub 而不是 {@code MySqlMemoryPort} 真的会截断到 limit。</li>
 * <li><b>消息与会话状态存在内存里并真的变化。</b> 多轮评测要求
 * {@code selectBySessionId} 返回逐轮增长的历史，否则阶段推进与追问上限根本走不到。</li>
 * </ol>
 *
 * <h3>零生产改动</h3>
 * 本类不需要生产代码提供任何测试钩子：{@code AgentChatServiceImpl} 的 22 个协作者
 * 全部构造注入，收集器的读取器全部 public，落库出口只有一个。
 * 这是 C5「收集器 + 单一落库出口」纪律的副产品。
 */
final class AgentEvalHarness {

    static final Long USER_ID = 90001L;
    static final Long SESSION_ID = 90101L;
    static final Long RECORD_ID = 90201L;

    /** 固定时钟：评测必须可复现，任何对 now() 的依赖都不能是真实时间。 */
    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-31T02:00:00Z"), ZoneId.of("Asia/Shanghai"));

    private final AppAgentProperties properties;
    private final ScriptedAgentModelClient modelClient;
    private final RecordingTraceSink traceSink;
    private final AgentChatServiceImpl service;
    private final AgentSession session;
    private final List<AgentMessage> messages = new ArrayList<>();
    private final List<AgentToolCall> toolCalls = new ArrayList<>();

    private AgentEvalHarness(Builder builder) {
        this.properties = builder.properties;
        this.traceSink = builder.observabilityEnabled
                ? RecordingTraceSink.enabled()
                : RecordingTraceSink.disabled();
        this.modelClient = ScriptedAgentModelClient.available(properties);

        AgentGuardrailRules guardrailRules = new AgentGuardrailRules();
        AgentGuardrailPolicy guardrailPolicy = new AgentGuardrailPolicy(properties, guardrailRules);
        AgentPromptBuilder promptBuilder = new AgentPromptBuilder(properties, guardrailPolicy, guardrailRules);
        AgentFaithfulnessChecker faithfulnessChecker = new AgentFaithfulnessChecker(properties);
        AgentContentChecker contentChecker = new AgentContentChecker(properties, faithfulnessChecker);
        AgentTimeAttributionChecker timeAttributionChecker = new AgentTimeAttributionChecker(properties);

        this.session = builder.session();
        RecordMapper recordMapper = recordMapperFake(builder);
        AgentSessionMapper agentSessionMapper = mock(AgentSessionMapper.class);
        when(agentSessionMapper.selectByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(session);

        AgentMessageMapper agentMessageMapper = agentMessageMapperFake();
        RecordTagMapper recordTagMapper = mock(RecordTagMapper.class);
        when(recordTagMapper.selectTagIdsByRecordId(anyLong())).thenReturn(builder.tagIds);

        TagService tagService = mock(TagService.class);
        when(tagService.listEnabled(any())).thenReturn(List.of());

        // 工具链用真实校验器与真实 coordinator：工具 fail-closed 与提议被拒
        // 是本刀要评的维度，用假 coordinator 就等于不评。
        AgentToolRegistry toolRegistry = new AgentToolRegistry();
        AgentToolValidator toolValidator = new AgentToolValidator(
                toolRegistry, properties, faithfulnessChecker, contentChecker,
                timeAttributionChecker, FIXED_CLOCK);
        AgentToolCoordinator toolCoordinator = new AgentToolCoordinator(
                toolCallMapperFake(),
                toolValidator,
                // executor 只在 confirmToolCall 时被用到，评测不确认提议，故 RecordService 恒不被调用。
                new AgentToolExecutor(mock(RecordService.class), toolValidator),
                modelClient,
                guardrailPolicy,
                FIXED_CLOCK);

        this.service = new AgentChatServiceImpl(
                agentSessionMapper,
                agentMessageMapper,
                recordMapper,
                new AgentStageMachine(),
                promptBuilder,
                guardrailPolicy,
                modelClient,
                // 生产组件，一行未改。走 scripted 客户端时它不参与回复生成
                // （isMockProvider() 为 false），只为满足构造签名。
                new AgentMockResponder(),
                new AgentToolSchemaFactory(toolRegistry, properties),
                toolCoordinator,
                faithfulnessChecker,
                contentChecker,
                new AgentGuardrailDowngrade(),
                timeAttributionChecker,
                new AgentReflectionPolicy(),
                // 真实 MemoryPort：注入预算的截断行为由它负责，评测断言的是它而不是替身。
                new MySqlMemoryPort(recordMapper, properties, FIXED_CLOCK),
                new MemoryCueExtractor(properties),
                recordTagMapper,
                tagService,
                traceSink,
                new AgentTraceVersions(
                        promptBuilder, guardrailPolicy, guardrailRules, new AgentReflectionPolicy()),
                properties,
                FIXED_CLOCK);

        if (builder.withOpening) {
            AgentMessage opening = new AgentMessage();
            opening.setId(1L);
            opening.setSessionId(SESSION_ID);
            opening.setUserId(USER_ID);
            opening.setRole(AgentMessageRole.ASSISTANT);
            opening.setTurnNo(0);
            opening.setStage(builder.purpose == AgentSessionPurpose.REVIEW_CHAT
                    ? AgentStage.REVIEW
                    : AgentStage.OPENING);
            opening.setContent("今天是什么让你想写下这一刻？");
            opening.setCreatedAt(LocalDateTime.of(2026, 7, 31, 10, 0));
            messages.add(opening);
        }
    }

    // ---------- 驱动 ----------

    /**
     * 跑一轮。异常不吞——评测要能断言 provider 失败时的**既有语义**，
     * 而失败一轮返回的是 status=failed 的 VO 而非异常，因此正常情况不会抛。
     */
    AgentEvalHarness turn(String userInput) {
        AgentMessageRequest request = new AgentMessageRequest();
        request.setContent(userInput);
        service.sendMessage(USER_ID, SESSION_ID, request);
        return this;
    }

    ScriptedAgentModelClient client() {
        return modelClient;
    }

    RecordingTraceSink sink() {
        return traceSink;
    }

    AppAgentProperties properties() {
        return properties;
    }

    AgentSession session() {
        return session;
    }

    /**
     * 已落库的消息（含开场）。用于长度比等需要对照用户输入的维度。
     */
    List<AgentMessage> messages() {
        return List.copyOf(messages);
    }

    // ---------- 数据库边界替身 ----------

    /**
     * 内存版消息存储。
     *
     * 必须真的增长：{@code sendMessage} 每轮开头都会 selectBySessionId 取历史，
     * 阶段推进、追问判定、prompt 组装规模、忠实度来源集合全依赖它。
     * 返回固定列表会让多轮评测退化成「反复跑第一轮」。
     */
    private AgentMessageMapper agentMessageMapperFake() {
        AgentMessageMapper mapper = mock(AgentMessageMapper.class);
        when(mapper.selectBySessionId(SESSION_ID)).thenAnswer(invocation -> List.copyOf(messages));
        when(mapper.insert(any())).thenAnswer(invocation -> {
            AgentMessage message = invocation.getArgument(0, AgentMessage.class);
            message.setId(100L + messages.size());
            messages.add(message);
            return 1;
        });
        return mapper;
    }

    private AgentToolCallMapper toolCallMapperFake() {
        AgentToolCallMapper mapper = mock(AgentToolCallMapper.class);
        when(mapper.insert(any())).thenAnswer(invocation -> {
            AgentToolCall toolCall = invocation.getArgument(0, AgentToolCall.class);
            toolCall.setId(200L + toolCalls.size());
            toolCalls.add(toolCall);
            return 1;
        });
        when(mapper.selectRecentSettledBySessionId(anyLong(), anyInt())).thenReturn(List.of());
        return mapper;
    }

    private RecordMapper recordMapperFake(Builder builder) {
        RecordMapper mapper = mock(RecordMapper.class);
        when(mapper.selectByIdAndUserId(eq(RECORD_ID), eq(USER_ID))).thenReturn(builder.record());
        // 记忆候选集：真实 MySqlMemoryPort 会对它做「取材字段优先级 + 截断 + limit」处理，
        // 所以这里给的是**候选记录**，不是成品片段。
        when(mapper.selectMemoryCandidates(
                anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(builder.memoryCandidates);
        return mapper;
    }

    // ---------- Builder ----------

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {

        private final AppAgentProperties properties = new AppAgentProperties();
        private AgentSessionPurpose purpose = AgentSessionPurpose.WRITING_GUIDANCE;
        private AgentStage stage = AgentStage.EMOTION;
        private int turnCount;
        private int stageReaskCount;
        private boolean withOpening = true;
        private boolean observabilityEnabled = true;
        private Long recordId = RECORD_ID;
        private String recordContent = "先记一点";
        private List<Long> tagIds = List.of();
        private List<Record> memoryCandidates = List.of();

        private Builder() {
            // 默认值显式写出，使评测不依赖 application.yml——
            // 配置漂移不该让评测结论跟着漂。数值与 AppAgentProperties 的默认一致。
            properties.setMaxTurnsPerSession(8);
            properties.setMaxReplyChars(120);
            properties.setMaxUserInputChars(1000);
            properties.setContextMessageWindow(12);
            properties.setDraftExcerptChars(300);
        }

        Builder purpose(AgentSessionPurpose value) {
            this.purpose = value;
            this.stage = value == AgentSessionPurpose.REVIEW_CHAT ? AgentStage.REVIEW : AgentStage.EMOTION;
            return this;
        }

        Builder stage(AgentStage value) {
            this.stage = value;
            return this;
        }

        Builder turnCount(int value) {
            this.turnCount = value;
            return this;
        }

        Builder stageReaskCount(int value) {
            this.stageReaskCount = value;
            return this;
        }

        Builder maxTurns(int value) {
            properties.setMaxTurnsPerSession(value);
            return this;
        }

        Builder reviewMaxTurns(int value) {
            properties.getReview().setMaxTurnsPerSession(value);
            return this;
        }

        Builder maxReplyChars(int value) {
            properties.setMaxReplyChars(value);
            return this;
        }

        Builder memoryEnabled(boolean value) {
            properties.getMemory().setEnabled(value);
            return this;
        }

        Builder observabilityEnabled(boolean value) {
            this.observabilityEnabled = value;
            return this;
        }

        Builder withoutRecord() {
            this.recordId = null;
            return this;
        }

        Builder recordContent(String value) {
            this.recordContent = value;
            return this;
        }

        Builder tagIds(List<Long> value) {
            this.tagIds = List.copyOf(value);
            return this;
        }

        /**
         * 记忆候选记录。片段文本取自 aiSummary（真实 port 的第一优先字段）。
         */
        Builder memoryCandidate(long id, String aiSummary, LocalDateTime createdAt) {
            List<Record> next = new ArrayList<>(memoryCandidates);
            Record record = new Record();
            record.setId(id);
            record.setUserId(USER_ID);
            record.setAiSummary(aiSummary);
            record.setStatus(RecordStatus.UNLOCKED);
            record.setCreatedAt(createdAt);
            next.add(record);
            this.memoryCandidates = List.copyOf(next);
            return this;
        }

        AppAgentProperties properties() {
            return properties;
        }

        private AgentSession session() {
            AgentSession session = new AgentSession();
            session.setId(SESSION_ID);
            session.setUserId(USER_ID);
            session.setRecordId(recordId);
            session.setPurpose(purpose);
            session.setStage(stage);
            session.setStatus(AgentSessionStatus.ACTIVE);
            session.setTurnCount(turnCount);
            session.setStageReaskCount(stageReaskCount);
            return session;
        }

        private Record record() {
            if (recordId == null) {
                return null;
            }
            Record record = new Record();
            record.setId(recordId);
            record.setUserId(USER_ID);
            record.setContent(recordContent);
            record.setStatus(purpose == AgentSessionPurpose.REVIEW_CHAT
                    ? RecordStatus.UNLOCKED
                    : RecordStatus.DRAFT);
            record.setCreatedAt(LocalDateTime.of(2026, 3, 14, 21, 0));
            return record;
        }

        AgentEvalHarness build() {
            return new AgentEvalHarness(this);
        }
    }
}
