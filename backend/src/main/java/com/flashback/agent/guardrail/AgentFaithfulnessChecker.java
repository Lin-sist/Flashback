package com.flashback.agent.guardrail;

import com.flashback.config.AppAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 忠实度判定（C4 核心）。
 *
 * 要解决的问题（proposal §1、§3）：C2 闸门 3 真实联调实测到模型在工具参数里
 * 增写了用户从未说过的整句，而 C1/C2 两层防御全部放行——
 * 白名单校验「能否执行」、二段式确认校验「用户是否同意」、参数校验「类型与长度」，
 * **没有任何一层在问「这段文字是不是用户说过的」**。
 *
 * 判定形态（design.md §3.2）：**双指标**。
 * - 指标 1 整体覆盖率：候选文本被用户原话覆盖的字符比例；
 * - 指标 2 最长连续未覆盖片段：候选文本中连续无来源的最长字符数。
 *
 * 为什么必须两个——这是本类设计的关键：
 * R1 的形态是「两句真话（约 30 字）+ 一句虚构（约 45 字）」，
 * 即便虚构部分零覆盖，**整体覆盖率仍有约 50%**；
 * 而把覆盖率阈值提到 50% 以上会大面积误伤正常整理（整理会引入连接词、删减重复，
 * 覆盖率本就不可能接近 100%）。
 * 真正能区分两者的是未覆盖片段的**分布形态**：
 * - 正常整理 → 未覆盖处零散分布在接缝上，每处几个字；
 * - 增写 → 一整段连续几十字都无来源。
 * 因此单一整体比例不得作为唯一判据。
 *
 * 性质保证：确定性（同输入必同输出）、零外调（不调用任何模型或外部服务）、可单测。
 * 这三点是蓝图给 C4 的「可持续回归的边界用例集」要求的前提——
 * 不可复现的判定无法回归，也无法在换 model 后判断护栏是否还在。
 */
@Component
public class AgentFaithfulnessChecker {

    private static final Logger log = LoggerFactory.getLogger(AgentFaithfulnessChecker.class);

    /**
     * 引号内片段要求的最低覆盖率。
     *
     * 高于自由文本阈值的理由：引用声称是用户的逐字原话，
     * 不存在「调语序、去口头语」的整理余地——它要么是用户说的，要么不是。
     */
    private static final double QUOTE_MIN_COVERAGE = 0.80d;

    private final AppAgentProperties appAgentProperties;

    public AgentFaithfulnessChecker(AppAgentProperties appAgentProperties) {
        this.appAgentProperties = appAgentProperties;
    }

    /**
     * 判定候选文本是否忠实于来源集合。
     *
     * fail-closed（design.md 关键不变量 2）：判定过程本身异常时返回违规，
     * 绝不因检查失败而放行未检文本。
     *
     * @param candidate 候选文本（会进入用户正文的模型产出）
     * @param corpus    来源集合（本会话用户原话）
     */
    public AgentGuardrailVerdict check(String candidate, AgentSourceCorpus corpus) {
        AppAgentProperties.Guardrail config = appAgentProperties.getGuardrail();
        if (!config.isFaithfulnessEnabled()) {
            // 开关关闭必须留痕，不静默表现为判定通过（backend-core delta 要求）。
            log.info("agent guardrail faithfulness check disabled by config");
            return AgentGuardrailVerdict.pass();
        }
        try {
            return evaluate(candidate, corpus, config);
        } catch (RuntimeException ex) {
            log.warn("agent guardrail faithfulness check failed cause={}", ex.getClass().getSimpleName());
            return AgentGuardrailVerdict.violation(AgentGuardrailViolation.CHECK_ERROR);
        }
    }

