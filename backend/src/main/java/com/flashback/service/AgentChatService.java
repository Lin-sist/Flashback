package com.flashback.service;

import com.flashback.agent.tool.AgentToolDecision;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.vo.AgentSessionVO;
import com.flashback.domain.AgentConversationIntent;

/**
 * Agent 多轮对话服务（C1 + C2）。
 *
 * 范围边界：C2 增加受白名单约束的工具调用，
 * Memory / 后置过滤 / 可观测查询分别留给 C3 / C4 / C5。
 */
public interface AgentChatService {

    /**
     * 开启或恢复会话。同一记录上最多一个 ACTIVE 会话。
     */
    AgentSessionVO startOrResume(Long userId, AgentSessionStartRequest request);

    /**
     * 读取会话与全部消息，用于中断恢复。
     */
    AgentSessionVO getSession(Long userId, Long sessionId);

    /**
     * 提交一轮用户消息并取得 Agent 回复。
     */
    AgentSessionVO sendMessage(Long userId, Long sessionId, AgentMessageRequest request);

    /**
     * 用户主动结束会话，返回素材草稿。
     */
    AgentSessionVO finish(Long userId, Long sessionId);

    /** P4.1：显式切换 ACTIVE WRITING_GUIDANCE 的用户意图。 */
    AgentSessionVO switchConversationIntent(
            Long userId, Long sessionId, AgentConversationIntent conversationIntent);

    /** P4.2：显式切换当前会话的跨记录记忆授权，不调用 provider。 */
    AgentSessionVO switchMemoryAuthorization(
            Long userId, Long sessionId, Boolean crossRecordMemoryEnabled);

    /**
     * C2：确认（接受或拒绝）一条工具提议。
     *
     * 这是工具执行的**唯一**入口：Agent 生成回复时只能提议，
     * 实际写操作只发生在本方法（design.md 决策 2、9）。
     * 重复确认幂等，不重复执行。
     */
    AgentSessionVO confirmToolCall(Long userId, Long sessionId, Long toolCallId, AgentToolDecision decision);
}
