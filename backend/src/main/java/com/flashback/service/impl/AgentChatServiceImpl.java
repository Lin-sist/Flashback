package com.flashback.service.impl;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentMockResponder;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.AgentStageDecision;
import com.flashback.agent.AgentStageMachine;
import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
import com.flashback.domain.Record;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.mapper.AgentMessageMapper;
import com.flashback.mapper.AgentSessionMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.service.AgentChatService;
import com.flashback.vo.AgentMessageVO;
import com.flashback.vo.AgentSessionVO;
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

/**
 * Agent 多轮对话编排实现（C1）。
 *
 * 关键约定：
 * 1. 阶段推进由 AgentStageMachine 决定，不交给模型（design.md 决策 4）；
 * 2. provider 不可用/失败时返回显式 UNAVAILABLE / FAILED，不做本地兜底冒充成功（决策 5）；
 * 3. provider 失败时用户消息已落库并保留，Agent 回复不落库，允许同轮重试；
 * 4. 日志只输出结构化元数据，绝不输出对话原文或日记原文；
 * 5. C1 不调用任何记录写操作，素材回填由前端在用户确认后走既有记录接口。
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

        AgentReply reply = generateReply(targetStage, history, draftExcerptOf(userId, session.getRecordId()), "turn");
        if (!reply.success()) {
            return failed(session, history, reply.message());
        }

        AgentMessage assistantMessage = persistMessage(
                session, AgentMessageRole.ASSISTANT, turnNo, targetStage, reply.content(), now);
        history = appended(history, assistantMessage);

        String materialDraft = null;
        if (targetStage == AgentStage.CLOSING) {
            materialDraft = generateMaterial(history);
            endSession(session, now);
        }

        return toSessionVO(session, history, materialDraft, STATUS_SUCCESS);
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
        if (modelClient.isMockProvider()) {
            String latestUserInput = latestUserContent(history);
            String mocked = guardrailPolicy.enforceReplyLength(mockResponder.reply(targetStage, latestUserInput));
            return AgentReply.ok(mocked);
        }

        long startedAt = System.nanoTime();
        try {
            String raw = modelClient.complete(
                    promptBuilder.buildConversationMessages(targetStage, history, draftExcerpt));
            String reply = modelClient.extractText(raw, "reply");
            if (reply == null) {
                logProviderIssue(operation, targetStage, startedAt, "invalid-content");
                return AgentReply.fail("AI返回内容无效");
            }
            return AgentReply.ok(guardrailPolicy.enforceReplyLength(reply));
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logProviderIssue(operation, targetStage, startedAt, ex.getClass().getSimpleName());
            return AgentReply.fail("AI服务暂时不可用");
        }
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

    private record AgentReply(boolean success, String content, String message) {

        static AgentReply ok(String content) {
            return new AgentReply(true, content, null);
        }

        static AgentReply fail(String message) {
            return new AgentReply(false, null, message);
        }
    }
}
