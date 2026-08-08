package com.flashback.agent.temporal;

import com.flashback.agent.AgentChatMode;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentTemporalPolicyTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-08T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

    private final AppAgentProperties properties = new AppAgentProperties();
    private AgentTemporalPolicy policy = new AgentTemporalPolicy(properties, CLOCK);

    @Test
    void shouldClassifyBoundaryDaysAndFutureAsUnknown() {
        List<MemoryFragment> fragments = List.of(
                fragment(1L, "2026-07-09T12:00:00", 100),
                fragment(2L, "2026-07-08T12:00:00", 100),
                fragment(3L, "2026-02-09T12:00:00", 100),
                fragment(4L, "2026-02-08T12:00:00", 100),
                fragment(5L, "2026-08-09T12:00:00", 100),
                new MemoryFragment(6L, null, "某时", "甲".repeat(100)),
                fragment(7L, "2026-08-08T00:00:00", 100));

        TemporalPolicyResult result = policy.evaluate(
                AgentChatMode.WRITING_GUIDANCE, "", List.of(), fragments);

        assertThat(result.contexts()).extracting(TemporalMemoryContext::band)
                .containsExactly(
                        TemporalDistanceBand.RECENT,
                        TemporalDistanceBand.DISTANT,
                        TemporalDistanceBand.DISTANT,
                        TemporalDistanceBand.LONG_AGO,
                        TemporalDistanceBand.UNKNOWN,
                        TemporalDistanceBand.UNKNOWN,
                        TemporalDistanceBand.RECENT);
    }

    @Test
    void shouldDecayOnlyAncillaryMemoryAndRespectMinimumWithoutExpansion() {
        MemoryFragment focal = fragment(10L, "2025-01-01T00:00:00", 100);
        MemoryFragment distant = fragment(11L, "2026-04-01T00:00:00", 100);
        MemoryFragment longAgo = fragment(12L, "2025-01-01T00:00:00", 60);
        MemoryFragment shortLongAgo = fragment(13L, "2025-01-01T00:00:00", 20);

        TemporalPolicyResult result = policy.evaluate(
                AgentChatMode.REVIEW_CHAT, "", List.of(focal), List.of(distant, longAgo, shortLongAgo));

        assertThat(result.injectedFragments()).extracting(fragment -> fragment.text().length())
                .containsExactly(100, 90, 60, 20);
        assertThat(result.beforeChars()).isEqualTo(280);
        assertThat(result.afterChars()).isEqualTo(270);
    }

    @Test
    void shouldUseBaseBudgetsForAllBandsIncludingUnknown() {
        List<MemoryFragment> fragments = List.of(
                fragment(41L, "2026-07-15T00:00:00", 120),
                fragment(42L, "2026-04-01T00:00:00", 120),
                fragment(43L, "2025-01-01T00:00:00", 120),
                new MemoryFragment(44L, null, "某时", "甲".repeat(120)));

        TemporalPolicyResult result = policy.evaluate(
                AgentChatMode.WRITING_GUIDANCE, "", List.of(), fragments);

        assertThat(result.injectedFragments()).extracting(fragment -> fragment.text().length())
                .containsExactly(120, 90, 60, 60);
    }

    @Test
    void shouldNeverExpandWhenConfiguredBaseIsBelowMinimum() {
        properties.getMemory().setMaxFragmentChars(20);
        MemoryFragment old = fragment(45L, "2025-01-01T00:00:00", 20);

        assertThat(policy.evaluate(AgentChatMode.WRITING_GUIDANCE, "", List.of(), List.of(old))
                .injectedFragments().get(0).text()).hasSize(20);
    }

    @Test
    void shouldSafelyIgnoreNullFragmentAndKeepNullOrBlankText() {
        List<MemoryFragment> inputs = new java.util.ArrayList<>();
        inputs.add(null);
        inputs.add(new MemoryFragment(46L, null, "某时", null));
        inputs.add(new MemoryFragment(47L, null, "某时", ""));

        TemporalPolicyResult result = policy.evaluate(
                AgentChatMode.WRITING_GUIDANCE, "", List.of(), inputs);

        assertThat(result.injectedFragments()).hasSize(2);
        assertThat(result.beforeChars()).isZero();
        assertThat(result.afterChars()).isZero();
    }

    @Test
    void shouldRequireReviewCueTwoDistinctRecordsAndNinetyDaySpan() {
        List<MemoryFragment> memories = List.of(
                fragment(21L, "2026-01-01T00:00:00", 80),
                fragment(22L, "2026-05-01T00:00:00", 80));

        assertThat(policy.evaluate(AgentChatMode.REVIEW_CHAT, "以前也有过类似的时候", List.of(), memories)
                .patternEvidence().eligible()).isTrue();
        assertThat(policy.evaluate(AgentChatMode.WRITING_GUIDANCE, "以前也有过类似的时候", List.of(), memories)
                .patternEvidence().eligible()).isFalse();
        assertThat(policy.evaluate(AgentChatMode.REVIEW_CHAT, "今天想聊聊", List.of(), memories)
                .patternEvidence().eligible()).isFalse();
    }

    @Test
    void shouldEnforceRecurrenceSpanDistinctIdAndKnownTimeBoundaries() {
        List<MemoryFragment> ninetyDays = List.of(
                fragment(51L, "2026-01-01T00:00:00", 80),
                fragment(52L, "2026-04-01T00:00:00", 80));
        List<MemoryFragment> eightyNineDays = List.of(
                fragment(51L, "2026-01-01T00:00:00", 80),
                fragment(52L, "2026-03-31T00:00:00", 80));
        List<MemoryFragment> duplicateId = List.of(
                fragment(51L, "2026-01-01T00:00:00", 80),
                fragment(51L, "2026-05-01T00:00:00", 80));
        List<MemoryFragment> unknownOnly = List.of(
                new MemoryFragment(51L, null, "某时", "甲".repeat(80)),
                new MemoryFragment(52L, null, "某时", "甲".repeat(80)));

        assertThat(policy.evaluate(AgentChatMode.REVIEW_CHAT, "又有类似感觉", List.of(), ninetyDays)
                .patternEvidence().eligible()).isTrue();
        assertThat(policy.evaluate(AgentChatMode.REVIEW_CHAT, "又有类似感觉", List.of(), eightyNineDays)
                .patternEvidence().eligible()).isFalse();
        assertThat(policy.evaluate(AgentChatMode.REVIEW_CHAT, "又有类似感觉", List.of(), duplicateId)
                .patternEvidence().eligible()).isFalse();
        assertThat(policy.evaluate(AgentChatMode.REVIEW_CHAT, "又有类似感觉", List.of(), unknownOnly)
                .patternEvidence().eligible()).isFalse();
        assertThat(policy.evaluate(AgentChatMode.REVIEW_CHAT, "又有类似感觉", ninetyDays, List.of())
                .patternEvidence().eligible()).isFalse();
    }

    @Test
    void shouldReturnOriginalFragmentsWhenDisabled() {
        properties.getTemporal().setEnabled(false);
        MemoryFragment old = fragment(31L, "2025-01-01T00:00:00", 100);

        TemporalPolicyResult result = policy.evaluate(
                AgentChatMode.REVIEW_CHAT, "又发生了", List.of(), List.of(old));

        assertThat(result.enabled()).isFalse();
        assertThat(result.injectedFragments().get(0).text()).hasSize(100);
        assertThat(result.contexts()).isEmpty();
        assertThat(result.patternEvidence().eligible()).isFalse();
    }

    @Test
    void shouldRejectInvalidCrossFieldConfiguration() {
        properties.getTemporal().setRecentDays(181);
        properties.getTemporal().setDistantDays(180);

        assertThatThrownBy(() -> new AgentTemporalPolicy(properties, CLOCK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("temporal");

        properties.getTemporal().setRecentDays(30);
        properties.getTemporal().setDistantBudgetPercent(0);
        assertThatThrownBy(() -> new AgentTemporalPolicy(properties, CLOCK))
                .isInstanceOf(IllegalStateException.class);
    }

    private MemoryFragment fragment(long id, String occurredAt, int chars) {
        return new MemoryFragment(id, LocalDateTime.parse(occurredAt), "某时", "甲".repeat(chars));
    }
}
