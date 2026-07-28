package com.flashback.agent.guardrail;

import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 忠实度判定的来源集合（C4）。
 *
 * 语义（design.md §3.1、关键不变量 5）：
 * - 来源**只含当前会话中该用户自己说过的话**；
 * - 不含 Agent 自己的表达——否则 Agent 上一轮说的话会成为下一轮增写的「合法来源」，
 * 忠实度闸会自我失效；
 * - 不含跨记录的历史检索结果——那是 C3 的边界，C4 不越。
 *
 * 为什么全量而非滑动窗口：contextMessageWindow 是给 prompt 的成本约束，
 * 而本类在后端本地跑、零 token 成本。用户第 2 轮说过的话在第 7 轮被整理进正文完全正常，
 * 用窗口会造出「说过但被判虚构」的误伤。
 *
 * 隐私（design.md §6）：本类只存在于内存中，构造出的语料不落库、不写日志、不外发。
 */
public final class AgentSourceCorpus {

    /** 空语料：无任何用户发言时使用。 */
    private static final AgentSourceCorpus EMPTY = new AgentSourceCorpus(Set.of(), 0, 0);

    private final Set<String> ngrams;
    private final int ngramSize;
    private final int sourceLength;

    private AgentSourceCorpus(Set<String> ngrams, int ngramSize, int sourceLength) {
        this.ngrams = ngrams;
        this.ngramSize = ngramSize;
        this.sourceLength = sourceLength;
    }

    /**
     * 由会话历史构造来源集合。
     *
     * @param history   会话消息（正序或乱序均可，本类只筛 role）
     * @param ngramSize n-gram 长度，须为正
     */
    public static AgentSourceCorpus of(List<AgentMessage> history, int ngramSize) {
        if (history == null || history.isEmpty() || ngramSize <= 0) {
            return EMPTY;
        }
        List<String> normalizedParts = new ArrayList<>();
        int totalLength = 0;
        for (AgentMessage message : history) {
            if (message == null || message.getRole() != AgentMessageRole.USER) {
                continue;
            }
            String normalized = AgentTextNormalizer.normalize(message.getContent());
            if (normalized.isEmpty()) {
                continue;
            }
            normalizedParts.add(normalized);
            totalLength += normalized.length();
        }
        if (normalizedParts.isEmpty()) {
            return EMPTY;
        }

        Set<String> ngrams = new HashSet<>();
        for (String part : normalizedParts) {
            collectNgrams(part, ngramSize, ngrams);
        }
        return new AgentSourceCorpus(Set.copyOf(ngrams), ngramSize, totalLength);
    }

    /**
     * 由若干段原始文本构造来源集合。供伪引用检查等场景直接指定来源使用。
     */
    public static AgentSourceCorpus ofTexts(List<String> texts, int ngramSize) {
        if (texts == null || texts.isEmpty() || ngramSize <= 0) {
            return EMPTY;
        }
        Set<String> ngrams = new HashSet<>();
        int totalLength = 0;
        for (String text : texts) {
            String normalized = AgentTextNormalizer.normalize(text);
            if (normalized.isEmpty()) {
                continue;
            }
            totalLength += normalized.length();
            collectNgrams(normalized, ngramSize, ngrams);
        }
        if (ngrams.isEmpty()) {
            return EMPTY;
        }
        return new AgentSourceCorpus(Set.copyOf(ngrams), ngramSize, totalLength);
    }

    private static void collectNgrams(String normalized, int ngramSize, Set<String> sink) {
        if (normalized.length() < ngramSize) {
            // 短于 n-gram 长度的来源整体入集，避免极短发言完全无法命中。
            sink.add(normalized);
            return;
        }
        for (int i = 0; i + ngramSize <= normalized.length(); i++) {
            sink.add(normalized.substring(i, i + ngramSize));
        }
    }

    /**
     * 该 n-gram 是否出现在来源中。
     */
    public boolean containsNgram(String ngram) {
        return ngram != null && ngrams.contains(ngram);
    }

    /**
     * 来源中是否包含该片段（用于短文本整体比对）。
     */
    public boolean containsFragment(String normalizedFragment) {
        if (normalizedFragment == null || normalizedFragment.isEmpty()) {
            return false;
        }
        if (normalizedFragment.length() >= ngramSize) {
            return containsNgram(normalizedFragment.substring(0, ngramSize));
        }
        for (String ngram : ngrams) {
            if (ngram.contains(normalizedFragment)) {
                return true;
            }
        }
        return false;
    }

    public int ngramSize() {
        return ngramSize;
    }

    /** 来源归一化后的总字符数，仅用于结构化指标，不暴露内容。 */
    public int sourceLength() {
        return sourceLength;
    }

    public boolean isEmpty() {
        return ngrams.isEmpty();
    }
}
