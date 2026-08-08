package com.flashback.agent.resilience;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * C8：一次 Agent HTTP 编排共享的 provider-work deadline。
 *
 * <p>使用 monotonic nano time；对象只保存时间预算，不保存任何用户内容。</p>
 */
public final class AgentCallBudget {

    static final long MIN_SAFE_CALL_MILLIS = 100L;

    private final long totalMillis;
    private final LongSupplier nanoTime;
    private final long startedAtNanos;

    private AgentCallBudget(long totalMillis, LongSupplier nanoTime) {
        if (totalMillis <= 0L) {
            throw new IllegalArgumentException("totalMillis must be positive");
        }
        this.totalMillis = totalMillis;
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.startedAtNanos = nanoTime.getAsLong();
    }

    public static AgentCallBudget start(long totalMillis) {
        return start(totalMillis, System::nanoTime);
    }

    public static AgentCallBudget start(long totalMillis, LongSupplier nanoTime) {
        return new AgentCallBudget(totalMillis, nanoTime);
    }

    public long nextCallTimeoutMillis(long perCallMaxMillis) throws AgentProviderException {
        if (perCallMaxMillis <= 0L) {
            throw new IllegalArgumentException("perCallMaxMillis must be positive");
        }
        long remaining = remainingMillis();
        if (remaining < MIN_SAFE_CALL_MILLIS) {
            throw AgentProviderException.deadlineExhausted();
        }
        return Math.min(perCallMaxMillis, remaining);
    }

    public long remainingMillis() {
        long elapsedNanos = Math.max(0L, nanoTime.getAsLong() - startedAtNanos);
        long elapsedMillis = elapsedNanos / 1_000_000L;
        return Math.max(0L, totalMillis - elapsedMillis);
    }

    /** 小于最小安全发起阈值即视为耗尽，而非等到精确 0。 */
    public boolean isExhausted() {
        return remainingMillis() < MIN_SAFE_CALL_MILLIS;
    }

    public String remainingBucket() {
        long remaining = remainingMillis();
        if (remaining <= 0L) {
            return "none";
        }
        if (remaining < 1_000L) {
            return "lt-1s";
        }
        if (remaining <= 5_000L) {
            return "1-5s";
        }
        return "gt-5s";
    }
}
