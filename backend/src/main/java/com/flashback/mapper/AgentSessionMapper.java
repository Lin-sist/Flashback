package com.flashback.mapper;

import com.flashback.domain.AgentSession;
import com.flashback.domain.AgentSessionPurpose;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AgentSessionMapper {

    int insert(AgentSession session);

    /**
     * 按 id + userId 查询，归属校验直接落在 SQL 上，避免遗漏。
     */
    AgentSession selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<AgentSession> selectByRecordIdAndUserId(@Param("recordId") Long recordId, @Param("userId") Long userId);

    /**
     * 查询该用户在某记录上、某用途下进行中的会话（recordId 为 null 时查无记录关联的会话）。
     *
     * C3b 增加 purpose 谓词（design 决策 9）：不加也能工作，因为同一条记录不可能
     * 同时是 DRAFT 和 UNLOCKED——但那让契约依赖一个巧合。一旦将来出现
     * 「解锁后允许再编辑」之类的变化，两种会话就会互相串，表现是
     * 「用户点回看却恢复了三个月前那次写作对话」，极难排查。
     */
    AgentSession selectActiveByUserAndRecord(
            @Param("userId") Long userId,
            @Param("recordId") Long recordId,
            @Param("purpose") AgentSessionPurpose purpose);

    int updateProgress(AgentSession session);
}
