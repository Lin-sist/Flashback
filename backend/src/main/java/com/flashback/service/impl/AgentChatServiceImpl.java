package com.flashback.service.impl;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentMockResponder;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentModelResponse;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.AgentRawToolCall;
import com.flashback.agent.AgentStageDecision;
import com.flashback.agent.AgentStageMachine;
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
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
import com.flashback.domain.AgentToolCall;
import com.flashback.domain.Record;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.mapper.AgentMessageMapper;
import com.flashback.mapper.AgentSessionMapper;
import com.flashback.mapper.RecordMapper;
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
        this.tagService = tagService;
        this.appAgentProperties = appAgentProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AgentSessionVO startOrResume(Long userId, AgentSessionStartRequest request) {
        Long recordId = request == null ? null : request.getRecordId();
        // 校验归属与草稿状态；返回值本身不参与后续编排。
        requireOwnedRecordIfPresent(userId, recordId);

        AgentSession existing = agentSessionMapper.selectActiveByUserAndRecord(userId, recordId);
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
            AgentStage openingStage = stageMachine.firstStage();
            AgentReply openingReply = generateReply(openingStage, List.of(), null, "opening-retry");
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
        session.setStage(AgentStage.OPENING);
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

        AgentStage openingStage = stageMachine.firstStage();
        AgentReply reply = generateReply(openingStage, List.of(), null, "opening");
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
        } else {
            turnNo = session.getTurnCount() + 1;
            AgentStageDecision decision = stageMachine.decide(
                    session.getStage(),
                    content,
                    session.getStageReaskCount(),
                    turnNo,
                    appAgentProperties.getMaxTurnsPerSession());

            // 用户的话先落库：即使随后 provider 失败，用户输入也不丢。
            AgentMessage userMessage = persistMessage(
                    session, AgentMessageRole.USER, turnNo, session.getStage(), content, now);
            history = appended(history, userMessage);
            session.setTurnCount(turnNo);
            session.setStageReaskCount(decision.stageReaskCount());
            session.setStage(decision.nextStage() == AgentStage.ENDED ? AgentStage.CLOSING : decision.nextStage());
            updateProgress(session, now);
            targetStage = session.getStage();
        }

        String unavailableReason = modelClient.unavailableReason();
        if (unavailableReason != null) {
            return unavailable(session, history, unavailableReason);
        }

        AgentToolContext toolContext = buildToolContext(session);
        AgentReply reply = generateReply(
                targetStage, history, draftExcerptOf(userId, session.getRecordId()), "turn", toolContext);
        if (!reply.success()) {
            return failed(session, history, reply.message());
        }

        AgentMessage assistantMessage = persistMessage(
                session, AgentMessageRole.ASSISTANT, turnNo, targetStage, reply.content(), now);
        history = appended(history, assistantMessage);

        // 提议只落库为待确认，**不执行**；执行只发生在 confirmToolCall（决策 2、9）。
        AgentToolCall pendingToolCall = toolContext.toolsEnabled()
                ? toolCoordinator.handleProposals(session, turnNo, reply.toolCalls())
                : null;

        String materialDraft = null;
        if (targetStage == AgentStage.CLOSING) {
            materialDraft = generateMaterial(history);
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

        if (session.getStatus() == AgentSessionStatus.ENDED) {
            return toSessionVO(session, history, generateMaterial(history), statusOfConfig());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String materialDraft = generateMaterial(history);
        endSession(session, now);
        return toSessionVO(session, history, materialDraft, statusOfConfig());
    }

    // ---------- 内部实现 ----------

    private AgentReply generateReply(
            AgentStage targetStage,
            List<AgentMessage> history,
            String draftExcerpt,
            String operation) {
        return generateReply(targetStage, history, draftExcerpt, operation, null);
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
            AgentToolContext toolContext) {
        if (modelClient.isMockProvider()) {
            String latestUserInput = latestUserContent(history);
            String mocked = guardrailPolicy.enforceReplyLength(mockResponder.reply(targetStage, latestUserInput));
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
                    toolContext == null ? null : toolContext.supplement());

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
    private AgentToolContext buildToolContext(AgentSession session) {
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
     * 素材整理失败不影响会话结束：素材为可选产物，缺失时前端不展示回填入口。
     */
    private String generateMaterial(List<AgentMessage> history) {
        if (modelClient.isMockProvider()) {
            return mockResponder.material(history);
        }
        if (modelClient.unavailableReason() != null) {
            return null;
        }
        long startedAt = System.nanoTime();
        try {
            String raw = modelClient.complete(promptBuilder.buildMaterialMessages(history));
            return modelClient.extractText(raw, "material");
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logProviderIssue("material", AgentStage.CLOSING, startedAt, ex.getClass().getSimpleName());
            return null;
        }
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

    private Record requireOwnedRecordIfPresent(Long userId, Long recordId) {
        if (recordId == null) {
            return null;
        }
        Record record = recordMapper.selectByIdAndUserId(recordId, userId);
        if (record == null) {
            throw new NotFoundException("记录不存在");
        }
        if (record.getStatus() != com.flashback.domain.RecordStatus.DRAFT) {
            throw new BizException(
                    ErrorCode.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "只有草稿记录可以开启写作对话");
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
    }
}
