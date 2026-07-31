package com.flashback.agent.eval;

/**
 * 评测维度（C6，蓝图 §4.2 的八维度表）。
 *
 * 八个维度中七个是**确定性**的：给定编排好的输入，期望值唯一，不符即失败。
 * 第八个（{@link #NARRATIVE_QUALITY}）是**回归型**的——它评的不是对错，
 * 而是「相对基线有没有变」，且它的绝对质量判断只能靠人评锚点。
 *
 * 这个划分本身是本刀最重要的诚实边界（D32）：替身路径评的是编排逻辑，
 * 不是模型的语言质量。把语言质量也塞进确定性维度，等于假装一个不存在的能力。
 */
enum AgentEvalDimension {

    /** 阶段判定序列（from / to / reason）合法。 */
    STAGE_PROGRESSION,

    /** 同阶段追问不超过上限；轮次上限可触发；用户结束意图被尊重。 */
    REASK_RESTRAINT,

    /** 记忆三态（关闭 / 失败 / 无命中）可区分。 */
    MEMORY_STATES,

    /** 注入条数与总长度不超过派生上限。 */
    INJECTION_BUDGET,

    /** 各层护栏判定与降级处置符合期望。 */
    GUARDRAIL,

    /** 回复长度不超过硬上限；裁剪留痕且不被记为降级。 */
    LENGTH_RESTRAINT,

    /** 无工具模式下的提议丢弃，以及提议被护栏拒绝。 */
    TOOL_FAIL_CLOSED,

    /**
     * 话术质量（**回归型，非判分**）。
     *
     * 本刀只建结构：指标进快照做前后比对，绝对质量靠 baseline/narrative-anchors.yaml
     * 的人评锚点，而那份锚点**当前为空**（N7 / 决策 8）。
     * 空锚点不等于该维度已覆盖——这一点在锚点文件里也写着。
     */
    NARRATIVE_QUALITY
}
