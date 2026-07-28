package com.flashback.agent.guardrail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 违规降级处置（C4）。
 *
 * 处置按路径分流（design.md 决策 3），因为三条路径的失败成本不同：
 * - 工具提议 → **拒绝**：用户只是少看到一个确认条，对话完全不受影响；
 * - 素材草稿 → **丢弃**：素材是可选产物，复用「生成失败即为 null」的既有语义；
 * - 对话回复 → **替换为安全兜底**：用户提交了一轮消息必须得到回应，
 * 拒绝或丢弃会让对话看起来挂了。
 *
 * 失败方向是刻意选的：宁可少一个确认条 / 少一段素材，
 * 也不放行一句用户没说过的话进他的日记。
 *
 * 关键边界（design.md §6）：兜底回复是**本地常量**，不是 provider 产物。
 * 使用时必须留下结构化痕迹，使其可与真实回复区分——
 * 兜底不得伪装成模型正常输出（对齐 AGENTS.md「真实路径不得 mock success 冒充真实成功」）。
 */
@Component
public class AgentGuardrailDowngrade {

    private static final Logger log = LoggerFactory.getLogger(AgentGuardrailDowngrade.class);

    /**
     * 安全兜底回复。
     */
    public String safeFallbackReply() {
        return AgentGuardrailRules.SAFE_FALLBACK_REPLY;
    }

    /**
     * 记录一次降级痕迹。
     *
     * 隐私：只输出结构化元数据与数值指标，**不输出**候选文本、用户原话
     * 或未覆盖片段内容（agent-runtime delta 的留痕条款）。
     *
     * @param path    发生降级的路径标识（reply / material / tool-proposal / ask-text）
     * @param verdict 判定结果
     */
    public void trace(String path, Long sessionId, Integer turnNo, AgentGuardrailVerdict verdict) {
        if (verdict == null || verdict.isPassed()) {
            return;
        }
        log.warn("agent guardrail downgrade path={} sessionId={} turnNo={} violation={} {} fallback=local",
                path,
                sessionId,
                turnNo,
                verdict.reason(),
                verdict.metrics());
    }
}
