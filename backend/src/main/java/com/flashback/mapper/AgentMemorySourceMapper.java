package com.flashback.mapper;

import com.flashback.domain.AgentMemorySource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMemorySourceMapper {

    int insert(AgentMemorySource source);

    List<AgentMemorySource> selectBySessionIdAndUserId(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId);
}
