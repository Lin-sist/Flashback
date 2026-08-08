package com.flashback.agent.temporal;

import java.time.LocalDateTime;

/** 固定 Clock 下计算出的内部时间距离；UNKNOWN 时 distanceDays 为 null。 */
public record TemporalDistance(
        LocalDateTime occurredAt,
        String timeLabel,
        Long distanceDays,
        TemporalDistanceBand band) {
}
