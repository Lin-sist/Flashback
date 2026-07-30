package com.flashback.mapper;

import com.flashback.domain.AgentTurnTrace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 决策轨迹的读写（C5）。
 *
 * 查询侧刻意只有「按会话取回」与「按用户取回」两个入口（design 决策 4）：
 * C5 的排查场景是「拿到一个 sessionId，看那几轮发生了什么」，
 * 不是跨会话统计——后者属于 C6 的范围。
 */
@Mapper
public interface AgentTurnTraceMapper {

    int insert(AgentTurnTrace trace);

    /**
     * 按会话取回全部轨迹，按轮次与尝试序号正序。
     *
     * 不带 userId 谓词：轨迹面向开发者排查，且无对外端点暴露（N4），
     * 归属校验由调用方（集成测试或未来的内部工具）负责。
     */
    List<AgentTurnTrace> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 按用户取回最近若干条轨迹，倒序。
     */
    List<AgentTurnTrace> selectRecentByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    /**
     * 某一轮已有多少条轨迹，用于推导下一次尝试的序号。
     *
     * 只在**同轮重试**时调用——首次尝试恒为 1，不必查库。
     */
    int countBySessionAndTurn(@Param("sessionId") Long sessionId, @Param("turnNo") int turnNo);

    /**
     * 保留期清理（N7 / 决策 11）。
     *
     * 手动调用，本刀不引入定时任务——自动删数据在无备份策略的环境里风险不对称。
     *
     * @return 删除行数
     */
    int deleteCreatedBefore(@Param("threshold") LocalDateTime threshold);
}
