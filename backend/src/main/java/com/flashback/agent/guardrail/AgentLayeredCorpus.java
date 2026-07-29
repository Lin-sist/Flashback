package com.flashback.agent.guardrail;

import com.flashback.domain.AgentMessage;

import java.util.List;

/**
 * 分层来源集合（C3 agent-memory-retrieval）。
 *
 * 形态（design.md 决策 3）：包装两个既有的 {@link AgentSourceCorpus}，
 * 而不是改造 AgentSourceCorpus 内部结构。
 *
 * 为什么用包装而不是改造：C4 留下的 397 项测试基线里有大量用例直接构造并断言
 * AgentSourceCorpus 的行为。把它内部改成「层 → n-gram 集合」映射会牵动
 * containsNgram / containsFragment 的语义，改断言的概率很高，
 * 而 AGENTS.md 要求改既有断言必须披露请示。多一个类换基线安全，划算。
 *
 * 两层的权限**不对等**，这是本类的核心语义而非实现细节：
 * - 会话层（当前会话中用户自己说过的话）是唯一可以进入记录正文的来源；
 * - 记忆层（本轮实际注入的历史片段）只能支撑对话中的复述，且复述须带时间归属。
 *
 * 对应三个出口：
 * - {@link #sessionOnly()} 供「会进用户正文」的路径使用，恒为单层；
 * - {@link #combined()} 供对话回复与引用检查使用，两层合并；
 * - {@link #longestMemoryOnlyRun(String)} 供时间归属检查使用，识别「只来自记忆层」的片段。
 *
 * 隐私：与 AgentSourceCorpus 同级——只存在于内存，不落库、不写日志、不外发。
 */
public final class AgentLayeredCorpus {

    private final AgentSourceCorpus session;
    private final AgentSourceCorpus memory;
    private final AgentSourceCorpus combined;
    private final boolean memoryPresent;

    private AgentLayeredCorpus(
            AgentSourceCorpus session, AgentSourceCorpus memory, AgentSourceCorpus combined, boolean memoryPresent) {
        this.session = session;
        this.memory = memory;
        this.combined = combined;
        this.memoryPresent = memoryPresent;
    }

    /**
     * 只有会话层的分层语料。等价于 C4 现状，用于未注入记忆的轮次。
     *
     * 这条路径必须存在且行为与 C4 完全一致：检索失败、无命中、记忆开关关闭时
     * 护栏判定要照旧严格，不能因为「没有记忆层」而变宽松（design.md 决策 6）。
     */
    public static AgentLayeredCorpus sessionOnly(AgentSourceCorpus session) {
        AgentSourceCorpus safeSession = session == null ? AgentSourceCorpus.of(List.of(), 1) : session;
        return new AgentLayeredCorpus(safeSession, AgentSourceCorpus.ofTexts(List.of(), 1), safeSession, false);
    }

    /**
     * 由会话历史与**本轮实际注入**的记忆片段构造分层语料。
     *
     * @param history           会话消息，仅 role=USER 会进入会话层
     * @param injectedFragments 本轮实际注入 prompt 的记忆片段原文；null 或空表示无记忆层
     * @param ngramSize         n-gram 长度，两层必须一致，否则合并层的命中语义会错乱
     */
    public static AgentLayeredCorpus of(
            List<AgentMessage> history, List<String> injectedFragments, int ngramSize) {

        AgentSourceCorpus session = AgentSourceCorpus.of(history, ngramSize);
        if (injectedFragments == null || injectedFragments.isEmpty()) {
            return sessionOnly(session);
        }
        AgentSourceCorpus memory = AgentSourceCorpus.ofTexts(injectedFragments, ngramSize);
        if (memory.isEmpty()) {
            return sessionOnly(session);
        }

        // 合并层不是「两个集合求并」的语法糖：判定需要一个能同时命中两层的语料对象，
        // 而 AgentCoverageProfile 只接受单个 corpus。因此这里重新构造一个含两层全部
        // 来源的语料，用于「允许引用记忆」的路径。
        AgentSourceCorpus combined = AgentSourceCorpus.merge(session, memory);
        return new AgentLayeredCorpus(session, memory, combined, true);
    }

    /**
     * 会话层。**唯一**可作为记录正文合法来源的层（不变量 2，不可配置放宽）。
     */
    public AgentSourceCorpus sessionOnly() {
        return session;
    }

    /**
     * 记忆层。只用于对话中的复述，且复述须带时间归属。
     */
    public AgentSourceCorpus memoryOnly() {
        return memory;
    }

    /**
     * 两层合并。用于对话回复、提议话术与引号片段的判定。
     */
    public AgentSourceCorpus combined() {
        return combined;
    }

    /**
     * 本轮是否真的注入了记忆片段。
     *
     * 与「检索是否命中」不同：命中但未注入时本项为 false，
     * 因为未注入的内容不构成合法来源（不变量 1）。
     */
    public boolean hasMemory() {
        return memoryPresent;
    }

    /**
     * 计算候选文本中「只被记忆层覆盖、不被会话层覆盖」的连续片段长度。
     *
     * 这是时间归属检查的唯一输入（design.md 决策 2）：
     * 若这样的片段足够长，说明 Agent 正在复述过去的内容，
     * 此时它必须说清那是哪个时候的事，否则读起来就像用户刚刚说的。
     *
     * 判定基于位图比较，确定性、零外调。
     *
     * @return 最长的「仅记忆层覆盖」连续片段字符数；无记忆层时恒为 0
     */
    public int longestMemoryOnlyRun(String candidate) {
        if (!memoryPresent) {
            return 0;
        }
        AgentCoverageProfile sessionProfile = AgentCoverageProfile.of(candidate, session);
        AgentCoverageProfile combinedProfile = AgentCoverageProfile.of(candidate, combined);
        return AgentCoverageProfile.longestExclusiveRun(sessionProfile, combinedProfile);
    }
}
