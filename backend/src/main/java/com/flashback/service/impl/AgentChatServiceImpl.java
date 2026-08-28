package com.flashback.service.impl;

import com.flashback.agent.AgentChatMode;
import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentMockResponder;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.AgentStageMachine;
import com.flashback.agent.AgentWitnessTurnDirective;
import com.flashback.agent.AgentWitnessTurnPolicy;
import com.flashback.agent.AgentWitnessTurnType;
import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentGuardrailDowngrade;
import com.flashback.agent.guardrail.AgentGuardrailVerdict;
import com.flashback.agent.guardrail.AgentLayeredCorpus;
import com.flashback.agent.guardrail.AgentSourceCorpus;
import com.flashback.agent.guardrail.AgentTimeAttributionChecker;
import com.flashback.agent.memory.MemoryCueExtractor;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.agent.memory.MemoryPort;
import com.flashback.agent.memory.MemoryQuery;
import com.flashback.agent.reflection.AgentReflectionPolicy;
import com.flashback.agent.reflection.AgentReply;
import com.flashback.agent.reflection.AgentReplyPipeline;
import com.flashback.agent.resilience.AgentCallBudget;
import com.flashback.agent.resilience.AgentProviderFailureCategory;
import com.flashback.agent.resilience.AgentProviderFailures;
import com.flashback.agent.resilience.AgentResiliencePolicy;
import com.flashback.agent.safety.AgentSafetyDecision;
import com.flashback.agent.safety.AgentSafetyPolicy;
import com.flashback.agent.tool.AgentToolCallStatus;
import com.flashback.agent.tool.AgentToolCoordinator;
import com.flashback.agent.tool.AgentToolDecision;
import com.flashback.agent.tool.AgentToolName;
import com.flashback.agent.tool.AgentToolPendingArgs;
import com.flashback.agent.tool.AgentToolProposal;
import com.flashback.agent.tool.AgentToolRegistry;
import com.flashback.agent.tool.AgentToolSchemaFactory;
import com.flashback.agent.temporal.AgentTemporalPolicy;
import com.flashback.agent.temporal.TemporalDistanceBand;
import com.flashback.agent.temporal.TemporalPolicyResult;
import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.agent.trace.AgentTraceLayer;
import com.flashback.agent.trace.AgentTraceSink;
import com.flashback.agent.trace.AgentTraceVersions;
import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentConversationIntent;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
import com.flashback.domain.AgentMemorySource;
import com.flashback.domain.AgentMemorySourceKind;
import com.flashback.domain.AgentToolCall;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.mapper.AgentMemorySourceMapper;
import com.flashback.mapper.AgentMessageMapper;
import com.flashback.mapper.AgentSessionMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.service.AgentChatService;
import com.flashback.service.TagService;
import com.flashback.service.data.DataOwnershipMutationGuard;
import com.flashback.vo.AgentMemorySourceVO;
import com.flashback.vo.AgentMessageVO;
import com.flashback.vo.AgentSessionVO;
import com.flashback.vo.AgentToolCallVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Agent 多轮对话编排实现（C1 + C2）。
 *
 * 关键约定：
 * 1. 阶段推进由 AgentStageMachine 决定，不交给模型（design.md 决策 4）；
 * 2. provider 不可用/失败时返回显式 UNAVAILABLE / FAILED，不做本地兜底冒充成功（决策 5）；
 * 3. provider 失败时用户消息已落库并保留，Agent 回复不落库，允许同轮重试；
 * 4. 日志只输出结构化元数据，绝不输出对话原文或日记原文；
 * 5. 素材回填仍由前端在用户确认后走既有记录接口。
 *
 * C2 增量：
 * 6. 工具提议经原生 function calling 产生，只落库为待确认，**不在本轮执行**（决策 2、9）；
 * 7. 工具执行的唯一入口是 confirmToolCall，且不推进阶段、不增加轮次（决策 8）；
 * 8. 工具不可用时只是不下发 tools，不改用任何自研提议协议（决策 1：无降级）。
 */
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private static final Logger log = LoggerFactory.getLogger(AgentChatServiceImpl.class);
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String STATUS_FAILED = "FAILED";

    private final AgentSessionMapper agentSessionMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final RecordMapper recordMapper;
    private final AgentStageMachine stageMachine;
    private final AgentWitnessTurnPolicy witnessTurnPolicy;
    private final AgentPromptBuilder promptBuilder;
    private final AgentGuardrailPolicy guardrailPolicy;
    private final AgentModelClient modelClient;
    private final AgentMockResponder mockResponder;
    private final AgentToolSchemaFactory toolSchemaFactory;
    private final AgentToolCoordinator toolCoordinator;
    private final AgentFaithfulnessChecker faithfulnessChecker;
    private final AgentContentChecker contentChecker;
    private final AgentGuardrailDowngrade guardrailDowngrade;
    private final AgentTimeAttributionChecker timeAttributionChecker;
    private final AgentReplyPipeline replyPipeline;
    private final AgentResiliencePolicy resiliencePolicy;
    private final AgentSafetyPolicy safetyPolicy = new AgentSafetyPolicy();
    private final AgentTemporalPolicy temporalPolicy;
    private final MemoryPort memoryPort;
    private final AgentMemorySourceMapper agentMemorySourceMapper;
    private final MemoryCueExtractor memoryCueExtractor;
    private final RecordTagMapper recordTagMapper;
    private final TagService tagService;
    private final AgentTraceSink traceSink;
    private final AgentTraceVersions traceVersions;
    private final AppAgentProperties appAgentProperties;
    private final Clock clock;
    private DataOwnershipMutationGuard dataOwnershipMutationGuard;

    @Autowired
    void setDataOwnershipMutationGuard(DataOwnershipMutationGuard guard) { this.dataOwnershipMutationGuard = guard; }

    public AgentChatServiceImpl(
            AgentSessionMapper agentSessionMapper,
            AgentMessageMapper agentMessageMapper,
            RecordMapper recordMapper,
            AgentStageMachine stageMachine,
            AgentPromptBuilder promptBuilder,
            AgentGuardrailPolicy guardrailPolicy,
            AgentModelClient modelClient,
            AgentMockResponder mockResponder,
            AgentToolSchemaFactory toolSchemaFactory,
            AgentToolCoordinator toolCoordinator,
            AgentFaithfulnessChecker faithfulnessChecker,
            AgentContentChecker contentChecker,
            AgentGuardrailDowngrade guardrailDowngrade,
            AgentTimeAttributionChecker timeAttributionChecker,
            AgentReflectionPolicy reflectionPolicy,
            MemoryPort memoryPort,
            AgentMemorySourceMapper agentMemorySourceMapper,
            MemoryCueExtractor memoryCueExtractor,
            RecordTagMapper recordTagMapper,
            TagService tagService,
            AgentTraceSink traceSink,
            AgentTraceVersions traceVersions,
            AppAgentProperties appAgentProperties,
            Clock clock) {
        this.agentSessionMapper = agentSessionMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.recordMapper = recordMapper;
        this.stageMachine = stageMachine;
        this.witnessTurnPolicy = new AgentWitnessTurnPolicy(stageMachine);
        this.promptBuilder = promptBuilder;
        this.guardrailPolicy = guardrailPolicy;
        this.modelClient = modelClient;
        this.mockResponder = mockResponder;
        this.toolSchemaFactory = toolSchemaFactory;
        this.toolCoordinator = toolCoordinator;
        this.faithfulnessChecker = faithfulnessChecker;
        this.contentChecker = contentChecker;
        this.guardrailDowngrade = guardrailDowngrade;
        this.timeAttributionChecker = timeAttributionChecker;
        this.resiliencePolicy = new AgentResiliencePolicy();
        this.temporalPolicy = new AgentTemporalPolicy(appAgentProperties, clock);
        this.replyPipeline = new AgentReplyPipeline(
                promptBuilder,
                guardrailPolicy,
                modelClient,
                mockResponder,
                toolSchemaFactory,
                contentChecker,
                guardrailDowngrade,
                timeAttributionChecker,
                reflectionPolicy,
                resiliencePolicy);
        this.memoryPort = memoryPort;
        this.agentMemorySourceMapper = agentMemorySourceMapper;
        this.memoryCueExtractor = memoryCueExtractor;
        this.recordTagMapper = recordTagMapper;
        this.tagService = tagService;
        this.traceSink = traceSink;
        this.traceVersions = traceVersions;
        this.appAgentProperties = appAgentProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AgentSessionVO startOrResume(Long userId, AgentSessionStartRequest request) {
        assertOwnershipWritable(userId);
        AgentCallBudget budget = newCallBudget();
        Long recordId = request == null ? null : request.getRecordId();
        // C3b：模式在最开始定一次，之后编排只问模式、不问 purpose（design 决策 1）。
        AgentSessionPurpose purpose = request == null
                ? AgentSessionPurpose.WRITING_GUIDANCE
                : request.purposeOrDefault();
        AgentChatMode mode = AgentChatMode.of(purpose);
        AgentConversationIntent requestedIntent = request == null
                ? AgentConversationIntent.LISTEN
                : request.conversationIntentOrDefault();
        boolean intentExplicitlyRequested = request != null
                && request.getConversationIntent() != null;

        if (purpose == AgentSessionPurpose.REVIEW_CHAT
                && request != null
                && request.getConversationIntent() != null) {
            throw new BizException(
                    ErrorCode.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "回看对话不接受写作会话意图");
        }

        if (mode.requiresRecord() && recordId == null) {
            // 回看没有可回看的对象。
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "回看对话需要指定记录");
        }
        // 校验归属与记录状态；返回值本身不参与后续编排。
        requireOwnedRecordIfPresent(userId, recordId, mode);

        AgentSession existing = agentSessionMapper.selectActiveByUserAndRecord(userId, recordId, purpose);
        if (existing != null) {
            normalizeWritingSession(existing);
            List<AgentMessage> existingMessages = agentMessageMapper.selectBySessionId(existing.getId());
            AgentMessage pendingUserMessage = findPendingUserMessage(
                    existingMessages, existing.getTurnCount());
            if (purpose == AgentSessionPurpose.WRITING_GUIDANCE
                    && intentExplicitlyRequested
                    && existing.getConversationIntent() != requestedIntent
                    && pendingUserMessage == null) {
                existing.setConversationIntent(requestedIntent);
                existing.setUpdatedAt(LocalDateTime.now(clock));
                agentSessionMapper.updateConversationIntent(existing);
            }
            // 正常中断恢复：若最后一条用户消息尚无同轮 Agent 回复，显式提示重试，
            // 不把 provider 失败后的半轮会话误报为 SUCCESS，也不让 start/resume
            // 绕过 intent switch 的冲突边界，使同一 attempt 在另一 policy 下重放。
            if (!existingMessages.isEmpty()) {
                AgentSessionVO resumed = toSessionVO(existing, existingMessages, null, statusOfConfig());
                if (pendingUserMessage != null) {
                    resumed.setStatus(STATUS_FAILED);
                    resumed.setMessage("上一轮回复尚未完成，请重试");
                }
                return resumed;
            }
            // 会话已创建但开场 provider 调用失败：再次触发时只重试开场，
            // 不创建第二个 ACTIVE 会话。
            String unavailableReason = modelClient.unavailableReason();
            if (unavailableReason != null) {
                logProviderUnavailable("opening-retry", openingStageOf(mode));
                return unavailable(existing, existingMessages, resiliencePolicy.failureMessage(
                        "opening-retry", openingStageOf(mode), AgentProviderFailureCategory.AUTH_CONFIGURATION));
            }
            LocalDateTime retryAt = LocalDateTime.now(clock);
            AgentStage openingStage = openingStageOf(mode);
            TemporalPolicyResult openingTemporal = temporalPolicy.evaluate(
                    mode, "", reviewFragmentsOf(userId, existing, mode), List.of());
            AgentReply openingReply = generateReply(
                    openingStage, List.of(), openingTemporal,
                    "opening-retry", mode, existing.getConversationIntent(), budget);
            if (!openingReply.success()) {
                return failed(existing, existingMessages, openingReply.message());
            }
            AgentMessage openingMessage = persistMessage(
                    existing, AgentMessageRole.ASSISTANT, 0, openingStage, openingReply.content(), retryAt);
            persistMemorySources(existing, openingMessage, openingTemporal.injectedFragments());
            existing.setStage(openingStage);
            updateProgress(existing, retryAt);
            return toSessionVO(existing, List.of(openingMessage), null, STATUS_SUCCESS);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AgentSession session = new AgentSession();
        session.setUserId(userId);
        session.setRecordId(recordId);
        session.setPurpose(purpose);
        session.setConversationIntent(mode.isStageMachineDriven() ? requestedIntent : null);
        // P4.1：新写作引导从单一 WITNESS 起；回看恒为 REVIEW。
        session.setStage(mode.isStageMachineDriven() ? AgentStage.WITNESS : AgentStage.REVIEW);
        session.setStatus(AgentSessionStatus.ACTIVE);
        session.setTurnCount(0);
        session.setStageReaskCount(0);
        session.setLastActiveAt(now);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        agentSessionMapper.insert(session);

        String unavailableReason = modelClient.unavailableReason();
        if (unavailableReason != null) {
            // 会话已创建，用户下次可重试；但明确告知当前不可用，不给假开场。
            logProviderUnavailable("opening", openingStageOf(mode));
            return unavailable(session, List.of(), resiliencePolicy.failureMessage(
                    "opening", openingStageOf(mode), AgentProviderFailureCategory.AUTH_CONFIGURATION));
        }

        AgentStage openingStage = openingStageOf(mode);
        TemporalPolicyResult openingTemporal = temporalPolicy.evaluate(
                mode, "", reviewFragmentsOf(userId, session, mode), List.of());
        AgentReply reply = generateReply(
                openingStage, List.of(), openingTemporal,
                "opening", mode, session.getConversationIntent(), budget);
        if (!reply.success()) {
            return failed(session, List.of(), reply.message());
        }

        AgentMessage assistantMessage = persistMessage(
                session, AgentMessageRole.ASSISTANT, 0, openingStage, reply.content(), now);
        persistMemorySources(session, assistantMessage, openingTemporal.injectedFragments());
        session.setStage(openingStage);
        updateProgress(session, now);

        return toSessionVO(session, List.of(assistantMessage), null, STATUS_SUCCESS);
    }

    @Override
    public AgentSessionVO getSession(Long userId, Long sessionId) {
        AgentSession session = requireOwnedSession(userId, sessionId);
        normalizeWritingSession(session);
        List<AgentMessage> messages = agentMessageMapper.selectBySessionId(sessionId);
        AgentSessionVO vo = toSessionVO(session, messages, null, statusOfConfig());
        if (findPendingUserMessage(messages, session.getTurnCount()) != null) {
            vo.setStatus(STATUS_FAILED);
            vo.setMessage("上一轮回复尚未完成，请重试");
        }
        return vo;
    }

    /**
     * C5：轨迹落库的**唯一出口**。
     *
     * 用 try/finally 包住整轮编排（design 决策 2）：provider 失败、护栏降级、
     * fail-closed 丢弃全都是提前返回的路径，散落的埋点在这些路径上最容易被跳过——
     * 而那正是最需要痕迹的时刻。放在 finally 里，早退也会带着已收集到的部分落库。
     *
     * 收集器在 {@link #sendMessageTraced} 里创建（要先算出 turnNo），
     * 因此用一个单元素持有者把它传回来。
     */
    @Override
    @Transactional
    public AgentSessionVO sendMessage(Long userId, Long sessionId, AgentMessageRequest request) {
        assertOwnershipWritable(userId);
        AgentTraceCollector[] holder = new AgentTraceCollector[1];
        AgentCallBudget budget = newCallBudget();
        try {
            return sendMessageTraced(userId, sessionId, request, holder, budget);
        } finally {
            // persist 自身 fail-open 且走独立事务：痕迹写不下去不能让这一轮对话挂掉，
            // 更不能把已落库的用户消息一起回滚（决策 7）。
            traceSink.persist(holder[0]);
        }
    }

    private AgentSessionVO sendMessageTraced(
            Long userId,
            Long sessionId,
            AgentMessageRequest request,
            AgentTraceCollector[] traceHolder,
            AgentCallBudget budget) {
        AgentSession session = requireOwnedSession(userId, sessionId);
        requireActive(session);
        normalizeWritingSession(session);

        String content = normalizeUserInput(request == null ? null : request.getContent());
        LocalDateTime now = LocalDateTime.now(clock);
        List<AgentMessage> history = agentMessageMapper.selectBySessionId(sessionId);

        AgentChatMode mode = AgentChatMode.of(session.getPurpose());
        int maxTurns = maxTurnsOf(mode);

        AgentMessage pendingUserMessage = findPendingUserMessage(history, session.getTurnCount());
        int turnNo;
        AgentStage targetStage;
        AgentWitnessTurnDirective directive;
        boolean retry = pendingUserMessage != null;
        if (retry) {
            // provider 失败后的同轮重试：用户消息已经落库，不重复 insert，也不再次推进状态机。
            if (!pendingUserMessage.getContent().equals(content)) {
                throw new BizException(
                        ErrorCode.BAD_REQUEST,
                        HttpStatus.BAD_REQUEST,
                        "上一轮回复失败，请先重试原消息");
            }
            turnNo = pendingUserMessage.getTurnNo();
            targetStage = session.getStage();
            directive = directiveFor(mode, session, content, turnNo, maxTurns);
        } else {
            turnNo = session.getTurnCount() + 1;
            targetStage = null;
            directive = null;
        }

        // 轨迹从这里开始收集：turnNo 已确定，且后续每条分支都要留痕。
        AgentTraceCollector trace = beginTrace(session, turnNo, retry);
        traceHolder[0] = trace;
        traceOf(trace, t -> t.mode(mode, maxTurns));

        if (retry) {
            traceOf(trace, t -> t.stageRetained(session.getStage()));
            AgentWitnessTurnDirective retryDirective = directive;
            traceOf(trace, t -> t.witnessPolicy(
                    mode.isStageMachineDriven() ? session.getConversationIntent() : null,
                    retryDirective.type(),
                    retryDirective.maxQuestions()));
        } else if (mode.isStageMachineDriven()) {
            AgentStage fromStage = session.getStage();
            directive = directiveFor(mode, session, content, turnNo, maxTurns);

            // 用户的话先落库：即使随后 provider 失败，用户输入也不丢。
            AgentMessage userMessage = persistMessage(
                    session, AgentMessageRole.USER, turnNo, session.getStage(), content, now);
            history = appended(history, userMessage);
            session.setTurnCount(turnNo);
            session.setStageReaskCount(0);
            session.setStage(directive.nextStage());
            updateProgress(session, now);
            targetStage = session.getStage();
            AgentStage toStage = targetStage;
            AgentWitnessTurnDirective turnDirective = directive;
            traceOf(trace, t -> t.stageDecision(fromStage, toStage, turnDirective.reason())
                    .witnessPolicy(
                            session.getConversationIntent(),
                            turnDirective.type(),
                            turnDirective.maxQuestions()));
        } else {
            // C3b：回看无阶段机（design 决策 2）。阶段恒为 REVIEW，
            // stageReaskCount 不被回看逻辑改写——回看不追问，没有「同阶段再问一次」的概念。
            directive = witnessTurnPolicy.reviewTurn(content, turnNo, maxTurns);
            AgentMessage userMessage = persistMessage(
                    session, AgentMessageRole.USER, turnNo, AgentStage.REVIEW, content, now);
            history = appended(history, userMessage);
            session.setTurnCount(turnNo);
            session.setStage(AgentStage.REVIEW);
            updateProgress(session, now);
            targetStage = AgentStage.REVIEW;
            AgentWitnessTurnDirective turnDirective = directive;
            traceOf(trace, t -> t.stageRetained(AgentStage.REVIEW)
                    .witnessPolicy(null, turnDirective.type(), turnDirective.maxQuestions()));
        }

        AgentSafetyDecision safetyDecision = safetyPolicy.assess(content);
        traceOf(trace, t -> t.safetyResponse(
                safetyDecision.level(), safetyDecision.rule(), safetyDecision.intervenes()));
        if (safetyDecision.intervenes()) {
            AgentMessage assistantMessage = persistMessage(
                    session,
                    AgentMessageRole.ASSISTANT,
                    turnNo,
                    targetStage,
                    AgentSafetyPolicy.LOCAL_RESPONSE,
                    now);
            traceOf(trace, t -> t.memorySources(0).tools(false, 0, 0).material(false, 0));
            history = appended(history, assistantMessage);
            AgentSessionVO vo = toSessionVO(session, history, null, STATUS_SUCCESS);
            vo.setPendingToolCall(null);
            return vo;
        }

        String unavailableReason = modelClient.unavailableReason();
        if (unavailableReason != null) {
            traceOf(trace, t -> t.providerUnavailable(budget));
            return unavailable(session, history, resiliencePolicy.failureMessage(
                    "turn", session.getStage(), AgentProviderFailureCategory.AUTH_CONFIGURATION));
        }

        AgentToolContext toolContext = buildToolContext(session, mode);

        // C3：先检索，再用**同一份注入列表**构造来源集合（不变量 1）。
        // 检索与建语料必须共用这个列表——若各取一次，
        // 就会出现「注入了 A 但来源集合装着 B」或「来源里有没注入的片段」，
        // 后者会让忠实度闸退化成「用户这辈子说过就放行」。
        // C3b：回看时，被回看记录自身的内容也是「过去的表达」，与检索到的历史片段
        // 一同进入 MEMORY 层（design 决策 4）。放进 SESSION 层会让 Agent 复述那时的话
        // 时**不需要**带时间归属，于是「你觉得撑不住」读起来就像用户此刻说的——
        // 那正是 C3a 整层护栏要防的事。
        List<MemoryFragment> reviewFragments = reviewFragmentsOf(userId, session, mode);
        List<MemoryFragment> retrievedMemory = retrieveMemory(session, history, trace);
        TemporalPolicyResult temporal = temporalPolicy.evaluate(
                mode, content, reviewFragments, retrievedMemory);
        List<MemoryFragment> injectedMemory = temporal.injectedFragments();
        traceTemporal(trace, temporal);
        traceMemoryScale(trace, injectedMemory);
        // C4 + C3：来源集合在生成回复前构造一次，回复检查与工具提议校验共用同一份，
        // 保证「Agent 说的话」与「要写进正文的文字」判定基准一致。
        AgentLayeredCorpus corpus = layeredCorpusOf(history, injectedMemory);
        String draftExcerpt = mode.isStageMachineDriven()
                // 回看不注入「草稿正文只读引用」——记录内容已在 MEMORY 层，
                // 且那段 prompt 的措辞（用户已经写下的正文）对已解锁记录是错的。
                ? draftExcerptOf(userId, session.getRecordId())
                : null;
        AgentReply reply = generateReply(
                targetStage,
                directive,
                history,
                draftExcerpt,
                "turn",
                toolContext,
                corpus,
                injectedMemory,
                temporal,
                budget,
                trace);
        if (!reply.success()) {
            return failed(session, history, reply.message());
        }
        traceOf(trace, t -> t.temporalPatternUsed(
                temporal.enabled() && reply.content() != null && reply.content().contains("似乎不止一次")));

        AgentMessage assistantMessage = persistMessage(
                session, AgentMessageRole.ASSISTANT, turnNo, targetStage, reply.content(), now);
        int sourceCount = persistMemorySources(session, assistantMessage, injectedMemory);
        traceOf(trace, t -> t.memorySources(sourceCount));
        history = appended(history, assistantMessage);

        // 提议只落库为待确认，**不执行**；执行只发生在 confirmToolCall（C2 决策 2、9）。
        AgentToolCall pendingToolCall = null;
        if (toolContext.toolsEnabled()) {
            pendingToolCall = toolCoordinator.handleProposals(session, turnNo, reply.toolCalls(), corpus, trace);
        } else if (reply.hasToolCalls()) {
            // C3b：回看不挂 tools，但模型仍可能自作主张返回提议。fail-closed——
            // 丢弃并留结构化审计，不落待确认记录、不下发确认条，本轮回复照常返回。
            log.warn("agent tool call discarded fail-closed sessionId={} turnNo={} mode={} count={}",
                    session.getId(), turnNo, mode, reply.toolCalls().size());
            // C5：C3b 归档时这条分支未活体触发，正确性仅由单测覆盖。
            // 轨迹让它真发生的那一次能被记下。
            int discarded = reply.toolCalls().size();
            traceOf(trace, t -> t.toolsFailClosed(discarded));
        }
        int returnedToolCalls = reply.toolCalls().size();
        AgentToolCall proposed = pendingToolCall;
        boolean toolsEnabled = toolContext.toolsEnabled();
        traceOf(trace, t -> t.tools(toolsEnabled, returnedToolCalls, proposed == null ? 0 : 1));

        String materialDraft = null;
        // C3b：素材是否产出由模式决定（design 决策 8）。往已解锁记录的正文里追加
        // 此刻的整理会破坏它的时间完整性——用户几个月后无法分辨哪句是当时写的。
        if (mode.isMaterialProduced() && targetStage == AgentStage.CLOSING) {
            materialDraft = generateMaterial(history, trace, budget);
            String produced = materialDraft;
            traceOf(trace, t -> t.material(produced != null, produced == null ? 0 : produced.length()));
            endSession(session, now);
            traceOf(trace, t -> t.sessionEnded("closing"));
        } else if (!mode.isStageMachineDriven()
                && (directive.type() == AgentWitnessTurnType.CLOSE
                || session.getTurnCount() >= maxTurns)) {
            // 回看没有 CLOSING 阶段，达到轮次上限即温和收束（design 决策 2）。
            endSession(session, now);
            traceOf(trace, t -> t.sessionEnded("turn-limit"));
        }

        AgentSessionVO vo = toSessionVO(session, history, materialDraft, STATUS_SUCCESS);
        vo.setPendingToolCall(toToolCallVO(pendingToolCall));
        return vo;
    }

    @Override
    @Transactional
    public AgentSessionVO confirmToolCall(
            Long userId, Long sessionId, Long toolCallId, AgentToolDecision decision) {
        assertOwnershipWritable(userId);
        AgentSession session = requireOwnedSession(userId, sessionId);
        if (decision == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "decision不能为空");
        }

        AgentToolCall handled = toolCoordinator.confirm(userId, sessionId, toolCallId, decision);
        if (handled == null) {
            // 跨用户、不存在、会话不匹配统一按未找到处理，不泄露存在性。
            throw new NotFoundException("操作不存在");
        }

        // 工具确认不推进阶段、不增加轮次（design 决策 8）：
        // 会话进度完全不变，仅工具状态流转。
        List<AgentMessage> history = agentMessageMapper.selectBySessionId(sessionId);
        AgentSessionVO vo = toSessionVO(session, history, null, statusOfConfig());
        vo.setLastToolCallResult(toToolCallVO(handled));
        vo.setPendingToolCall(toToolCallVO(toolCoordinator.pendingOf(sessionId)));
        if (handled.getStatus() == AgentToolCallStatus.FAILED) {
            // 失败必须显式，不谎报成功。
            vo.setStatus(STATUS_FAILED);
            vo.setMessage(handled.getResultSummary());
        }
        return vo;
    }

    @Override
    @Transactional
    public AgentSessionVO finish(Long userId, Long sessionId) {
        assertOwnershipWritable(userId);
        AgentCallBudget budget = newCallBudget();
        AgentSession session = requireOwnedSession(userId, sessionId);
        List<AgentMessage> history = agentMessageMapper.selectBySessionId(sessionId);
        AgentChatMode mode = AgentChatMode.of(session.getPurpose());

        // C3b：回看主动结束不产素材（design 决策 8），也不调 provider。
        if (session.getStatus() == AgentSessionStatus.ENDED) {
            return toSessionVO(
                    session, history, mode.isMaterialProduced() ? generateMaterial(history, null, budget) : null,
                    statusOfConfig());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String materialDraft = mode.isMaterialProduced() ? generateMaterial(history, null, budget) : null;
        endSession(session, now);
        return toSessionVO(session, history, materialDraft, statusOfConfig());
    }

    // ---------- C5 决策轨迹 ----------

    /**
     * 创建本轮的轨迹收集器；可观测关闭时返回 null。
     *
     * 返回 null 而不是一个空实现的理由：关闭时应当**完全不产生**采集开销，
     * 且 {@link #traceOf} 的 null 检查让调用点无需各自判断开关。
     */
    private AgentTraceCollector beginTrace(AgentSession session, int turnNo, boolean retry) {
        if (!traceSink.isEnabled()) {
            // 关闭必须留痕，不静默表现为轨迹无数据（沿用 C3a memory 开关的既有语义）。
            traceSink.traceDisabled(session.getId());
            return null;
        }
        // 同轮重试时 (sessionId, turnNo) 会重复，靠 attemptNo 区分「重试」与「新一轮」。
        int attemptNo = traceSink.nextAttemptNo(session.getId(), turnNo, retry);
        AgentTraceCollector trace = new AgentTraceCollector(
                session.getId(),
                session.getUserId(),
                session.getRecordId(),
                turnNo,
                attemptNo,
                session.getPurpose(),
                session.getStage());
        // 版本锚点由文案内容哈希派生（决策 6）：改文案自动变，不依赖人工 bump。
        return trace.versions(traceVersions.promptVersion(), traceVersions.policyVersion());
    }

    /**
     * 对收集器执行一次采集；收集器为 null（可观测关闭）时安静跳过。
     *
     * 采集自身绝不抛出：可观测是辅助设施，不该有能力搞挂对话（决策 7）。
     */
    private void traceOf(AgentTraceCollector trace, java.util.function.Consumer<AgentTraceCollector> action) {
        if (trace == null) {
            return;
        }
        try {
            action.accept(trace);
        } catch (RuntimeException ex) {
            log.warn("agent trace collect failed sessionId={} cause={}",
                    trace.sessionId(), ex.getClass().getSimpleName());
        }
    }

    /**
     * 记忆注入规模。只记条数与总长度，不记片段内容。
     */
    private void traceMemoryScale(AgentTraceCollector trace, List<MemoryFragment> injected) {
        if (trace == null) {
            return;
        }
        int chars = 0;
        for (MemoryFragment fragment : injected) {
            if (fragment != null && fragment.text() != null) {
                chars += fragment.text().length();
            }
        }
        int injectedChars = chars;
        traceOf(trace, t -> t.memoryInjected(injected.size(), injectedChars));
    }

    // ---------- 内部实现 ----------

    /**
     * 开场与开场重试。
     *
     * C3b：写作引导的开场没有任何来源（用户还没说话，也没有 memory 线索）；
     * 回看的开场则要带上被回看记录的内容，否则 Agent 第一句就无从提起那时的事。
     * 记录内容进 MEMORY 层，故开场引用它同样要带时间归属。
     */
    private AgentReply generateReply(
            AgentStage targetStage,
            List<AgentMessage> history,
            TemporalPolicyResult temporal,
            String operation,
            AgentChatMode mode,
            AgentConversationIntent conversationIntent,
            AgentCallBudget budget) {
        AgentWitnessTurnDirective directive = mode.isStageMachineDriven()
                ? witnessTurnPolicy.opening(conversationIntent)
                : witnessTurnPolicy.reviewOpening();
        return generateReply(
                targetStage, directive, history, null, operation,
                new AgentToolContext(false, null),
                layeredCorpusOf(history, temporal.injectedFragments()), temporal.injectedFragments(), temporal,
                budget,
                // 开场不落轨迹：它不属于任何一轮（turnNo=0，且没有用户消息与之配对），
                // 强行记一条会让「一轮一条」的语义破掉。开场的护栏与 provider 结果
                // 仍走既有结构化日志。
                null);
    }

    /**
     * C4：构造忠实度判定的会话层来源——**只含本会话用户自己说过的话**。
     *
     * 边界（C4 design.md 关键不变量 5）：不含 Agent 自己的表达（否则 Agent 上一轮说的话
     * 会成为下一轮增写的「合法来源」，忠实度闸自我失效）。
     * C3 起跨记录片段进入独立的记忆层，见 {@link #layeredCorpusOf}。
     */
    private AgentSourceCorpus corpusOf(List<AgentMessage> history) {
        return AgentSourceCorpus.of(history, faithfulnessChecker.ngramSize());
    }

    /**
     * C3：构造分层来源集合。
     *
     * @param injectedFragments 本轮**实际注入** prompt 的片段；空表示无记忆层，
     *                          此时判定行为与 C4 现状完全一致
     */
    private AgentLayeredCorpus layeredCorpusOf(
            List<AgentMessage> history, List<MemoryFragment> injectedFragments) {
        if (injectedFragments == null || injectedFragments.isEmpty()) {
            return AgentLayeredCorpus.sessionOnly(corpusOf(history));
        }
        List<String> texts = new ArrayList<>();
        for (MemoryFragment fragment : injectedFragments) {
            if (fragment != null && fragment.text() != null && !fragment.text().isBlank()) {
                texts.add(fragment.text());
            }
        }
        return AgentLayeredCorpus.of(history, texts, faithfulnessChecker.ngramSize());
    }

    /**
     * C3：检索本轮要注入的记忆片段。
     *
     * fail-open**仅对能力**（design.md 决策 6）：检索异常时返回空列表，
     * 本轮退回无记忆行为、对话正常继续。
     * 护栏方向相反——没有记忆层就没有 MEMORY 来源，判定照旧严格，
     * 绝不会因为「检索失败了所以宽容一点」。
     *
     * 记忆能力是增强而非依赖：让一次 LIKE 查询的超时毁掉用户正在进行的写作对话，
     * 违反 baseline「Agent 可用性 SHALL NOT 成为记录生命周期的依赖」的精神。
     */
    private List<MemoryFragment> retrieveMemory(
            AgentSession session, List<AgentMessage> history, AgentTraceCollector trace) {
        AppAgentProperties.Memory config = appAgentProperties.getMemory();
        boolean configEnabled = config.isEnabled();
        boolean sessionEnabled = session.isCrossRecordMemoryEnabled()
                && session.getStatus() == AgentSessionStatus.ACTIVE;
        boolean allowed = configEnabled && sessionEnabled;
        traceOf(trace, t -> t.memoryAuthorization(configEnabled, sessionEnabled, allowed));
        if (!allowed) {
            // 配置或用户授权任一关闭都必须留痕，且 cue / MemoryPort 调用数为 0。
            log.info("agent memory retrieval blocked sessionId={} config={} sessionAuth={}",
                    session.getId(), configEnabled, sessionEnabled);
            traceOf(trace, t -> t.memoryRetrieval(false, false, false, 0, 0, 0));
            return List.of();
        }
        List<String> keywords = List.of();
        List<Long> tagIds = List.of();
        try {
            keywords = memoryCueExtractor.extractKeywords(history);
            tagIds = memoryTagIdsOf(session);
            MemoryQuery query = new MemoryQuery(
                    session.getUserId(),
                    session.getPurpose(),
                    keywords,
                    tagIds,
                    session.getRecordId(),
                    config.getMaxFragments());
            if (!query.hasCue()) {
                int cues = keywords.size();
                int tags = tagIds.size();
                traceOf(trace, t -> t.memoryRetrieval(true, false, false, cues, tags, 0));
                return List.of();
            }
            List<MemoryFragment> retrieved = memoryPort.retrieve(query);
            int cues = keywords.size();
            int tags = tagIds.size();
            int hits = retrieved == null ? 0 : retrieved.size();
            traceOf(trace, t -> t.memoryRetrieval(true, false, true, cues, tags, hits));
            return retrieved;
        } catch (RuntimeException ex) {
            // 只记异常类型，不记检索线索或片段内容。
            log.warn("agent memory retrieval failed sessionId={} cause={}",
                    session.getId(), ex.getClass().getSimpleName());
            int cues = keywords.size();
            int tags = tagIds.size();
            traceOf(trace, t -> t.memoryRetrieval(true, true, false, cues, tags, 0));
            return List.of();
        }
    }

    /**
     * C3b：模式对应的开场阶段。
     */
    private AgentStage openingStageOf(AgentChatMode mode) {
        return mode.isStageMachineDriven() ? AgentStage.WITNESS : AgentStage.REVIEW;
    }

    private AgentWitnessTurnDirective directiveFor(
            AgentChatMode mode,
            AgentSession session,
            String userInput,
            int turnNo,
            int maxTurns) {
        if (mode.isStageMachineDriven() && session.getStage() == AgentStage.CLOSING) {
            return AgentWitnessTurnDirective.close(
                    AgentStage.CLOSING, com.flashback.agent.AgentStageDecision.Reason.CLOSED);
        }
        return mode.isStageMachineDriven()
                ? witnessTurnPolicy.decide(
                        session.getConversationIntent(), userInput, turnNo, maxTurns)
                : witnessTurnPolicy.reviewTurn(userInput, turnNo, maxTurns);
    }

    /**
     * C3b：模式对应的轮次上限。回看单列配置，默认比写作引导更短——
     * 回看是读后闲聊，没有要抵达的终点，聊到不想聊就该停。
     */
    private int maxTurnsOf(AgentChatMode mode) {
        return mode.isStageMachineDriven()
                ? appAgentProperties.getMaxTurnsPerSession()
                : appAgentProperties.getReview().getMaxTurnsPerSession();
    }

    /**
     * C3b：把被回看记录自身的内容做成记忆片段。
     *
     * 取 content + ai_summary + belief_then 三个字段（design 决策 3）——
     * 它们的时间语义统一，都属于「封存那一刻的表达与当时的整理」。
     * 刻意**不取** reality_later 与 reply：那是用户解锁**之后**写的，
     * 与正文不在同一个时间点上，而时间归属检查只区分「会话层 / 记忆层」，
     * 区分不了记忆层内部的两个时间点——混进来 Agent 就可能把后来的感想
     * 当成那时的想法复述。
     *
     * 写作引导模式下恒返回空列表。
     */
    private List<MemoryFragment> reviewFragmentsOf(Long userId, AgentSession session, AgentChatMode mode) {
        if (mode.isStageMachineDriven() || session.getRecordId() == null) {
            return List.of();
        }
        Record record = recordMapper.selectByIdAndUserId(session.getRecordId(), userId);
        if (record == null) {
            return List.of();
        }
        int limit = appAgentProperties.getReview().getRecordExcerptChars();
        LocalDateTime occurredAt = record.getCreatedAt();
        String timeLabel = occurredAt == null
                ? "那时候"
                : occurredAt.getYear() + "年" + occurredAt.getMonthValue() + "月";

        List<MemoryFragment> fragments = new ArrayList<>();
        String contextNote = blankToNull(record.getAgentMemoryContextNote());
        addReviewFragment(fragments, record.getId(), occurredAt, timeLabel, record.getContent(), limit, contextNote);
        addReviewFragment(fragments, record.getId(), occurredAt, timeLabel, record.getAiSummary(), limit, contextNote);
        addReviewFragment(fragments, record.getId(), occurredAt, timeLabel, record.getBeliefThen(), limit, contextNote);
        return List.copyOf(fragments);
    }

    private void addReviewFragment(
            List<MemoryFragment> sink,
            Long recordId,
            LocalDateTime occurredAt,
            String timeLabel,
            String text,
            int limit,
            String contextNote) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalized = text.trim();
        String clipped = normalized.length() <= limit ? normalized : normalized.substring(0, limit);
        sink.add(new MemoryFragment(recordId, occurredAt, timeLabel, clipped, contextNote));
    }

    /**
     * 当前草稿已绑定的标签，用作同标签关联的检索线索。
     */
    private List<Long> memoryTagIdsOf(AgentSession session) {
        if (session.getRecordId() == null) {
            return List.of();
        }
        List<Long> tagIds = recordTagMapper.selectTagIdsByRecordId(session.getRecordId());
        return tagIds == null ? List.of() : tagIds;
    }

    /**
     * 生成一轮回复。
     *
     * C2 变化：走 completeWithTools 并解析 message.tool_calls。
     * 若 provider 只返回提议而 content 为空，则用提议的 askText 作为该轮回复
     * （design 数据流 2.1 要点二），避免出现空白气泡。
     *
     * 无降级：工具不可用时只是不下发 tools，**不会**改用任何自研提议协议（决策 1）。
     */
    private AgentReply generateReply(
            AgentStage targetStage,
            AgentWitnessTurnDirective directive,
            List<AgentMessage> history,
            String draftExcerpt,
            String operation,
            AgentToolContext toolContext,
            AgentLayeredCorpus corpus,
            List<MemoryFragment> injectedMemory,
            TemporalPolicyResult temporal,
            AgentCallBudget budget,
            AgentTraceCollector trace) {
        return replyPipeline.generate(
                targetStage,
                directive,
                history,
                draftExcerpt,
                operation,
                toolContext != null && toolContext.toolsEnabled(),
                toolContext == null ? null : toolContext.supplement(),
                corpus,
                injectedMemory,
                temporal,
                budget,
                trace);
    }

    @Override
    @Transactional
    public AgentSessionVO switchConversationIntent(
            Long userId, Long sessionId, AgentConversationIntent conversationIntent) {
        assertOwnershipWritable(userId);
        AgentSession session = requireOwnedSession(userId, sessionId);
        requireActive(session);
        normalizeWritingSession(session);
        if (session.getPurpose() != AgentSessionPurpose.WRITING_GUIDANCE) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "回看对话不能切换写作意图");
        }
        if (conversationIntent == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "conversationIntent不能为空");
        }
        List<AgentMessage> history = agentMessageMapper.selectBySessionId(sessionId);
        if (findPendingUserMessage(history, session.getTurnCount()) != null) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT, "请先重试上一轮回复或结束对话");
        }
        if (session.getConversationIntent() != conversationIntent) {
            session.setConversationIntent(conversationIntent);
            session.setUpdatedAt(LocalDateTime.now(clock));
            agentSessionMapper.updateConversationIntent(session);
        }
        return toSessionVO(session, history, null, statusOfConfig());
    }

    @Override
    @Transactional
    public AgentSessionVO switchMemoryAuthorization(
            Long userId, Long sessionId, Boolean crossRecordMemoryEnabled) {
        assertOwnershipWritable(userId);
        if (crossRecordMemoryEnabled == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "crossRecordMemoryEnabled不能为空");
        }
        AgentSession session = requireOwnedSession(userId, sessionId);
        requireActive(session);
        normalizeWritingSession(session);
        List<AgentMessage> history = agentMessageMapper.selectBySessionId(sessionId);
        boolean pendingRetry = findPendingUserMessage(history, session.getTurnCount()) != null;
        if (pendingRetry && crossRecordMemoryEnabled && !session.isCrossRecordMemoryEnabled()) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.CONFLICT, "请先重试上一轮回复后再开启参考过去");
        }
        if (session.isCrossRecordMemoryEnabled() != crossRecordMemoryEnabled) {
            session.setCrossRecordMemoryEnabled(crossRecordMemoryEnabled);
            session.setUpdatedAt(LocalDateTime.now(clock));
            if (agentSessionMapper.updateMemoryAuthorization(session) != 1) {
                throw new BizException(
                        ErrorCode.BAD_REQUEST,
                        HttpStatus.CONFLICT,
                        "会话状态已变化，请刷新后重试");
            }
        }
        return toSessionVO(session, history, null, statusOfConfig());
    }

    private void traceTemporal(AgentTraceCollector trace, TemporalPolicyResult result) {
        if (trace == null || result == null) {
            return;
        }
        long recent = result.contexts().stream().filter(c -> c.band() == TemporalDistanceBand.RECENT).count();
        long distant = result.contexts().stream().filter(c -> c.band() == TemporalDistanceBand.DISTANT).count();
        long longAgo = result.contexts().stream().filter(c -> c.band() == TemporalDistanceBand.LONG_AGO).count();
        long unknown = result.contexts().stream().filter(c -> c.band() == TemporalDistanceBand.UNKNOWN).count();
        traceOf(trace, t -> t.temporal(
                result.enabled(), (int) recent, (int) distant, (int) longAgo, (int) unknown,
                result.beforeChars(), result.afterChars(), result.patternEvidence().eligible()));
    }

    /**
     * 组装本轮的工具上下文：是否下发 tools，以及预注入的读工具内容。
     *
     * 读工具（可选标签清单、草稿快照）由后端主动注入而非模型调用（design §3.1），
     * 因为 C2 不做单轮内 FC 循环（决策 9）。
     */
    private AgentToolContext buildToolContext(AgentSession session, AgentChatMode mode) {
        // C3b（tasks T-09）：这一处必须放在最前面。
        // 下面那个「无 recordId 则不下发 tools」的判断对回看会给出**错误答案**——
        // 回看会话恰好绑定一条记录，于是「回看无工具」不会自动成立。
        // 已解锁记录没有任何合法写操作（封存后 location/attachments/cover 不可变，
        // 正文也不该被回看追加），所以按模式显式短路，而不是依赖恰好没配。
        if (!mode.areToolsAvailable()) {
            return new AgentToolContext(false, null);
        }
        if (session.getRecordId() == null) {
            // 无草稿则无写工具作用对象，本轮不下发 tools。
            return new AgentToolContext(false, null);
        }
        if (modelClient.toolCallingUnavailableReason() != null) {
            return new AgentToolContext(false, null);
        }
        String supplement = promptBuilder.buildToolSupplement(
                tagService.listEnabled(null),
                toolCoordinator.recentSettled(session.getId(), appAgentProperties.getToolOutcomeWindow()));
        return new AgentToolContext(true, supplement);
    }

    private record AgentToolContext(boolean toolsEnabled, String supplement) {
    }

    private Long traceSessionId(AgentTraceCollector trace) {
        return trace == null ? null : trace.sessionId();
    }

    private Integer traceTurnNo(AgentTraceCollector trace) {
        return trace == null ? null : trace.turnNo();
    }

    /**
     * 素材整理失败不影响会话结束：素材为可选产物，缺失时前端不展示回填入口。
     *
     * C4：素材同为「模型产出且会进入用户正文」的文本，与工具正文参数结构完全同质，
     * 因此走同一道忠实度闸（design 决策 6）。不忠实的素材等同于「没生成出来」——
     * 复用既有的可选产物语义，前端零改动。
     */
    private String generateMaterial(List<AgentMessage> history) {
        return generateMaterial(history, null, newCallBudget());
    }

    private String generateMaterial(
            List<AgentMessage> history, AgentTraceCollector trace, AgentCallBudget budget) {
        AgentSourceCorpus corpus = corpusOf(history);
        if (modelClient.isMockProvider()) {
            return applyMaterialGuardrail(mockResponder.material(history), corpus, trace);
        }
        if (modelClient.unavailableReason() != null) {
            traceOf(trace, t -> t.materialFailed(AgentProviderFailureCategory.AUTH_CONFIGURATION, budget));
            return null;
        }
        long startedAt = System.nanoTime();
        try {
            String raw = modelClient.complete(promptBuilder.buildMaterialMessages(history), budget);
            return applyMaterialGuardrail(modelClient.extractText(raw, "material"), corpus, trace);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            AgentProviderFailureCategory category = AgentProviderFailures.fromThrowable(ex);
            logProviderIssue("material", AgentStage.CLOSING, startedAt, category);
            // 素材失败不改变本轮 outcome：素材是可选产物，缺失时前端只是不显示回填入口，
            // 把它记成 FAILED 会让「对话成功但素材没出来」看起来像一轮失败。
            traceOf(trace, t -> t.materialFailed(category, budget));
            return null;
        }
    }

    private AgentCallBudget newCallBudget() {
        return AgentCallBudget.start(appAgentProperties.getResilience().getProviderWorkTimeoutMillis());
    }

    /**
     * C4：素材路径的忠实度闸与降级。不忠实即丢弃，用户看不到回填入口。
     */
    private String applyMaterialGuardrail(
            String material, AgentSourceCorpus corpus, AgentTraceCollector trace) {
        if (material == null || material.isBlank()) {
            return material;
        }
        AgentGuardrailVerdict faithfulness = faithfulnessChecker.check(material, corpus);
        traceOf(trace, t -> t.guardrail(AgentTraceLayer.MATERIAL_FAITHFULNESS, faithfulness));
        if (!faithfulness.isPassed()) {
            // C5（V4 补齐）：同 applyReplyGuardrail，不再传 null。
            guardrailDowngrade.trace("material", traceSessionId(trace), traceTurnNo(trace), faithfulness);
            traceOf(trace, t -> t.downgrade(
                    AgentTraceLayer.MATERIAL_FAITHFULNESS, faithfulness.violation(), false));
            return null;
        }
        AgentGuardrailVerdict content = contentChecker.check(material, corpus);
        traceOf(trace, t -> t.guardrail(AgentTraceLayer.MATERIAL_CONTENT, content));
        if (!content.isPassed()) {
            guardrailDowngrade.trace("material", traceSessionId(trace), traceTurnNo(trace), content);
            traceOf(trace, t -> t.downgrade(AgentTraceLayer.MATERIAL_CONTENT, content.violation(), false));
            return null;
        }
        return material;
    }

    private AgentMessage persistMessage(
            AgentSession session,
            AgentMessageRole role,
            int turnNo,
            AgentStage stage,
            String content,
            LocalDateTime now) {
        AgentMessage message = new AgentMessage();
        message.setSessionId(session.getId());
        message.setUserId(session.getUserId());
        message.setRole(role);
        message.setTurnNo(turnNo);
        message.setStage(stage);
        message.setContent(content);
        message.setCreatedAt(now);
        agentMessageMapper.insert(message);
        return message;
    }

    private void updateProgress(AgentSession session, LocalDateTime now) {
        session.setLastActiveAt(now);
        session.setUpdatedAt(now);
        agentSessionMapper.updateProgress(session);
    }

    private void endSession(AgentSession session, LocalDateTime now) {
        session.setStage(AgentStage.ENDED);
        session.setStatus(AgentSessionStatus.ENDED);
        session.setStageReaskCount(0);
        updateProgress(session, now);
    }

    private AgentSession requireOwnedSession(Long userId, Long sessionId) {
        AgentSession session = agentSessionMapper.selectByIdAndUserId(sessionId, userId);
        if (session == null) {
            // 跨用户访问与不存在返回同一结果，不泄露会话存在性。
            throw new NotFoundException("会话不存在");
        }
        return session;
    }

    /** migration 之外的防御性兼容：只归一当前 session，不改写历史 message/trace。 */
    private void normalizeWritingSession(AgentSession session) {
        if (session == null || session.getPurpose() != AgentSessionPurpose.WRITING_GUIDANCE) {
            return;
        }
        boolean changed = false;
        if (session.getConversationIntent() == null) {
            session.setConversationIntent(AgentConversationIntent.LISTEN);
            changed = true;
        }
        if (session.getStatus() == AgentSessionStatus.ACTIVE
                && session.getStage() != AgentStage.WITNESS
                && session.getStage() != AgentStage.CLOSING) {
            session.setStage(AgentStage.WITNESS);
            session.setStageReaskCount(0);
            changed = true;
        }
        if (changed && session.getId() != null) {
            updateProgress(session, LocalDateTime.now(clock));
        }
    }

    private void requireActive(AgentSession session) {
        if (session.getStatus() != AgentSessionStatus.ACTIVE) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "会话已结束，可以重新开启一次对话");
        }
    }

    /**
     * 校验记录归属与状态。
     *
     * C3b：状态要求由模式给出，不再硬编码 DRAFT。
     * 写作引导仍只允许 DRAFT（封存后不可变契约的延续）；
     * 回看只允许 UNLOCKED——SEALED 未解锁的记录用户自己都还没到能看的时刻，
     * Agent 陪他聊它等于替时间拆封。
     */
    private Record requireOwnedRecordIfPresent(Long userId, Long recordId, AgentChatMode mode) {
        if (recordId == null) {
            return null;
        }
        Record record = recordMapper.selectByIdAndUserId(recordId, userId);
        if (record == null) {
            throw new NotFoundException("记录不存在");
        }
        if (!mode.allowsRecordStatus(record.getStatus())
                || (record.getStatus() == RecordStatus.DRAFT
                && record.getDraftExpiresAt() != null
                && !record.getDraftExpiresAt().isAfter(LocalDateTime.now(clock)))) {
            throw new BizException(
                    ErrorCode.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    mode.isStageMachineDriven()
                            ? "只有未过期草稿或已留下记录可以开启写作对话"
                            : "只有已解锁的记录可以开启回看对话");
        }
        return record;
    }

    private String draftExcerptOf(Long userId, Long recordId) {
        if (recordId == null) {
            return null;
        }
        Record record = recordMapper.selectByIdAndUserId(recordId, userId);
        return record == null ? null : record.getContent();
    }

    private String normalizeUserInput(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "content不能为空");
        }
        if (normalized.length() > appAgentProperties.getMaxUserInputChars()) {
            throw new BizException(
                    ErrorCode.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "content长度不能超过" + appAgentProperties.getMaxUserInputChars());
        }
        return normalized;
    }

    private AgentMessage findPendingUserMessage(List<AgentMessage> history, int turnCount) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        AgentMessage latestUser = null;
        AgentMessage latestAssistant = null;
        for (AgentMessage message : history) {
            if (message.getTurnNo() != turnCount) {
                continue;
            }
            if (message.getRole() == AgentMessageRole.USER) {
                latestUser = message;
            } else if (message.getRole() == AgentMessageRole.ASSISTANT) {
                latestAssistant = message;
            }
        }
        return latestUser != null && latestAssistant == null ? latestUser : null;
    }

    private String latestUserContent(List<AgentMessage> history) {
        if (history == null) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).getRole() == AgentMessageRole.USER) {
                return history.get(i).getContent();
            }
        }
        return null;
    }

    private List<AgentMessage> appended(List<AgentMessage> history, AgentMessage message) {
        List<AgentMessage> merged = new ArrayList<>(history == null ? List.of() : history);
        merged.add(message);
        return merged;
    }

    private AgentSessionVO unavailable(AgentSession session, List<AgentMessage> history, String message) {
        AgentSessionVO vo = toSessionVO(session, history, null, STATUS_UNAVAILABLE);
        vo.setMessage(message);
        return vo;
    }

    private AgentSessionVO failed(AgentSession session, List<AgentMessage> history, String message) {
        AgentSessionVO vo = toSessionVO(session, history, null, STATUS_FAILED);
        vo.setMessage(message);
        return vo;
    }

    private String statusOfConfig() {
        return modelClient.unavailableReason() == null ? STATUS_SUCCESS : STATUS_UNAVAILABLE;
    }

    private AgentSessionVO toSessionVO(
            AgentSession session,
            List<AgentMessage> messages,
            String materialDraft,
            String status) {
        AgentSessionVO vo = new AgentSessionVO();
        vo.setSessionId(session.getId());
        vo.setRecordId(session.getRecordId());
        vo.setConversationIntent(session.getPurpose() == AgentSessionPurpose.WRITING_GUIDANCE
                && session.getConversationIntent() != null
                ? session.getConversationIntent().name()
                : null);
        vo.setStage(session.getStage() == null ? null : session.getStage().name());
        vo.setSessionStatus(session.getStatus() == null ? null : session.getStatus().name());
        vo.setTurnCount(session.getTurnCount());
        vo.setMaxTurns(appAgentProperties.getMaxTurnsPerSession());
        vo.setCanContinue(session.getStatus() == AgentSessionStatus.ACTIVE
                && session.getTurnCount() < appAgentProperties.getMaxTurnsPerSession());
        vo.setCrossRecordMemoryEnabled(session.isCrossRecordMemoryEnabled());
        vo.setMessages(toMessageVOs(session, messages));
        vo.setMaterialDraft(materialDraft);
        vo.setSource(modelClient.provider());
        vo.setStatus(status);
        return vo;
    }

    /**
     * 映射工具提议视图。
     *
     * 不暴露 argsDigest / pendingArgs：审计与瞬态执行参数属后端内部。
     */
    private AgentToolCallVO toToolCallVO(AgentToolCall toolCall) {
        if (toolCall == null) {
            return null;
        }
        AgentToolCallVO vo = new AgentToolCallVO();
        vo.setToolCallId(toolCall.getId());
        vo.setTool(toolCall.getToolName());
        vo.setStatus(toolCall.getStatus() == null ? null : toolCall.getStatus().name());
        vo.setAskText(toolCall.getAskText());
        vo.setResultSummary(toolCall.getResultSummary());
        vo.setFailureType(toolCall.getFailureType());

        // 待确认期间才有 pendingArgs，据此让前端展示「加哪些标签 / 设到什么时间」。
        AgentToolName tool = AgentToolName.fromWireName(toolCall.getToolName());
        AgentToolProposal proposal = AgentToolPendingArgs.deserialize(
                tool, toolCall.getAskText(), toolCall.getPendingArgs());
        if (proposal != null) {
            if (tool == AgentToolName.ADD_RECORD_TAGS) {
                vo.setTagIds(proposal.tagIds());
            } else if (tool == AgentToolName.PROPOSE_UNLOCK_AT) {
                vo.setUnlockAt(proposal.unlockAt());
            }
        }
        return vo;
    }

    private List<AgentMessageVO> toMessageVOs(AgentSession session, List<AgentMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AgentMemorySource>> sourcesByMessage = sourcesByMessage(session);
        List<AgentMessageVO> result = new ArrayList<>(messages.size());
        for (AgentMessage message : messages) {
            AgentMessageVO vo = new AgentMessageVO();
            vo.setId(message.getId());
            vo.setRole(message.getRole() == null ? null : message.getRole().name());
            vo.setTurnNo(message.getTurnNo());
            vo.setStage(message.getStage() == null ? null : message.getStage().name());
            vo.setContent(message.getContent());
            vo.setCreatedAt(message.getCreatedAt());
            vo.setMemorySources(resolveMemorySources(session, message, sourcesByMessage));
            result.add(vo);
        }
        return result;
    }

    private Map<Long, List<AgentMemorySource>> sourcesByMessage(AgentSession session) {
        if (session == null || session.getId() == null || agentMemorySourceMapper == null) {
            return Map.of();
        }
        List<AgentMemorySource> rows = agentMemorySourceMapper.selectBySessionIdAndUserId(
                session.getId(), session.getUserId());
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<AgentMemorySource>> grouped = new LinkedHashMap<>();
        for (AgentMemorySource row : rows) {
            if (row == null || row.getAssistantMessageId() == null) {
                continue;
            }
            grouped.computeIfAbsent(row.getAssistantMessageId(), key -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private List<AgentMemorySourceVO> resolveMemorySources(
            AgentSession session, AgentMessage message, Map<Long, List<AgentMemorySource>> sourcesByMessage) {
        if (message == null || message.getRole() != AgentMessageRole.ASSISTANT || message.getId() == null) {
            return List.of();
        }
        List<AgentMemorySource> rows = sourcesByMessage.get(message.getId());
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<AgentMemorySourceVO> result = new ArrayList<>(rows.size());
        for (AgentMemorySource row : rows) {
            result.add(toMemorySourceVO(session, row));
        }
        return result;
    }

    private AgentMemorySourceVO toMemorySourceVO(AgentSession session, AgentMemorySource row) {
        AgentMemorySourceVO vo = new AgentMemorySourceVO();
        vo.setSourceKind(row.getSourceKind() == null ? null : row.getSourceKind().name());
        if (row.getSourceRecordId() == null) {
            vo.setAvailable(false);
            return vo;
        }
        Record record = recordMapper.selectByIdAndUserId(row.getSourceRecordId(), session.getUserId());
        if (!isMemorySourceAvailable(record)) {
            vo.setAvailable(false);
            return vo;
        }
        vo.setAvailable(true);
        vo.setRecordId(record.getId());
        vo.setDisplayTitle(blankToNull(record.getTitle()));
        vo.setOccurredAt(record.getCreatedAt());
        vo.setContextNote(blankToNull(record.getAgentMemoryContextNote()));
        return vo;
    }

    private boolean isMemorySourceAvailable(Record record) {
        if (record == null || record.getStatus() == RecordStatus.SEALED) {
            return false;
        }
        return record.getStatus() != RecordStatus.DRAFT
                || record.getDraftExpiresAt() == null
                || record.getDraftExpiresAt().isAfter(LocalDateTime.now(clock));
    }

    private int persistMemorySources(
            AgentSession session, AgentMessage assistant, List<MemoryFragment> injected) {
        if (agentMemorySourceMapper == null
                || session == null
                || assistant == null
                || assistant.getId() == null
                || assistant.getRole() != AgentMessageRole.ASSISTANT
                || injected == null
                || injected.isEmpty()) {
            return 0;
        }
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int sourceCount = 0;
        for (MemoryFragment fragment : injected) {
            if (fragment == null || fragment.recordId() == null) {
                continue;
            }
            AgentMemorySourceKind kind = sourceKindOf(session, fragment);
            String key = kind.name() + ":" + fragment.recordId();
            if (!seen.add(key)) {
                continue;
            }
            AgentMemorySource source = new AgentMemorySource();
            source.setUserId(session.getUserId());
            source.setSessionId(session.getId());
            source.setAssistantMessageId(assistant.getId());
            source.setSourceRecordId(fragment.recordId());
            source.setSourceKind(kind);
            source.setCreatedAt(assistant.getCreatedAt());
            if (agentMemorySourceMapper.insert(source) != 1) {
                throw new IllegalStateException("agent memory source persistence failed");
            }
            sourceCount++;
        }
        return sourceCount;
    }

    private AgentMemorySourceKind sourceKindOf(AgentSession session, MemoryFragment fragment) {
        if (session.getPurpose() == AgentSessionPurpose.REVIEW_CHAT
                && session.getRecordId() != null
                && session.getRecordId().equals(fragment.recordId())) {
            return AgentMemorySourceKind.REVIEW_TARGET;
        }
        return AgentMemorySourceKind.CROSS_RECORD;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 只记录结构化元数据，不记录对话原文。
     */
    private void logProviderIssue(
            String operation,
            AgentStage stage,
            long startedAt,
            AgentProviderFailureCategory category) {
        log.warn(
                "Agent provider issue: operation={} stage={} provider={} durationMs={} category={} transient={}",
                operation,
                stage,
                modelClient.provider(),
                Duration.ofNanos(System.nanoTime() - startedAt).toMillis(),
                category.wireId(),
                category.isTransient());
    }

    private void logProviderUnavailable(String operation, AgentStage stage) {
        AgentProviderFailureCategory category = AgentProviderFailureCategory.AUTH_CONFIGURATION;
        log.warn(
                "Agent provider issue: operation={} stage={} provider={} durationMs={} category={} transient={}",
                operation,
                stage,
                modelClient.provider(),
                0,
                category.wireId(),
                category.isTransient());
    }

    private void assertOwnershipWritable(Long userId) {
        if (dataOwnershipMutationGuard != null) dataOwnershipMutationGuard.assertWritable(userId);
    }

}
