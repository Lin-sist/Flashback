package com.flashback.agent.temporal;

import com.flashback.agent.AgentChatMode;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.config.AppAgentProperties;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * C9 时间策略：给记忆附加时间距离、衰减远期辅助片段，并在证据充分时允许一句克制提示。
 */
public class AgentTemporalPolicy {

    private static final List<String> RECURRENCE_CUES = List.of(
            "又", "再次", "以前也", "之前也", "反复", "总是", "不止一次", "类似", "重复");

    private final AppAgentProperties properties;
    private final Clock clock;

    public AgentTemporalPolicy(AppAgentProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        validate(properties.getTemporal());
    }

    public TemporalPolicyResult evaluate(
            AgentChatMode mode,
            String latestUserInput,
            List<MemoryFragment> focalFragments,
            List<MemoryFragment> ancillaryFragments) {
        List<MemoryFragment> focal = safe(focalFragments);
        List<MemoryFragment> ancillary = safe(ancillaryFragments);
        AppAgentProperties.Temporal config = properties.getTemporal();
        validate(config);

        int beforeChars = chars(focal) + chars(ancillary);
        if (!config.isEnabled()) {
            List<MemoryFragment> unchanged = merge(focal, ancillary);
            return new TemporalPolicyResult(false, unchanged, List.of(),
                    TemporalPatternEvidence.absent(), beforeChars, beforeChars);
        }

        LocalDate today = LocalDate.now(clock);
        List<MemoryFragment> decayedAncillary = ancillary.stream()
                .map(fragment -> decay(fragment, bandOf(fragment, today, config), config))
                .toList();
        List<MemoryFragment> injected = merge(focal, decayedAncillary);

        List<TemporalMemoryContext> contexts = new ArrayList<>();
        focal.forEach(fragment -> contexts.add(contextOf(fragment, true, today, config)));
        decayedAncillary.forEach(fragment -> contexts.add(contextOf(fragment, false, today, config)));

        TemporalPatternEvidence pattern = recurrenceEvidence(
                mode, latestUserInput, ancillary, config, today);
        return new TemporalPolicyResult(true, injected, contexts, pattern,
                beforeChars, chars(injected));
    }

    public String fingerprintSource() {
        return fingerprintSource(properties.getTemporal());
    }

    public static String fingerprintSource(AppAgentProperties.Temporal c) {
        return "temporal:v1:" + c.isEnabled() + ':' + c.getRecentDays() + ':' + c.getDistantDays()
                + ':' + c.getDistantBudgetPercent() + ':' + c.getLongAgoBudgetPercent()
                + ':' + c.getMinFragmentChars() + ':' + c.getRecurrenceMinSpanDays();
    }

    private TemporalPatternEvidence recurrenceEvidence(
            AgentChatMode mode,
            String latestUserInput,
            List<MemoryFragment> ancillary,
            AppAgentProperties.Temporal config,
            LocalDate today) {
        if (mode != AgentChatMode.REVIEW_CHAT || latestUserInput == null
                || RECURRENCE_CUES.stream().noneMatch(latestUserInput::contains)) {
            return TemporalPatternEvidence.absent();
        }
        List<MemoryFragment> known = ancillary.stream()
                .filter(fragment -> fragment.recordId() != null && fragment.occurredAt() != null)
                .filter(fragment -> !fragment.occurredAt().toLocalDate().isAfter(today))
                .filter(distinctByRecordId())
                .sorted(Comparator.comparing(MemoryFragment::occurredAt))
                .toList();
        if (known.size() < 2) {
            return new TemporalPatternEvidence(false, known.size(), 0L);
        }
        long span = ChronoUnit.DAYS.between(
                known.get(0).occurredAt().toLocalDate(),
                known.get(known.size() - 1).occurredAt().toLocalDate());
        return new TemporalPatternEvidence(span >= config.getRecurrenceMinSpanDays(), known.size(), span);
    }

