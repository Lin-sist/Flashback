package com.flashback.agent.temporal;

/** 只含时间元数据的 prompt 辅助上下文，不承载日记原文。 */
public record TemporalMemoryContext(
        Long recordId,
        String timeLabel,
        TemporalDistanceBand band,
        Long distanceDays,
        boolean focal) {
}