    /**
     * 判定一段**声称为用户原话的引用**是否真的来自用户。
     *
     * 比 {@link #check} 更严，两点差异：
     * 1. 不跳过短文本——引用天然可以很短，而实测发现伪造的短引用
     * （「我已经彻底放弃找工作了」，11 字）恰好落在最短受检长度之下，
     * 用通用判据会漏放；
     * 2. 覆盖率要求更高——引用声称是逐字复述，不存在「整理」的余地。
     *
     * 这条更严的判据只用于引号内片段，不影响自由文本的整理空间。
     */
    public AgentGuardrailVerdict checkQuotedFragment(String quoted, AgentSourceCorpus corpus) {
        AppAgentProperties.Guardrail config = appAgentProperties.getGuardrail();
        if (!config.isFaithfulnessEnabled()) {
            return AgentGuardrailVerdict.pass();
        }
        try {
            AgentCoverageProfile profile = profileOf(quoted, corpus);
            int length = profile.length();
            if (length == 0) {
                return AgentGuardrailVerdict.pass();
            }
            if (corpus == null || corpus.isEmpty()) {
                return AgentGuardrailVerdict.violation(
                        AgentGuardrailViolation.UNFAITHFUL, profile.coverage(), profile.maxUncoveredRun(), length);
            }
            boolean faithful = profile.coverage() >= QUOTE_MIN_COVERAGE
                    && profile.maxUncoveredRun() <= config.getMaxUncoveredRun();
            return faithful
                    ? AgentGuardrailVerdict.pass(profile.coverage(), profile.maxUncoveredRun(), length)
                    : AgentGuardrailVerdict.violation(
                            AgentGuardrailViolation.UNFAITHFUL, profile.coverage(), profile.maxUncoveredRun(), length);
        } catch (RuntimeException ex) {
            log.warn("agent guardrail quote fragment check failed cause={}", ex.getClass().getSimpleName());
            return AgentGuardrailVerdict.violation(AgentGuardrailViolation.CHECK_ERROR);
        }
    }

    /**
     * 计算覆盖画像。供诊断检查复用同一份「有来源 / 新增」分区（design.md 决策 4）。
     */
    public AgentCoverageProfile profileOf(String candidate, AgentSourceCorpus corpus) {
        return AgentCoverageProfile.of(candidate, corpus);
    }

    public int ngramSize() {
        return appAgentProperties.getGuardrail().getFaithfulnessNgramSize();
    }

    private AgentGuardrailVerdict evaluate(
            String candidate, AgentSourceCorpus corpus, AppAgentProperties.Guardrail config) {

        // 经由 profileOf 取覆盖画像，与诊断检查共用同一份分区来源（design 决策 4），
        // 同时让 fail-closed 语义可被测试覆盖。
        AgentCoverageProfile profile = profileOf(candidate, corpus);
        int length = profile.length();
        if (length == 0) {
            // 无实质内容：交由调用方按各自路径的空值语义处理，不在此判违规。
            return AgentGuardrailVerdict.pass();
        }

        double coverage = profile.coverage();
        int maxUncoveredRun = profile.maxUncoveredRun();

        if (corpus == null || corpus.isEmpty()) {
            // 用户一句话都没说，却产出了会进正文的文本——不可能忠实。
            return AgentGuardrailVerdict.violation(
                    AgentGuardrailViolation.UNFAITHFUL, coverage, maxUncoveredRun, length);
        }

        // 主判据：连续无来源片段。R1 型增写在此命中。
        if (maxUncoveredRun > config.getMaxUncoveredRun()) {
            return AgentGuardrailVerdict.violation(
                    AgentGuardrailViolation.UNFAITHFUL, coverage, maxUncoveredRun, length);
        }

        // 辅判据：整体覆盖率。短文本跳过，避免小样本抖动（主判据仍已生效）。
        if (length >= config.getMinCheckedLength() && coverage < config.getMinCoverage()) {
            return AgentGuardrailVerdict.violation(
                    AgentGuardrailViolation.UNFAITHFUL, coverage, maxUncoveredRun, length);
        }

        return AgentGuardrailVerdict.pass(coverage, maxUncoveredRun, length);
    }
}
