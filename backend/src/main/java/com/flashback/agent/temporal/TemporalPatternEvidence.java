package com.flashback.agent.temporal;

/** 复现提示的结构化证据；只记录数量和跨度，不记录用户文本。 */
public record TemporalPatternEvidence(boolean eligible, int distinctRecordCount, long spanDays) {

    public static TemporalPatternEvidence absent() {
        return new TemporalPatternEvidence(false, 0, 0L);
    }
}
