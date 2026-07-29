package com.flashback.agent.tool;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentRawToolCall;
import com.flashback.agent.guardrail.AgentLayeredCorpus;
import com.flashback.agent.guardrail.AgentSourceCorpus;
import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentToolCall;
import com.flashback.mapper.AgentToolCallMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具提议的落库、还原与确认编排（C2）。
 *
 * 存在理由：把工具生命周期从 AgentChatServiceImpl 中分离，
 * 使对话编排仍然只关心「阶段推进 + 回复生成」，工具语义集中在本类。
 *
 * 关键不变量：
 * - 提议落库为 PROPOSED，**不执行**；执行只在 confirm 路径发生（design 决策 2、9）；
 * - 单轮至多保留一个提议，其余记审计后丢弃（design 决策 10）；
 * - 重复确认幂等：依赖 updateStatusIfProposed 的条件更新，而非先查后写；
 * - 审计只存结构化摘要，不存参数原文（design 决策 6）。
 *
 * C4 增量：校验时传入来源集合（本会话用户原话），使内容忠实度成为提议合法性的一个维度。
 * 忠实度不通过的提议走既有 persistGuardRejected 通道落 REJECTED_BY_GUARD，
 * **本轮 Agent 回复照常返回**——沿用「校验失败不毁掉整轮对话」的既有语义。
 */
@Component
public class AgentToolCoordinator {

    private static final Logger log = LoggerFactory.getLogger(AgentToolCoordinator.class);

    private final AgentToolCallMapper agentToolCallMapper;
    private final AgentToolValidator validator;
    private final AgentToolExecutor executor;
    private final AgentModelClient modelClient;
    private final AgentGuardrailPolicy guardrailPolicy;
    private final Clock clock;

    public AgentToolCoordinator(
            AgentToolCallMapper agentToolCallMapper,
            AgentToolValidator validator,
            AgentToolExecutor executor,
            AgentModelClient modelClient,
            AgentGuardrailPolicy guardrailPolicy,
            Clock clock) {
        this.agentToolCallMapper = agentToolCallMapper;
        this.validator = validator;
        this.executor = executor;
        this.modelClient = modelClient;
        this.guardrailPolicy = guardrailPolicy;
        this.clock = clock;
    }

    /**
     * 处理某一轮返回的原始工具提议。
     *
     * 失败不影响本轮对话：校验不通过时只记审计并返回 null，
     * Agent 该说的话照说（design 数据流 2.1 要点一）。
     *
     * @return 落库后的待确认提议；无有效提议时为 null
     */
    public AgentToolCall handleProposals(
            AgentSession session,
            int turnNo,
            List<AgentRawToolCall> rawToolCalls,
            AgentSourceCorpus corpus) {
        // C4 签名保留：无记忆层是真实的常见运行状态（检索无命中 / 失败 / 开关关闭），
        // 此时行为与 C4 完全一致。
        return handleProposals(session, turnNo, rawToolCalls, AgentLayeredCorpus.sessionOnly(corpus));
    }

    /**
     * C3 重载：使用分层来源集合处理提议。
     */
    public AgentToolCall handleProposals(
            AgentSession session,
            int turnNo,
            List<AgentRawToolCall> rawToolCalls,
            AgentLayeredCorpus corpus) {

        if (rawToolCalls == null || rawToolCalls.isEmpty()) {
            return null;
        }
        boolean hasDraft = session.getRecordId() != null;
        AgentToolCall accepted = null;

        for (AgentRawToolCall raw : rawToolCalls) {
            if (accepted != null) {
                // 已有一个合法提议，其余丢弃但留痕，便于观察模型是否倾向批量提议。
                persistGuardRejected(session, turnNo, raw.name(), AgentToolValidationResult.REASON_SUPERSEDED);
                continue;
            }
            AgentToolRawArguments args = parseArguments(raw);
            AgentToolValidationResult result = validator.validate(raw.name(), args, hasDraft, corpus);
            if (!result.isAccepted()) {
                persistGuardRejected(session, turnNo, raw.name(), result.rejectReason());
                continue;
            }
            accepted = persistProposed(session, turnNo, result.proposal());
        }
        return accepted;
    }

