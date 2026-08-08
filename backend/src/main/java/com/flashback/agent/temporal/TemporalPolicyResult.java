package com.flashback.agent.temporal;

import com.flashback.agent.memory.MemoryFragment;

import java.util.List;

/** C9 时间策略的不可变结果。 */
public record TemporalPolicyResult(
        boolean enabled,
        List<MemoryFragment> injectedFragments,
        List<TemporalMemoryContext> contexts,
        TemporalPatternEvidence patternEvidence,
        int beforeChars,
        int afterChars) {

    public TemporalPolicyResult {
        injectedFragments = List.copyOf(injectedFragments);
        contexts = List.copyOf(contexts);
    }
}
