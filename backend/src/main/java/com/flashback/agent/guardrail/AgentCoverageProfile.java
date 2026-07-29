package com.flashback.agent.guardrail;

/**
 * 候选文本相对来源集合的逐字覆盖画像（C4）。
 *
 * 存在理由（design.md §3.2、决策 4）：
 * 忠实度判定与诊断检查需要的其实是同一份信息——**候选文本的哪些字有来源、哪些是新增的**。
 * 因此把覆盖标记抽成独立画像：
 * - 忠实度用它算「整体覆盖率」与「最长连续未覆盖片段」；
 * - 诊断 / 代决检查用它把文本分成「有来源区段」与「新增区段」，只在后者匹配规则，
 * 避免把用户自己说的病症词误判成 Agent 下诊断。
 *
 * 判定基于归一化后的文本（见 AgentTextNormalizer），确定性、可复现、零外调。
 */
public final class AgentCoverageProfile {

    private final String normalized;
    private final boolean[] covered;
    private final int coveredCount;
    private final int maxUncoveredRun;

    private AgentCoverageProfile(String normalized, boolean[] covered, int coveredCount, int maxUncoveredRun) {
        this.normalized = normalized;
        this.covered = covered;
        this.coveredCount = coveredCount;
        this.maxUncoveredRun = maxUncoveredRun;
    }

    /**
     * 计算候选文本相对来源集合的覆盖画像。
     *
     * @param candidate 候选文本原文（内部自行归一化）
     * @param corpus    来源集合
     */
    public static AgentCoverageProfile of(String candidate, AgentSourceCorpus corpus) {
        String normalized = AgentTextNormalizer.normalize(candidate);
        int length = normalized.length();
        boolean[] covered = new boolean[length];
        if (length == 0 || corpus == null || corpus.isEmpty()) {
            return new AgentCoverageProfile(normalized, covered, 0, length);
        }

        int ngramSize = corpus.ngramSize();
        if (length < ngramSize) {
            // 候选比 n-gram 还短：整体比对，避免极短文本必然判为无来源。
            if (corpus.containsFragment(normalized)) {
                for (int i = 0; i < length; i++) {
                    covered[i] = true;
                }
            }
        } else {
            for (int i = 0; i + ngramSize <= length; i++) {
                if (!corpus.containsNgram(normalized.substring(i, i + ngramSize))) {
                    continue;
                }
                // 命中的 n-gram 覆盖其全部字符。
                for (int j = i; j < i + ngramSize; j++) {
                    covered[j] = true;
                }
            }
        }

        int coveredCount = 0;
        int maxRun = 0;
        int currentRun = 0;
        for (int i = 0; i < length; i++) {
            if (covered[i]) {
                coveredCount++;
                currentRun = 0;
            } else {
                currentRun++;
                maxRun = Math.max(maxRun, currentRun);
            }
        }
        return new AgentCoverageProfile(normalized, covered, coveredCount, maxRun);
    }

    /**
     * 计算「只被 wider 覆盖、不被 narrower 覆盖」的最长连续片段（C3 新增）。
     *
     * 用途（design.md 决策 2）：narrower 传会话层画像、wider 传两层合并画像，
     * 得到的就是「只来自记忆层」的最长连续片段——即 Agent 正在复述过去内容的长度证据。
     * 这是时间归属检查的唯一输入。
     *
     * 之所以用两个画像相减而不是给本类加第二个位图：两个画像各自都是既有构造路径的产物，
     * 相减是纯读操作，不改变 of() 的行为，也不影响任何既有断言。
     *
     * 两个画像必须来自**同一段候选文本**——归一化是确定性的，因此长度一致即可校验。
     */
    static int longestExclusiveRun(AgentCoverageProfile narrower, AgentCoverageProfile wider) {
        if (narrower == null || wider == null) {
            return 0;
        }
        if (narrower.covered.length != wider.covered.length) {
            // 不同候选文本的画像不可比较；按无片段处理而非抛错，
            // 因为调用方是护栏路径，抛错会把可判定的轮次变成 fail-closed。
            return 0;
        }
        int maxRun = 0;
        int currentRun = 0;
        for (int i = 0; i < wider.covered.length; i++) {
            if (wider.covered[i] && !narrower.covered[i]) {
                currentRun++;
                maxRun = Math.max(maxRun, currentRun);
            } else {
                currentRun = 0;
            }
        }
        return maxRun;
    }

    /** 归一化后的候选文本。仅供同包内判定使用，不对外泄露到日志或审计。 */
    String normalized() {
        return normalized;
    }

    public int length() {
        return normalized.length();
    }

    /**
     * 被来源覆盖的字符比例。空文本视为完全覆盖，交由调用方按「无需判定」处理。
     */
    public double coverage() {
        int length = normalized.length();
        return length == 0 ? 1.0d : (double) coveredCount / length;
    }

    /**
     * 最长连续未覆盖字符数——增写的主判据（design.md §3.2）。
     */
    public int maxUncoveredRun() {
        return maxUncoveredRun;
    }

    /**
     * 指定区间是否**多数字符无来源**，即该区间属于 Agent 新增表述。
     *
     * 用「多数」而非「任一」的理由：n-gram 覆盖在片段边缘天然会有零星命中，
     * 用「任一未覆盖」会把用户原话中的词误判为新增，方向上应偏向不误伤（决策 4）。
     */
    public boolean isMostlyUncovered(int startInclusive, int endExclusive) {
        int start = Math.max(0, startInclusive);
        int end = Math.min(normalized.length(), endExclusive);
        if (start >= end) {
            return false;
        }
        int uncovered = 0;
        for (int i = start; i < end; i++) {
            if (!covered[i]) {
                uncovered++;
            }
        }
        return uncovered * 2 >= (end - start);
    }
}
