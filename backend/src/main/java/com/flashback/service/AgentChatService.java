package com.flashback.service;

import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.vo.AgentSessionVO;

/**
 * Agent 多轮对话服务（C1）。
 *
 * 范围边界：C1 只做对话 Runtime，不调用任何记录写操作，
 * Tool Calling / Memory / 后置过滤分别留给 C2 / C3 / C4。
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
}