    private java.util.function.Predicate<MemoryFragment> distinctByRecordId() {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        return fragment -> seen.add(fragment.recordId());
    }

    private TemporalMemoryContext contextOf(
            MemoryFragment fragment,
            boolean focal,
            LocalDate today,
            AppAgentProperties.Temporal config) {
        TemporalDistance distance = distanceOf(fragment, today, config);
        return new TemporalMemoryContext(fragment.recordId(), distance.timeLabel(),
                distance.band(), distance.distanceDays(), focal);
    }

    private TemporalDistance distanceOf(
            MemoryFragment fragment,
            LocalDate today,
            AppAgentProperties.Temporal config) {
        TemporalDistanceBand band = bandOf(fragment, today, config);
        Long days = fragment.occurredAt() == null || band == TemporalDistanceBand.UNKNOWN
                ? null
                : ChronoUnit.DAYS.between(fragment.occurredAt().toLocalDate(), today);
        return new TemporalDistance(fragment.occurredAt(), fragment.timeLabel(), days, band);
    }

    private TemporalDistanceBand bandOf(
            MemoryFragment fragment,
            LocalDate today,
            AppAgentProperties.Temporal config) {
        if (fragment.occurredAt() == null || fragment.occurredAt().toLocalDate().isAfter(today)) {
            return TemporalDistanceBand.UNKNOWN;
        }
        long days = ChronoUnit.DAYS.between(fragment.occurredAt().toLocalDate(), today);
        if (days <= config.getRecentDays()) {
            return TemporalDistanceBand.RECENT;
        }
        if (days <= config.getDistantDays()) {
            return TemporalDistanceBand.DISTANT;
        }
        return TemporalDistanceBand.LONG_AGO;
    }

    private MemoryFragment decay(
            MemoryFragment fragment,
            TemporalDistanceBand band,
            AppAgentProperties.Temporal config) {
        int percent = switch (band) {
            case DISTANT -> config.getDistantBudgetPercent();
            case LONG_AGO, UNKNOWN -> config.getLongAgoBudgetPercent();
            default -> 100;
        };
        String text = fragment.text();
        if (text == null || percent == 100) {
            return fragment;
        }
        int base = properties.getMemory().getMaxFragmentChars();
        int effectiveBudget = Math.min(base, Math.max(config.getMinFragmentChars(), base * percent / 100));
        int target = Math.min(text.length(), effectiveBudget);
        return target == text.length() ? fragment : new MemoryFragment(
                fragment.recordId(), fragment.occurredAt(), fragment.timeLabel(),
                text.substring(0, target), fragment.contextNote());
    }

    private void validate(AppAgentProperties.Temporal config) {
        if (config.getRecentDays() <= 0
                || config.getDistantDays() <= 0
                || config.getRecentDays() >= config.getDistantDays()
                || config.getDistantBudgetPercent() <= 0
                || config.getDistantBudgetPercent() > 100
                || config.getLongAgoBudgetPercent() <= 0
                || config.getLongAgoBudgetPercent() > 100
                || config.getMinFragmentChars() <= 0
                || config.getRecurrenceMinSpanDays() <= 0) {
            throw new IllegalStateException("Invalid app.agent.temporal configuration");
        }
    }

    private List<MemoryFragment> safe(List<MemoryFragment> fragments) {
        return fragments == null ? List.of() : fragments.stream().filter(Objects::nonNull).toList();
    }

    private List<MemoryFragment> merge(List<MemoryFragment> focal, List<MemoryFragment> ancillary) {
        List<MemoryFragment> merged = new ArrayList<>(focal.size() + ancillary.size());
        merged.addAll(focal);
        merged.addAll(ancillary);
        return List.copyOf(merged);
    }

    private int chars(List<MemoryFragment> fragments) {
        return fragments.stream().map(MemoryFragment::text).filter(Objects::nonNull).mapToInt(String::length).sum();
    }
}
