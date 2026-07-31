package com.flashback.agent.eval;

import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.agent.trace.AgentTraceSink;
import com.flashback.config.AppAgentProperties;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * 把轨迹收在内存里的落库出口替身（C6，N2 / design 决策 1）。
 *
 * <h3>为什么不落库</h3>
 * 评测断言的是**编排信号**（阶段序列、注入规模、护栏 verdict、降级层、长度比），
 * 而这些在收集器里已经全部就位。走 DB 只会带来两样东西：Spring 上下文的启动成本，
 * 以及 C5 那条反直觉约束——{@code persist} 用
 * {@code TransactionSynchronization.afterCompletion} 延后落库（那是修「每轮卡 50 秒」
 * 锁等待的产物），因此走库的测试**不能加 {@code @Transactional}**，得手工清理。
 * 为「断言编排信号」付这个成本，零增量价值。轨迹落库的正确性归 C5 的
 * {@code AgentObservabilityIntegrationTest}，本刀不重复。
 *
 * <h3>为什么是手写替身而不是 Mockito mock</h3>
 * 评测要按顺序读回多轮的收集器。用 mock 就得每条用例摆 ArgumentCaptor，
 * 而 captor 拿到的是同一个可变对象的引用——多轮时读到的是**最后一轮的终态**，
 * 前几轮的中间状态已经被覆盖。本类在 persist 时刻把收集器逐个存下，顺序天然正确。
 *
 * 继承 {@link AgentTraceSink} 而非新起一个接口：生产代码里
 * {@code AgentChatServiceImpl} 依赖的就是这个具体类，抽接口需要改 main，
 * 而本刀承诺 {@code src/main} 零改动。
 */
final class RecordingTraceSink extends AgentTraceSink {

    private final List<AgentTraceCollector> persisted = new ArrayList<>();
    private final List<Long> disabledFor = new ArrayList<>();
    private final boolean enabled;

    /**
     * 三个 super 构造参数都传得起：本类覆写了全部会触碰它们的方法，
     * 因此 mapper 传 null 不会被解引用。
     */
    private RecordingTraceSink(boolean enabled) {
        super(null, new AppAgentProperties(), Clock.systemDefaultZone());
        this.enabled = enabled;
    }

    static RecordingTraceSink enabled() {
        return new RecordingTraceSink(true);
    }

    static RecordingTraceSink disabled() {
        return new RecordingTraceSink(false);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void traceDisabled(Long sessionId) {
        disabledFor.add(sessionId);
    }

    /**
     * 尝试序号：首次为 1，重试时按已收集的同轮条数推导。
     *
     * 刻意复刻生产语义而不是恒返回 1——否则「同轮重试可区分」这条不变量
     * 断言的就是我自己写死的值。
     */
    @Override
    public int nextAttemptNo(Long sessionId, int turnNo, boolean retry) {
        if (!retry) {
            return 1;
        }
        int seen = 0;
        for (AgentTraceCollector collector : persisted) {
            if (collector != null
                    && sessionId.equals(collector.sessionId())
                    && collector.turnNo() == turnNo) {
                seen++;
            }
        }
        return seen + 1;
    }

    /**
     * 唯一出口。null 表示可观测关闭时编排层没有创建收集器，此处如实记下。
     */
    @Override
    public void persist(AgentTraceCollector collector) {
        persisted.add(collector);
    }

    @Override
    public void persistNow(AgentTraceCollector collector) {
        throw new AssertionError(
                "persistNow 不该被编排层直接调用：它会绕过 C5 的延后落库机制（锁等待故障的修复点）");
    }

    /**
     * 按 persist 顺序取回全部收集器，含可观测关闭时的 null。
     */
    List<AgentTraceCollector> all() {
        return List.copyOf(persisted);
    }

    /**
     * 只取真正产生了轨迹的轮次。
     */
    List<AgentTraceCollector> traces() {
        List<AgentTraceCollector> result = new ArrayList<>();
        for (AgentTraceCollector collector : persisted) {
            if (collector != null) {
                result.add(collector);
            }
        }
        return List.copyOf(result);
    }

    /**
     * 最后一轮的轨迹；无轨迹时返回 null。
     */
    AgentTraceCollector last() {
        List<AgentTraceCollector> traces = traces();
        return traces.isEmpty() ? null : traces.get(traces.size() - 1);
    }

    List<Long> disabledFor() {
        return List.copyOf(disabledFor);
    }
}
