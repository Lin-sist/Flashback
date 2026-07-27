package com.flashback.agent.tool;

import com.flashback.common.exception.BizException;
import com.flashback.domain.AgentToolCall;
import com.flashback.service.RecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具执行分发（C2）。
 *
 * 边界（design.md 关键不变量 3、决策 5）：
 * - 一切写操作都经 RecordService，因此 requireOwnedRecord + ensureDraft 天然生效；
 * - **不存在**仅 Agent 可用的旁路 SQL——绕过 service 就等于绕过封存不可变约束；
 * - 失败必须显式：业务校验失败转为 FAILED + 可读原因，绝不谎报成功。
 *
 * 日志约定：只输出结构化元数据，不输出参数原文或对话原文。
 */
@Component
public class AgentToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentToolExecutor.class);

    private final RecordService recordService;
    private final AgentToolValidator validator;

    public AgentToolExecutor(RecordService recordService, AgentToolValidator validator) {
        this.recordService = recordService;
        this.validator = validator;
    }

    /**
     * 执行一条已确认的工具调用。
     *
     * @param toolCall 待执行的提议记录（状态已由调用方确认为 PROPOSED）
     * @param proposal 从 toolCall 还原出的提议参数
     */
    public AgentToolOutcome execute(AgentToolCall toolCall, AgentToolProposal proposal) {
        if (toolCall == null || proposal == null) {
            return AgentToolOutcome.failed(
                    AgentToolOutcome.FAILURE_PRECONDITION,
                    "这一步已经失效了，可以让我重新提一次");
        }
        Long recordId = toolCall.getRecordId();
        if (recordId == null) {
            return AgentToolOutcome.failed(
                    AgentToolOutcome.FAILURE_PRECONDITION,
                    "这条对话还没有对应的记录草稿");
        }

        long startedAt = System.nanoTime();
        try {
            AgentToolOutcome outcome = dispatch(toolCall.getUserId(), recordId, proposal);
            logOutcome(toolCall, outcome, startedAt);
            return outcome;
        } catch (BizException ex) {
            // 业务校验拒绝（记录已封存、标签不存在、时间非法等）：显式失败，记录未变更。
            // NotFoundException 继承 BizException，跨用户访问也走这一支。
            AgentToolOutcome outcome = AgentToolOutcome.failed(
                    AgentToolOutcome.FAILURE_BUSINESS_REJECTED,
                    readableMessage(ex));
            logOutcome(toolCall, outcome, startedAt);
            return outcome;
        } catch (Exception ex) {
            AgentToolOutcome outcome = AgentToolOutcome.failed(
                    AgentToolOutcome.FAILURE_UNEXPECTED,
                    "这一步没有成功，你可以自己在页面上操作");
            logOutcome(toolCall, outcome, startedAt);
            return outcome;
        }
    }

    private AgentToolOutcome dispatch(Long userId, Long recordId, AgentToolProposal proposal) {
        return switch (proposal.tool()) {
            case APPEND_RECORD_CONTENT -> {
                recordService.appendContent(userId, recordId, proposal.text());
                yield AgentToolOutcome.executed("已把这段放进正文");
            }
            case ADD_RECORD_TAGS -> {
                recordService.appendTags(userId, recordId, new ArrayList<>(proposal.tagIds()));
                yield AgentToolOutcome.executed("已添加标签");
            }
            case PROPOSE_UNLOCK_AT -> {
                LocalDateTime unlockAt = validator.parseUnlockAt(proposal.unlockAt());
                if (unlockAt == null) {
                    yield AgentToolOutcome.failed(
                            AgentToolOutcome.FAILURE_BUSINESS_REJECTED,
                            "解锁时间格式不对");
                }
                recordService.updateUnlockAt(userId, recordId, unlockAt);
                yield AgentToolOutcome.executed("已设置解锁时间");
            }
            // 读工具不可执行：它们由后端预注入上下文，不进入执行分发。
            default -> AgentToolOutcome.failed(
                    AgentToolOutcome.FAILURE_PRECONDITION,
                    "这个操作我做不了");
        };
    }

    private String readableMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "这一步没有成功" : message;
    }

    /**
     * 结构化日志：只含 sessionId / toolCallId / tool / status / failureType / 耗时。
     */
    private void logOutcome(AgentToolCall toolCall, AgentToolOutcome outcome, long startedAt) {
        long costMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        log.info("agent tool execution sessionId={} toolCallId={} tool={} status={} failureType={} costMs={}",
                toolCall.getSessionId(),
                toolCall.getId(),
                toolCall.getToolName(),
                outcome.status(),
                outcome.failureType() == null ? "none" : outcome.failureType(),
                costMillis);
    }

    /**
     * 提供只读的工具名列表，便于测试与审查确认可执行范围。
     */
    public List<String> executableToolNames() {
        List<String> names = new ArrayList<>();
        for (AgentToolName tool : AgentToolName.values()) {
            if (tool.isWriteTool()) {
                names.add(tool.wireName());
            }
        }
        return List.copyOf(names);
    }
}
