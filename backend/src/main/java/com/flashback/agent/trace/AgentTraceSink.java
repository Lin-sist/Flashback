package com.flashback.agent.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentTurnTrace;
import com.flashback.mapper.AgentTurnTraceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 决策轨迹的唯一落库出口（C5）。
 *
 * <h3>为什么只有一个出口（design.md 决策 2）</h3>
 * 采集点分散在编排的七八个分支里，其中 provider 失败、护栏降级、fail-closed 丢弃
 * 都是**早退路径**。若每处各自 insert，早退时最容易被跳过——而那正是最需要痕迹的时刻。
 * 收集器把一轮内的步骤攒在内存里，只在这里落一次。
 *
 * <h3>为什么必须 REQUIRES_NEW（design.md 决策 7）</h3>
 * {@code AgentChatServiceImpl.sendMessage} 是 {@code @Transactional} 的。
 * 若轨迹写入加入同一事务且失败，会把**用户消息与 Agent 回复一起回滚掉**——
 * 那正是 C1 花力气保证的「provider 失败时用户输入不丢」的反面。
 * 一个可观测设施绝不该有能力毁掉用户这一轮的表达，所以起独立事务。
 *
 * 同理，写入异常一律 fail-open：只记 warn，不向上抛。痕迹缺一条的代价是
 * 「这一轮不好排查」，对话失败一轮的代价是用户真实体验受损，两者不对称。
 */
@Component
public class AgentTraceSink {

    private static final Logger log = LoggerFactory.getLogger(AgentTraceSink.class);
    private static final ObjectMapper STEPS_MAPPER = new ObjectMapper();

    /** steps_json 落库上限。超限时截断并留标记，绝不让一行痕迹把 insert 打挂。 */
    private static final int MAX_STEPS_JSON_CHARS = 60000;

    private final AgentTurnTraceMapper agentTurnTraceMapper;
    private final AppAgentProperties appAgentProperties;
    private final Clock clock;

    public AgentTraceSink(
            AgentTurnTraceMapper agentTurnTraceMapper,
            AppAgentProperties appAgentProperties,
            Clock clock) {
        this.agentTurnTraceMapper = agentTurnTraceMapper;
        this.appAgentProperties = appAgentProperties;
        this.clock = clock;
    }

    /**
     * 可观测能力是否启用。
     *
     * 调用方据此决定是否创建收集器；关闭时留痕说明未生效，
     * **不静默表现为轨迹无数据**（沿用 C3a memory 开关的既有语义）。
     */
    public boolean isEnabled() {
        return appAgentProperties.getObservability().isEnabled();
    }

    /**
     * 关闭状态下的留痕。每轮一条 info，使「没有轨迹」可与「采集失败」区分。
     */
    public void traceDisabled(Long sessionId) {
        log.info("agent observability disabled by config sessionId={}", sessionId);
    }

    /**
     * 本轮的尝试序号。
     *
     * 首次尝试恒为 1，不查库；同轮重试时按已有轨迹条数推导。
     * 放在 sink 而非编排层的理由：编排层不该知道轨迹表的存在，
     * 它只面对「收集器 + 落库出口」这两个概念。
     *
     * 查询失败时退回 1 —— 序号不准只是让重试不好区分，不值得因此中断这一轮。
     */
    public int nextAttemptNo(Long sessionId, int turnNo, boolean retry) {
        if (!retry) {
            return 1;
        }
        try {
            return agentTurnTraceMapper.countBySessionAndTurn(sessionId, turnNo) + 1;
        } catch (RuntimeException ex) {
            log.warn("agent trace attempt lookup failed sessionId={} turnNo={} cause={}",
                    sessionId, turnNo, ex.getClass().getSimpleName());
            return 1;
        }
    }

    /**
     * 落库一条轨迹。
     *
     * @param collector 本轮收集器；null 表示可观测关闭，直接返回
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(AgentTraceCollector collector) {
        if (collector == null) {
            return;
        }
        try {
            agentTurnTraceMapper.insert(toEntity(collector));
        } catch (RuntimeException ex) {
            // fail-open：轨迹写不下去不能让用户这一轮对话挂掉。
            // 只记异常类型，不记异常消息——消息可能回带 SQL 参数。
            log.warn("agent trace persist failed sessionId={} turnNo={} attemptNo={} cause={}",
                    collector.sessionId(),
                    collector.turnNo(),
                    collector.attemptNo(),
                    ex.getClass().getSimpleName());
        }
    }

    /**
     * 保留期清理（N7）。手动调用，不引入定时任务。
     *
     * @return 删除行数
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeExpired() {
        int retentionDays = appAgentProperties.getObservability().getRetentionDays();
        if (retentionDays <= 0) {
            // 0 或负值表示不清理；不把它解释成「删除全部」——那个误读的代价太大。
            return 0;
        }
        LocalDateTime threshold = LocalDateTime.now(clock).minusDays(retentionDays);
        int deleted = agentTurnTraceMapper.deleteCreatedBefore(threshold);
        log.info("agent trace purge retentionDays={} deleted={}", retentionDays, deleted);
        return deleted;
    }

    private AgentTurnTrace toEntity(AgentTraceCollector collector) {
        AgentTurnTrace trace = new AgentTurnTrace();
        trace.setTraceId(collector.traceId());
        trace.setSessionId(collector.sessionId());
        trace.setUserId(collector.userId());
        trace.setRecordId(collector.recordId());
        trace.setTurnNo(collector.turnNo());
        trace.setAttemptNo(collector.attemptNo());
        trace.setPurpose(collector.purpose().name());
        trace.setStage(collector.stage() == null ? "UNKNOWN" : collector.stage().name());
        trace.setStageReason(collector.stageReason() == null ? null : collector.stageReason().name());
        trace.setModel(collector.model());
        trace.setPromptVersion(collector.promptVersion());
        trace.setPolicyVersion(collector.policyVersion());
        trace.setOutcome(collector.outcome().name());
        trace.setProviderDurationMs(collector.providerDurationMs());
        trace.setCauseType(collector.causeType());
        trace.setDowngradePath(collector.downgradePath());
        trace.setViolation(collector.violation());
        trace.setStepsJson(serializeSteps(collector.steps()));
        trace.setCreatedAt(LocalDateTime.now(clock));
        return trace;
    }

    /**
     * 序列化步骤明细。
     *
     * 序列化失败不放弃整条轨迹——头部字段（结果、耗时、违规类型、版本）本身就有排查价值，
     * 为了明细丢掉它们不划算。
     */
    private String serializeSteps(List<Map<String, Object>> steps) {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        try {
            String json = STEPS_MAPPER.writeValueAsString(steps);
            if (json.length() > MAX_STEPS_JSON_CHARS) {
                log.warn("agent trace steps truncated length={}", json.length());
                return json.substring(0, MAX_STEPS_JSON_CHARS);
            }
            return json;
        } catch (Exception ex) {
            log.warn("agent trace steps serialization failed cause={}", ex.getClass().getSimpleName());
            return null;
        }
    }
}
