package com.flashback.agent.guardrail;

import com.flashback.config.AppAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 时间归属判定（C3 agent-memory-retrieval 的新护栏）。
 *
 * 要防的具体危害（design.md 决策 2）：Memory 让 Agent 能复述用户过去写下的话。
 * 一旦它复述时不说清「那是哪个时候的事」，用户读到的就是一句
 * 听起来像自己刚刚说过、实际上是三个月前说过的话。
 * 这不是措辞问题——它篡改了用户对自己时间线的感知，
 * 而产品的全部承诺都建立在「时间感」上。
 *
 * 判定形态：
 * 1. 用分层语料算出候选文本中「只被记忆层覆盖、不被会话层覆盖」的最长连续片段；
 * 2. 该片段短于阈值 → 视为措辞巧合，放行（用户此刻与过去用了同一个常见短语很正常）；
 * 3. 该片段达到阈值 → 认定 Agent 正在复述过去，此时文本中必须出现时间归属表述，
 * 否则判违规。
 *
 * 为什么不用模型判断「这句话有没有时间归属」：C4 已经把护栏标准定成
 * 确定性、零外调、可单测（否则无法写回归用例、换 model 后无从验证护栏还在），
 * 并明确否决了 LLM-as-judge 方向。本类严格沿用该标准。
 *
 * 误伤方向是刻意选的：词表收得宽、阈值定得高，宁可放过一句表达生硬的合法回复，
 * 也不要把正常的回忆句判成违规——被误伤的后果是用户收到一句兜底回复，
 * 而那会让 Agent 显得突然失忆。
 */
@Component
public class AgentTimeAttributionChecker {

    private static final Logger log = LoggerFactory.getLogger(AgentTimeAttributionChecker.class);

    private final AppAgentProperties appAgentProperties;

    public AgentTimeAttributionChecker(AppAgentProperties appAgentProperties) {
        this.appAgentProperties = appAgentProperties;
    }

    /**
     * 判定 Agent 表述是否在复述记忆内容时给出了时间归属。
     *
     * fail-closed：判定过程异常时返回违规，不放行未检文本。
     *
     * @param text   Agent 的表述（对话回复或提议话术）
     * @param corpus 分层来源集合；无记忆层时恒判通过
     */
    public AgentGuardrailVerdict check(String text, AgentLayeredCorpus corpus) {
        if (!appAgentProperties.getGuardrail().isContentCheckEnabled()) {
            // 与既有后置检查共用开关，避免出现「一半检查开着一半关着」的中间态。
            log.info("agent guardrail time attribution check disabled by config");
            return AgentGuardrailVerdict.pass();
        }
        if (text == null || text.isBlank() || corpus == null || !corpus.hasMemory()) {
            // 没有记忆层就没有「过去的话」可冒充，等价于 C4 现状。
            return AgentGuardrailVerdict.pass();
        }
        try {
            int memoryOnlyRun = memoryOnlyRunOf(text, corpus);
            int threshold = appAgentProperties.getGuardrail().getMinMemoryOnlyRunForAttribution();
            if (memoryOnlyRun < threshold) {
                return AgentGuardrailVerdict.pass();
            }
            if (hasTimeAttribution(text)) {
                return AgentGuardrailVerdict.pass();
            }
            // 指标位复用既有字段语义：maxUncoveredRun 位记录 memory-only 片段长度，
            // 便于痕迹里读出「复述了多长的旧内容」。仍然只有数值，不含文本。
            return AgentGuardrailVerdict.violation(
                    AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION,
                    0.0d,
                    memoryOnlyRun,
                    AgentTextNormalizer.normalize(text).length());
        } catch (RuntimeException ex) {
            log.warn("agent guardrail time attribution check failed cause={}", ex.getClass().getSimpleName());
            return AgentGuardrailVerdict.violation(AgentGuardrailViolation.CHECK_ERROR);
        }
    }

    /**
     * 计算「仅记忆层覆盖」的最长连续片段。
     *
     * 单独抽出的理由：它是本类唯一可能抛异常的计算步骤，
     * 抽出后 fail-closed 行为可以被测试真实触发，而不用靠构造畸形输入去碰运气。
     */
    int memoryOnlyRunOf(String text, AgentLayeredCorpus corpus) {
        return corpus.longestMemoryOnlyRun(text);
    }

    /**
     * 文本中是否出现时间归属表述。
     *
     * 与规则匹配的一致性要求：规则词与文本都要归一化后比较，
     * 否则规则里的标点会与归一化文本对不上（沿用 AgentContentChecker 的既有做法）。
     */
    private boolean hasTimeAttribution(String text) {
        String normalized = AgentTextNormalizer.normalize(text);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String term : AgentGuardrailRules.TIME_ATTRIBUTION_TERMS) {
            String normalizedTerm = AgentTextNormalizer.normalize(term);
            if (!normalizedTerm.isEmpty() && normalized.contains(normalizedTerm)) {
                return true;
            }
        }
        return false;
    }
}
