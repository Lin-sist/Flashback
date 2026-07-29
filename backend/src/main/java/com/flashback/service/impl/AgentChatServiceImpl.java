package com.flashback.service.impl;

import com.flashback.agent.AgentChatMode;
import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentMockResponder;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentModelResponse;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.AgentRawToolCall;
import com.flashback.agent.AgentStageDecision;
import com.flashback.agent.AgentStageMachine;
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
import com.flashback.agent.tool.AgentToolCallStatus;
import com.flashback.agent.tool.AgentToolCoordinator;
import com.flashback.agent.tool.AgentToolDecision;
import com.flashback.agent.tool.AgentToolName;
import com.flashback.agent.tool.AgentToolPendingArgs;
import com.flashback.agent.tool.AgentToolProposal;
import com.flashback.agent.tool.AgentToolRegistry;
import com.flashback.agent.tool.AgentToolSchemaFactory;
import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
import com.flashback.domain.AgentToolCall;
import com.flashback.domain.Record;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.mapper.AgentMessageMapper;
import com.flashback.mapper.AgentSessionMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.RecordTagMapper;
import com.flashback.service.AgentChatService;
import com.flashback.service.TagService;
import com.flashback.vo.AgentMessageVO;
import com.flashback.vo.AgentSessionVO;
import com.flashback.vo.AgentToolCallVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final MemoryPort memoryPort;
    private final MemoryCueExtractor memoryCueExtractor;
    private final RecordTagMapper recordTagMapper;
    private final TagService tagService;
    private final AppAgentProperties appAgentProperties;
    private final Clock clock;

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
            MemoryPort memoryPort,
            MemoryCueExtractor memoryCueExtractor,
            RecordTagMapper recordTagMapper,
            TagService tagService,
            AppAgentProperties appAgentProperties,
            Clock clock) {
        this.agentSessionMapper = agentSessionMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.recordMapper = recordMapper;
        this.stageMachine = stageMachine;
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
        this.memoryPort = memoryPort;
        this.memoryCueExtractor = memoryCueExtractor;
        this.recordTagMapper = recordTagMapper;
        this.tagService = tagService;
        this.appAgentProperties = appAgentProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AgentSessionVO startOrResume(Long userId, AgentSessionStartRequest request) {
        Long recordId = request == null ? null : request.getRecordId();
        // C3b：模式在最开始定一次，之后编排只问模式、不问 purpose（design 决策 1）。
        AgentSessionPurpose purpose = request == null
                ? AgentSessionPurpose.WRITING_GUIDANCE
                : request.purposeOrDefault();
        AgentChatMode mode = AgentChatMode.of(purpose);

        if (mode.requiresRecord() && recordId == null) {
            // 回看没有可回看的对象。
            throw new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "回看对话需要指定记录");
        }
        // 校验归属与记录状态；返回值本身不参与后续编排。
        requireOwnedRecordIfPresent(userId, recordId, mode);

        AgentSession existing = agentSessionMapper.selectActiveByUserAndRecord(userId, recordId, purpose);
        if (existing != null) {
            List<AgentMessage> existingMessages = agentMessageMapper.selectBySessionId(existing.getId());
            // 正常中断恢复：若最后一条用户消息尚无同轮 Agent 回复，显式提示重试，
            // 不把 provider 失败后的半轮会话误报为 SUCCESS。
            if (!existingMessages.isEmpty()) {
                AgentSessionVO resumed = toSessionVO(existing, existingMessages, null, statusOfConfig());
                if (findPendingUserMessage(existingMessages, existing.getTurnCount()) != null) {
                    resumed.setStatus(STATUS_FAILED);
                    resumed.setMessage("上一轮回复尚未完成，请重试");
                }
                return resumed;
            }
            // 会话已创建但开场 provider 调用失败：再次触发时只重试开场，
            // 不创建第二个 ACTIVE 会话。
            String unavailableReason = modelClient.unavailableReason();
            if (unavailableReason != null) {
                return unavailable(existing, existingMessages, unavailableReason);
            }
            LocalDateTime retryAt = LocalDateTime.now(clock);
            AgentStage openingStage = openingStageOf(mode);
            AgentReply openingReply = generateReply(
                    openingStage, List.of(), reviewFragmentsOf(userId, existing, mode), "opening-retry", mode);
            if (!openingReply.success()) {
                return failed(existing, existingMessages, openingReply.message());
            }
            AgentMessage openingMessage = persistMessage(
                    existing, AgentMessageRole.ASSISTANT, 0, openingStage, openingReply.content(), retryAt);
            existing.setStage(openingStage);
            updateProgress(existing, retryAt);
            return toSessionVO(existing, List.of(openingMessage), null, STATUS_SUCCESS);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AgentSession session = new AgentSession();
        session.setUserId(userId);
        session.setRecordId(recordId);
        session.setPurpose(purpose);
        // 回看恒为 REVIEW（不经阶段机）；写作引导从 OPENING 起。
        session.setStage(mode.isStageMachineDriven() ? AgentStage.OPENING : AgentStage.REVIEW);
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
            return unavailable(session, List.of(), unavailableReason);
        }

        AgentStage openingStage = openingStageOf(mode);
        AgentReply reply = generateReply(
                openingStage, List.of(), reviewFragmentsOf(userId, session, mode), "opening", mode);
        if (!reply.success()) {
            return failed(session, List.of(), reply.message());
        }

        AgentMessage assistantMessage = persistMessage(
                session, AgentMessageRole.ASSISTANT, 0, openingStage, reply.content(), now);
        session.setStage(openingStage);
        updateProgress(session, now);

        return toSessionVO(session, List.of(assistantMessage), null, STATUS_SUCCESS);
    }

    @Override
    public AgentSessionVO getSession(Long userId, Long sessionId) {
        AgentSession session = requireOwnedSession(userId, sessionId);
        List<AgentMessage> messages = agentMessageMapper.selectBySessionId(sessionId);
        AgentSessionVO vo = toSessionVO(session, messages, null, statusOfConfig());
        if (findPendingUserMessage(messages, session.getTurnCount()) != null) {
            vo.setStatus(STATUS_FAILED);
            vo.setMessage("上一轮回复尚未完成，请重试");
        }
        return vo;
    }

    @Override
    @Transactional
    public AgentSessionVO sendMessage(Long userId, Long sessionId, AgentMessageRequest request) {
        AgentSession session = requireOwnedSession(userId, sessionId);
        requireActive(session);

        String content = normalizeUserInput(request == null ? null : request.getContent());
        LocalDateTime now = LocalDateTime.now(clock);
        List<AgentMessage> history = agentMessageMapper.selectBySessionId(sessionId);

        AgentChatMode mode = AgentChatMode.of(session.getPurpose());
        int maxTurns = maxTurnsOf(mode);

        AgentMessage pendingUserMessage = findPendingUserMessage(history, session.getTurnCount());
        int turnNo;
        AgentStage targetStage;
        if (pendingUserMessage != null) {
            // provider 失败后的同轮重试：用户消息已经落库，不重复 insert，也不再次推进状态机。
            if (!pendingUserMessage.getContent().equals(content)) {
                throw new BizException(
                        ErrorCode.BAD_REQUEST,
                        HttpStatus.BAD_REQUEST,
                        "上一轮回复失败，请先重试原消息");
            }
            turnNo = pendingUserMessage.getTurnNo();
            targetStage = session.getStage();
        } else if (mode.isStageMachineDriven()) {
            turnNo = session.getTurnCount() + 1;
            AgentStageDecision decision = stageMachine.decide(
                    session.getStage(),
                    content,
                    session.getStageReaskCount(),
                    turnNo,
                    maxTurns);

            // 用户的话先落库：即使随后 provider 失败，用户输入也不丢。
            AgentMessage userMessage = persistMessage(
                    session, AgentMessageRole.USER, turnNo, session.getStage(), content, now);
            history = appended(history, userMessage);
            session.setTurnCount(turnNo);
            session.setStageReaskCount(decision.stageReaskCount());
            session.setStage(decision.nextStage() == AgentStage.ENDED ? AgentStage.CLOSING : decision.nextStage());
            updateProgress(session, now);
            targetStage = session.getStage();
        } else {
            // C3b：回看无阶段机（design 决策 2）。阶段恒为 REVIEW，
            // stageReaskCount 不被回看逻辑改写——回看不追问，没有「同阶段再问一次」的概念。
            turnNo = session.getTurnCount() + 1;
            AgentMessage userMessage = persistMessage(
                    session, AgentMessageRole.USER, turnNo, AgentStage.REVIEW, content, now);
            history = appended(history, userMessage);
            session.setTurnCount(turnNo);
            session.setStage(AgentStage.REVIEW);
            updateProgress(session, now);
            targetStage = AgentStage.REVIEW;
        }

        String unavailableReason = modelClient.unavailableReason();
        if (unavailableReason != null) {
            return unavailable(session, history, unavailableReason);
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
        List<MemoryFragment> injectedMemory = mergedMemoryOf(
                reviewFragmentsOf(userId, session, mode), retrieveMemory(session, history));
        // C4 + C3：来源集合在生成回复前构造一次，回复检查与工具提议校验共用同一份，
        // 保证「Agent 说的话」与「要写进正文的文字」判定基准一致。
        AgentLayeredCorpus corpus = layeredCorpusOf(history, injectedMemory);
        AgentReply reply = generateReply(
                targetStage,
                history,
                // 回看不注入「草稿正文只读引用」——记录内容已在 MEMORY 层，
                // 且那段 prompt 的措辞（用户已经写下的正文）对已解锁记录是错的。
                mode.isStageMachineDriven() ? draftExcerptOf(userId, session.getRecordId()) : null,
                "turn",
                toolContext,
                corpus,
                injectedMemory);
        if (!reply.success()) {
            return failed(session, history, reply.message());
        }

        AgentMessage assistantMessage = persistMessage(
                session, AgentMessageRole.ASSISTANT, turnNo, targetStage, reply.content(), now);
        history = appended(history, assistantMessage);

        // 提议只落库为待确认，**不执行**；执行只发生在 confirmToolCall（C2 决策 2、9）。
        AgentToolCall pendingToolCall = null;
        if (toolContext.toolsEnabled()) {
            pendingToolCall = toolCoordinator.handleProposals(session, turnNo, reply.toolCalls(), corpus);
        } else if (reply.hasToolCalls()) {
            // C3b：回看不挂 tools，但模型仍可能自作主张返回提议。fail-closed——
            // 丢弃并留结构化审计，不落待确认记录、不下发确认条，本轮回复照常返回。
            log.warn("agent tool call discarded fail-closed sessionId={} turnNo={} mode={} count={}",
                    session.getId(), turnNo, mode, reply.toolCalls().size());
        }

        String materialDraft = null;
        // C3b：素材是否产出由模式决定（design 决策 8）。往已解锁记录的正文里追加
        // 此刻的整理会破坏它的时间完整性——用户几个月后无法分辨哪句是当时写的。
        if (mode.isMaterialProduced() && targetStage == AgentStage.CLOSING) {
            materialDraft = generateMaterial(history);
            endSession(session, now);
        } else if (!mode.isStageMachineDriven() && session.getTurnCount() >= maxTurns) {
            // 回看没有 CLOSING 阶段，达到轮次上限即温和收束（design 决策 2）。
            endSession(session, now);
        }

        AgentSessionVO vo = toSessionVO(session, history, materialDraft, STATUS_SUCCESS);
        vo.setPendingToolCall(toToolCallVO(pendingToolCall));
        return vo;
    }

    @Override
    @Transactional
    public AgentSessionVO confirmToolCall(
            Long userId, Long sessionId, Long toolCallId, AgentToolDecision decision) {
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
        AgentSession session = requireOwnedSession(userId, sessionId);
        List<AgentMessage> history = agentMessageMapper.selectBySessionId(sessionId);
        AgentChatMode mode = AgentChatMode.of(session.getPurpose());

        // C3b：回看主动结束不产素材（design 决策 8），也不调 provider。
        if (session.getStatus() == AgentSessionStatus.ENDED) {
            return toSessionVO(
                    session, history, mode.isMaterialProduced() ? generateMaterial(history) : null,
                    statusOfConfig());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String materialDraft = mode.isMaterialProduced() ? generateMaterial(history) : null;
        endSession(session, now);
        return toSessionVO(session, history, materialDraft, statusOfConfig());
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
            List<MemoryFragment> reviewFragments,
            String operation,
            AgentChatMode mode) {
        List<MemoryFragment> fragments = reviewFragments == null ? List.of() : reviewFragments;
        return generateReply(
                targetStage, history, null, operation,
                new AgentToolContext(false, null),
                layeredCorpusOf(history, fragments), fragments);
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
    private List<MemoryFragment> retrieveMemory(AgentSession session, List<AgentMessage> history) {
        AppAgentProperties.Memory config = appAgentProperties.getMemory();
        if (!config.isEnabled()) {
            // 开关关闭必须留痕，不静默表现为检索无命中（backend-core delta 要求）。
            log.info("agent memory retrieval disabled by config sessionId={}", session.getId());
            return List.of();
        }
        try {
            List<String> keywords = memoryCueExtractor.extractKeywords(history);
            List<Long> tagIds = memoryTagIdsOf(session);
            MemoryQuery query = new MemoryQuery(
                    session.getUserId(),
                    session.getPurpose(),
                    keywords,
                    tagIds,
                    session.getRecordId(),
                    config.getMaxFragments());
            if (!query.hasCue()) {
                return List.of();
            }
            return memoryPort.retrieve(query);
        } catch (RuntimeException ex) {
            // 只记异常类型，不记检索线索或片段内容。
            log.warn("agent memory retrieval failed sessionId={} cause={}",
                    session.getId(), ex.getClass().getSimpleName());
            return List.of();
        }
    }

    /**
     * C3b：模式对应的开场阶段。
     */
    private AgentStage openingStageOf(AgentChatMode mode) {
        return mode.isStageMachineDriven() ? stageMachine.firstStage() : AgentStage.REVIEW;
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
        addReviewFragment(fragments, record.getId(), occurredAt, timeLabel, record.getContent(), limit);
        addReviewFragment(fragments, record.getId(), occurredAt, timeLabel, record.getAiSummary(), limit);
        addReviewFragment(fragments, record.getId(), occurredAt, timeLabel, record.getBeliefThen(), limit);
        return List.copyOf(fragments);
    }

    private void addReviewFragment(
            List<MemoryFragment> sink,
            Long recordId,
            LocalDateTime occurredAt,
            String timeLabel,
            String text,
            int limit) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalized = text.trim();
        String clipped = normalized.length() <= limit ? normalized : normalized.substring(0, limit);
        sink.add(new MemoryFragment(recordId, occurredAt, timeLabel, clipped));
    }

    /**
     * C3b：合并被回看记录的片段与检索到的历史片段。
     *
     * 两者都进 MEMORY 层，因为都是「过去的表达」。合并后的列表既用于注入 prompt，
     * 也用于构造来源集合——**必须是同一份列表**（C3a 不变量 1）。
     */
    private List<MemoryFragment> mergedMemoryOf(
            List<MemoryFragment> reviewFragments, List<MemoryFragment> retrieved) {
        if (reviewFragments.isEmpty()) {
            return retrieved;
        }
        List<MemoryFragment> merged = new ArrayList<>(reviewFragments);
        merged.addAll(retrieved);
        return List.copyOf(merged);
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
            List<AgentMessage> history,
            String draftExcerpt,
            String operation,
            AgentToolContext toolContext,
            AgentLayeredCorpus corpus,
            List<MemoryFragment> injectedMemory) {
        if (modelClient.isMockProvider()) {
            String latestUserInput = latestUserContent(history);
            String mocked = guardrailPolicy.enforceReplyLength(
                    applyReplyGuardrail(mockResponder.reply(targetStage, latestUserInput), corpus, operation));
            List<AgentRawToolCall> mockToolCalls = toolContext == null
                    ? List.of()
                    : mockResponder.toolCalls(targetStage, latestUserInput, toolContext.toolsEnabled());
            return AgentReply.ok(mocked, mockToolCalls);
        }

        long startedAt = System.nanoTime();
        try {
            boolean toolsEnabled = toolContext != null && toolContext.toolsEnabled();
            List<Map<String, Object>> tools = toolsEnabled
                    ? toolSchemaFactory.buildTools(modelClient.useStrictMode())
                    : List.of();
            List<Map<String, String>> messages = promptBuilder.buildConversationMessages(
                    targetStage,
                    history,
                    draftExcerpt,
                    toolContext == null ? null : toolContext.supplement(),
                    promptBuilder.buildMemorySupplement(injectedMemory));

            AgentModelResponse response = modelClient.completeWithTools(
                    messages, tools, toolsEnabled && modelClient.useStrictMode());

            String reply = response.content();
            if (reply == null && response.hasToolCalls()) {
                // 只给了提议没给话：用提议自带的 askText 兜底。
                reply = modelClient.readArgumentText(
                        response.firstToolCall().arguments(), AgentToolRegistry.PARAM_ASK_TEXT);
            }
            if (reply == null) {
                logProviderIssue(operation, targetStage, startedAt, "invalid-content");
                return AgentReply.fail("AI返回内容无效");
            }
            // 形状兜底：模型偶尔仍会把回复包成 JSON，剥壳后再裁剪长度，
            // 避免 {"reply":"..."} 原文进入对话气泡。
            reply = promptBuilder.normalizeReplyShape(reply);
            // C4：后置内容检查在形状兜底之后、长度裁剪之前。
            // 长度硬上限在多层叠加后仍然生效（agent-runtime delta 要求）。
            reply = applyReplyGuardrail(reply, corpus, operation);
            return AgentReply.ok(guardrailPolicy.enforceReplyLength(reply), response.toolCalls());
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logProviderIssue(operation, targetStage, startedAt, ex.getClass().getSimpleName());
            return AgentReply.fail("AI服务暂时不可用");
        }
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

    /**
     * C4：回复路径的后置内容检查与降级。
     *
     * 回复**不进忠实度闸**——Agent 提问本来就是它自己的话，
     * 用「必须源自用户原话」判定会把一切提问判死。
     * 回复受诊断 / 代决检查约束；命中则替换为安全兜底回复（design 决策 3），
     * 因为用户提交了一轮消息必须得到回应，拒绝或丢弃会让对话看起来挂了。
     */
    private String applyReplyGuardrail(String reply, AgentLayeredCorpus corpus, String operation) {
        if (reply == null) {
            return null;
        }
        // 内容检查用合并层：Agent 可以合法地提起过去的事，
        // 若只用会话层，「我记得你三月份也写过」里的复述部分会被当成 Agent 新增表述，
        // 诊断 / 代决的分区判定会因此失准。
        AgentGuardrailVerdict verdict = contentChecker.check(reply, corpus.combined());
        if (!verdict.isPassed()) {
            guardrailDowngrade.trace("reply:" + operation, null, null, verdict);
            return guardrailDowngrade.safeFallbackReply();
        }
        // C3：复述记忆内容必须带时间归属，否则三个月前的心情会被读成此刻的心情。
        AgentGuardrailVerdict attribution = timeAttributionChecker.check(reply, corpus);
        if (!attribution.isPassed()) {
            guardrailDowngrade.trace("reply-attribution:" + operation, null, null, attribution);
            return guardrailDowngrade.safeFallbackReply();
        }
        return reply;
    }

    /**
     * 素材整理失败不影响会话结束：素材为可选产物，缺失时前端不展示回填入口。
     *
     * C4：素材同为「模型产出且会进入用户正文」的文本，与工具正文参数结构完全同质，
     * 因此走同一道忠实度闸（design 决策 6）。不忠实的素材等同于「没生成出来」——
     * 复用既有的可选产物语义，前端零改动。
     */
    private String generateMaterial(List<AgentMessage> history) {
        AgentSourceCorpus corpus = corpusOf(history);
        if (modelClient.isMockProvider()) {
            return applyMaterialGuardrail(mockResponder.material(history), corpus);
        }
        if (modelClient.unavailableReason() != null) {
            return null;
        }
        long startedAt = System.nanoTime();
        try {
            String raw = modelClient.complete(promptBuilder.buildMaterialMessages(history));
            return applyMaterialGuardrail(modelClient.extractText(raw, "material"), corpus);
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logProviderIssue("material", AgentStage.CLOSING, startedAt, ex.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * C4：素材路径的忠实度闸与降级。不忠实即丢弃，用户看不到回填入口。
     */
    private String applyMaterialGuardrail(String material, AgentSourceCorpus corpus) {
        if (material == null || material.isBlank()) {
            return material;
        }
        AgentGuardrailVerdict faithfulness = faithfulnessChecker.check(material, corpus);
        if (!faithfulness.isPassed()) {
            guardrailDowngrade.trace("material", null, null, faithfulness);
            return null;
        }
        AgentGuardrailVerdict content = contentChecker.check(material, corpus);
        if (!content.isPassed()) {
            guardrailDowngrade.trace("material", null, null, content);
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
        if (record.getStatus() != mode.requiredRecordStatus()) {
            throw new BizException(
                    ErrorCode.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    mode.isStageMachineDriven()
                            ? "只有草稿记录可以开启写作对话"
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
        vo.setStage(session.getStage() == null ? null : session.getStage().name());
        vo.setSessionStatus(session.getStatus() == null ? null : session.getStatus().name());
        vo.setTurnCount(session.getTurnCount());
        vo.setMaxTurns(appAgentProperties.getMaxTurnsPerSession());
        vo.setCanContinue(session.getStatus() == AgentSessionStatus.ACTIVE
                && session.getTurnCount() < appAgentProperties.getMaxTurnsPerSession());
        vo.setMessages(toMessageVOs(messages));
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

    private List<AgentMessageVO> toMessageVOs(List<AgentMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<AgentMessageVO> result = new ArrayList<>(messages.size());
        for (AgentMessage message : messages) {
            AgentMessageVO vo = new AgentMessageVO();
            vo.setId(message.getId());
            vo.setRole(message.getRole() == null ? null : message.getRole().name());
            vo.setTurnNo(message.getTurnNo());
            vo.setStage(message.getStage() == null ? null : message.getStage().name());
            vo.setContent(message.getContent());
            vo.setCreatedAt(message.getCreatedAt());
            result.add(vo);
        }
        return result;
    }

    /**
     * 只记录结构化元数据，不记录对话原文。
     */
    private void logProviderIssue(String operation, AgentStage stage, long startedAt, String cause) {
        log.warn(
                "Agent provider issue: operation={} stage={} provider={} durationMs={} cause={}",
                operation,
                stage,
                modelClient.provider(),
                Duration.ofNanos(System.nanoTime() - startedAt).toMillis(),
                cause);
    }

    private record AgentReply(
            boolean success,
            String content,
            String message,
            List<AgentRawToolCall> toolCalls) {

        AgentReply {
            toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        }

        static AgentReply ok(String content) {
            return new AgentReply(true, content, null, List.of());
        }

        static AgentReply ok(String content, List<AgentRawToolCall> toolCalls) {
            return new AgentReply(true, content, null, toolCalls);
        }

        static AgentReply fail(String message) {
            return new AgentReply(false, null, message, List.of());
        }

        /** C3b：用于回看路径的 fail-closed 留痕——模型在无工具模式下仍返回了提议。 */
        boolean hasToolCalls() {
            return !toolCalls.isEmpty();
        }
    }
}
