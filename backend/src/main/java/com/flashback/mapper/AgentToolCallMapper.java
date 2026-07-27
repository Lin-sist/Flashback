package com.flashback.mapper;

import com.flashback.domain.AgentToolCall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AgentToolCallMapper {

    int insert(AgentToolCall toolCall);

    /**
     * 按 id + 归属用户读取；跨用户查询必然返回 null。
     */
    AgentToolCall selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 读取会话中当前待确认的提议（至多一条，design 决策 10）。
     */
    AgentToolCall selectPendingBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 读取最近若干条已终结的工具调用（倒序），用于回注对话上下文。
     */
    List<AgentToolCall> selectRecentSettledBySessionId(
            @Param("sessionId") Long sessionId,
            @Param("limit") int limit);

    /**
     * 条件更新：只有当前状态仍为 PROPOSED 时才允许流转，保证重复确认幂等。
     *
     * @return 影响行数；0 表示已被其他请求处理，调用方须按幂等返回当前状态
     */
    int updateStatusIfProposed(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("failureType") String failureType,
            @Param("resultSummary") String resultSummary,
            @Param("updatedAt") LocalDateTime updatedAt);
}
