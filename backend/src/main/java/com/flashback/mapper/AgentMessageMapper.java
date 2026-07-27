package com.flashback.mapper;

import com.flashback.domain.AgentMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMessageMapper {

    int insert(AgentMessage message);

    /**
     * 按会话读取全部消息，正序返回，用于中断恢复。
     */
    List<AgentMessage> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 读取最近若干条消息（返回为倒序），用于组装上下文滑动窗口。
     */
    List<AgentMessage> selectRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);
}