    /**
     * 确认（接受或拒绝）一条提议。
     *
     * @param decision ACCEPT 或 REJECT
     * @return 处理后的提议记录；已终结时原样返回（幂等）
     */
    public AgentToolCall confirm(Long userId, Long sessionId, Long toolCallId, AgentToolDecision decision) {
        AgentToolCall toolCall = agentToolCallMapper.selectByIdAndUserId(toolCallId, userId);
        if (toolCall == null || !toolCall.getSessionId().equals(sessionId)) {
            // 跨用户、不存在、会话不匹配统一按未找到处理，不泄露存在性。
            return null;
        }
        if (toolCall.getStatus() != AgentToolCallStatus.PROPOSED) {
            // 幂等：已执行 / 已失败 / 已拒绝的提议不再处理，原样返回当前状态。
            return toolCall;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (decision == AgentToolDecision.REJECT) {
            return applyOutcome(toolCall, AgentToolOutcome.rejected("好的，那就不动它"), now);
        }

        AgentToolProposal proposal = restoreProposal(toolCall);
        AgentToolOutcome outcome = executor.execute(toolCall, proposal);
        return applyOutcome(toolCall, outcome, now);
    }

    /**
     * 当前待确认提议；无则为 null。
     */
    public AgentToolCall pendingOf(Long sessionId) {
        return agentToolCallMapper.selectPendingBySessionId(sessionId);
    }

    /**
     * 最近若干条已终结的工具调用，供回注对话上下文。
     */
    public List<AgentToolCall> recentSettled(Long sessionId, int limit) {
        List<AgentToolCall> rows = agentToolCallMapper.selectRecentSettledBySessionId(sessionId, limit);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        // mapper 按 id DESC 返回，这里正序化便于拼接时间线摘要。
        List<AgentToolCall> ordered = new ArrayList<>(rows);
        ordered.sort((left, right) -> Long.compare(left.getId(), right.getId()));
        return List.copyOf(ordered);
    }

    // ---------- 内部实现 ----------

    private AgentToolCall applyOutcome(AgentToolCall toolCall, AgentToolOutcome outcome, LocalDateTime now) {
        int affected = agentToolCallMapper.updateStatusIfProposed(
                toolCall.getId(),
                toolCall.getUserId(),
                outcome.status().name(),
                outcome.failureType(),
                outcome.message(),
                now);
        if (affected == 0) {
            // 并发下已被其他请求处理：重新读取当前状态，绝不重复执行。
            AgentToolCall current = agentToolCallMapper.selectByIdAndUserId(toolCall.getId(), toolCall.getUserId());
            return current == null ? toolCall : current;
        }
        toolCall.setStatus(outcome.status());
        toolCall.setFailureType(outcome.failureType());
        toolCall.setResultSummary(outcome.message());
        toolCall.setUpdatedAt(now);
        return toolCall;
    }

    private AgentToolCall persistProposed(AgentSession session, int turnNo, AgentToolProposal proposal) {
        LocalDateTime now = LocalDateTime.now(clock);
        AgentToolCall toolCall = baseToolCall(session, turnNo, proposal.tool().wireName(), now);
        toolCall.setStatus(AgentToolCallStatus.PROPOSED);
        toolCall.setArgsDigest(AgentToolArgsDigest.of(proposal));
        // 瞬态执行参数：确认时需要原始入参，终结后即被 SQL 清空。
        toolCall.setPendingArgs(AgentToolPendingArgs.serialize(proposal));
        toolCall.setAskText(guardrailPolicy.enforceReplyLength(proposal.askText()));
        agentToolCallMapper.insert(toolCall);
        logToolCall("proposed", toolCall, null);
        return toolCall;
    }

    private void persistGuardRejected(AgentSession session, int turnNo, String wireName, String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        // 工具名可能是模型幻觉出的任意字符串，落库前截断以适配列宽。
        AgentToolCall toolCall = baseToolCall(session, turnNo, truncate(wireName, 50), now);
        toolCall.setStatus(AgentToolCallStatus.REJECTED_BY_GUARD);
        toolCall.setArgsDigest(AgentToolArgsDigest.ofRejected(truncate(wireName, 50), reason));
        toolCall.setFailureType(reason);
        agentToolCallMapper.insert(toolCall);
        logToolCall("rejected-by-guard", toolCall, reason);
    }

    private AgentToolCall baseToolCall(AgentSession session, int turnNo, String toolName, LocalDateTime now) {
        AgentToolCall toolCall = new AgentToolCall();
        toolCall.setSessionId(session.getId());
        toolCall.setUserId(session.getUserId());
        toolCall.setRecordId(session.getRecordId());
        toolCall.setTurnNo(turnNo);
        toolCall.setToolName(toolName);
        toolCall.setCreatedAt(now);
        toolCall.setUpdatedAt(now);
        return toolCall;
    }

    private AgentToolRawArguments parseArguments(AgentRawToolCall raw) {
        String arguments = raw.arguments();
        return new AgentToolRawArguments(
                modelClient.readArgumentText(arguments, AgentToolRegistry.PARAM_ASK_TEXT),
                modelClient.readArgumentText(arguments, AgentToolRegistry.PARAM_TEXT),
                modelClient.readArgumentLongArray(arguments, AgentToolRegistry.PARAM_TAG_IDS),
                modelClient.readArgumentText(arguments, AgentToolRegistry.PARAM_UNLOCK_AT));
    }

    /**
     * 从落库记录还原执行所需参数。
     *
     * 参数取自 pendingArgs（瞬态缓冲）而非 argsDigest——后者按决策 6 只存不可还原的摘要。
     * 也不接受前端在 confirm 时回传参数：那等于让客户端绕过白名单与校验。
     */
    private AgentToolProposal restoreProposal(AgentToolCall toolCall) {
        AgentToolName tool = AgentToolName.fromWireName(toolCall.getToolName());
        if (tool == null) {
            return null;
        }
        return AgentToolPendingArgs.deserialize(tool, toolCall.getAskText(), toolCall.getPendingArgs());
    }

    private String truncate(String value, int limit) {
        if (value == null) {
            return "unknown";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "unknown";
        }
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit);
    }

    /**
     * 结构化日志：不含参数原文与对话原文。
     */
    private void logToolCall(String event, AgentToolCall toolCall, String reason) {
        log.info("agent tool {} sessionId={} toolCallId={} tool={} turnNo={} reason={}",
                event,
                toolCall.getSessionId(),
                toolCall.getId(),
                toolCall.getToolName(),
                toolCall.getTurnNo(),
                reason == null ? "none" : reason);
    }
}
