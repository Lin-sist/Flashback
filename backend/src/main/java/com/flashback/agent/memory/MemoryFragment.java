package com.flashback.agent.memory;

import java.time.LocalDateTime;

/**
 * 一段可注入上下文的历史记录片段（C3 agent-memory-retrieval）。
 *
 * 为什么必须带时间锚点（design.md §2.2、决策 1）：
 * 片段一旦进入 prompt，模型就有可能复述它。若片段本身不携带「这是什么时候的事」，
 * 模型无从表达时间归属，而缺少时间归属的复述会让三个月前的心情读起来像此刻的心情。
 * 时间锚点既是 prompt 里可读的事实，也是时间归属护栏的语义目标。
 *
 * 隐私（design.md §6）：{@code text} 是**其他记录的日记原文**，属最高敏数据。
 * 本对象只允许存在于内存与发往 provider 的 prompt 中，
 * 禁止写入 agent_message / agent_tool_call / 日志 / 审计痕迹。
 * 因此本类刻意不实现 toString——避免任何一次不经意的日志拼接把原文带出去。
 *
 * @param recordId   来源记录标识（可安全记入结构化痕迹）
 * @param occurredAt 该记录的发生时间，用于排序与生成时间标签
 * @param timeLabel  注入 prompt 用的可读时间标签，例如「2026年3月」
 * @param text       片段原文（已按配置截断）
 * @param contextNote 用户后来补充的时间语境说明；同样不得进入日志或来源关系表
 */
public record MemoryFragment(
        Long recordId,
        LocalDateTime occurredAt,
        String timeLabel,
        String text,
        String contextNote) {

    public MemoryFragment(Long recordId, LocalDateTime occurredAt, String timeLabel, String text) {
        this(recordId, occurredAt, timeLabel, text, null);
    }

    /**
     * 刻意屏蔽默认 record toString：默认实现会把 text 原文拼进字符串，
     * 一旦被日志或异常信息意外引用就是日记原文泄露。
     */
    @Override
    public String toString() {
        return "MemoryFragment{recordId=" + recordId + ", timeLabel=" + timeLabel
                + ", textChars=" + (text == null ? 0 : text.length()) + "}";
    }
}
