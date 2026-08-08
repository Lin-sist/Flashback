package com.flashback.agent.temporal;

/** 内部时间距离分层；不得作为用户标签输出。 */
public enum TemporalDistanceBand {
    RECENT,
    DISTANT,
    LONG_AGO,
    UNKNOWN
}
