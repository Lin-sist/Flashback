package com.flashback.agent.reflection;

import com.flashback.agent.guardrail.AgentGuardrailViolation;
import com.flashback.domain.AgentStage;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * C7 的封闭 reflection policy。
 *
 * 输入只有阶段与违规枚举，类型上无法接收候选文本、用户原话、记忆片段或 prompt。
 * CLOSING 后还要生成 material，若此处开环会让单轮达到三次 provider 调用，故明确排除。
 */
@Component
public final class AgentReflectionPolicy {

    public static final int MAX_REFLECTION_REWRITES = 1;

    private static final String MISSING_TIME_ATTRIBUTION_INSTRUCTION = """
            请重新生成这一轮回复，并明确说明你提到的是用户过去某个时候写下或表达过的内容，
            不要把过去的表达说成用户此刻刚刚说过的话。保持原有语气与长度，不要增加新的事实、判断或行动建议。
            """.trim();

    private static final String EXCESSIVE_QUESTIONS_ZERO_INSTRUCTION = """
            请重新生成这一轮回复。先回应用户已经表达的内容，不要提出任何问题；
            不要增加新的事实、判断、关系承诺、行动建议或结论。
            """.trim();

    private static final String EXCESSIVE_QUESTIONS_ONE_INSTRUCTION = """
            请重新生成这一轮回复。先回应用户已经表达的内容，至多提出一个具体且可跳过的问题；
            没有必要可以不问，不要增加新的事实、判断、关系承诺、行动建议或结论。
            """.trim();

    /** 返回固定指令；empty 表示该阶段/违规不得开环。 */
    public Optional<String> instructionFor(AgentStage stage, AgentGuardrailViolation violation) {
        return instructionFor(stage, violation, 1);
    }

    /**
     * P4.1：问题数违规只接收 typed violation 与 0/1 上限，不接收候选或用户文本。
     */
    public Optional<String> instructionFor(
            AgentStage stage, AgentGuardrailViolation violation, int maxQuestions) {
        if (stage == null
                || stage == AgentStage.CLOSING) {
            return Optional.empty();
        }
        if (violation == AgentGuardrailViolation.MISSING_TIME_ATTRIBUTION) {
            return Optional.of(MISSING_TIME_ATTRIBUTION_INSTRUCTION);
        }
        if (violation == AgentGuardrailViolation.EXCESSIVE_QUESTIONS) {
            if (maxQuestions == 0) {
                return Optional.of(EXCESSIVE_QUESTIONS_ZERO_INSTRUCTION);
            }
            if (maxQuestions == 1) {
                return Optional.of(EXCESSIVE_QUESTIONS_ONE_INSTRUCTION);
            }
        }
        return Optional.empty();
    }

    /** 供版本锚点覆盖所有会进入 reflection prompt 的固定文案。 */
    public String fingerprintSource() {
        return "missing-time-attribution=" + MISSING_TIME_ATTRIBUTION_INSTRUCTION
                + "\nexcessive-questions-0=" + EXCESSIVE_QUESTIONS_ZERO_INSTRUCTION
                + "\nexcessive-questions-1=" + EXCESSIVE_QUESTIONS_ONE_INSTRUCTION
                + "\nmax-rewrites=" + MAX_REFLECTION_REWRITES
                + "\nclosing=disabled";
    }
}
