package com.flashback.mapper;

import com.flashback.domain.AgentSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgentSessionMapper {

    int insert(AgentSession session);

    /**
     * 按 id + userId 查询，归属校验直接落在 SQL 上，避免遗漏。
     */
    AgentSession selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 查询该用户在某记录上进行中的会话（recordId 为 null 时查无记录关联的会话）。
     */
    AgentSession selectActiveByUserAndRecord(@Param("userId") Long userId, @Param("recordId") Long recordId);

    int updateProgress(AgentSession session);
}
