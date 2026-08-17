package com.flashback.agent;

import com.flashback.domain.AgentStage;

/**
 * P4.1：编排层传给 prompt 与回复护栏的 typed turn contract。
 *
 * @param type         本轮回复策略
 * @param maxQuestions 后端允许的最大问题数，只能为 0 或 1
 * @param nextStage    本轮持久化后的 session stage
 * @param reason       结构化判定原因
 */
public record AgentWitnessTurnDirective(
        AgentWitnessTurnType type,
        int maxQuestions,
        AgentStage nextStage,
        AgentStageDecision.Reason reason) {

    public AgentWitnessTurnDirective {
        if (type == null || nextStage == null || reason == null) {
            throw new IllegalArgumentException("witness turn directive fields must not be null");
        }
        if (maxQuestions < 0 || maxQuestions > 1) {
            throw new IllegalArgumentException("maxQuestions must be 0 or 1");
        }
        if (type != AgentWitnessTurnType.MAY_ASK_ONE && maxQuestions != 0) {
            throw new IllegalArgumentException("only MAY_ASK_ONE may allow a question");
        }
    }

    /**
     * 历史调用方的安全缺省。生产写作编排会显式传入 policy 结果；
     * legacy stage 只保留读取/测试兼容，不再由新 session 产生。
     */
    public static AgentWitnessTurnDirective safeDefault(AgentStage stage) {
        if (stage == AgentStage.CLOSING || stage == AgentStage.ENDED) {
            return close(stage == AgentStage.ENDED ? AgentStage.ENDED : AgentStage.CLOSING,
                    AgentStageDecision.Reason.CLOSED);
        }
        if (stage == AgentStage.WITNESS) {
            return reflectOnly(AgentStage.WITNESS);
        }
        if (stage == AgentStage.REVIEW) {
            return mayAskOne(AgentStage.REVIEW);
        }
        return mayAskOne(stage == null ? AgentStage.WITNESS : stage);
    }

    public static AgentWitnessTurnDirective reflectOnly(AgentStage nextStage) {
        return new AgentWitnessTurnDirective(
                AgentWitnessTurnType.REFLECT_ONLY,
                0,
                nextStage,
                AgentStageDecision.Reason.WITNESS_RETAINED);
    }

    public static AgentWitnessTurnDirective mayAskOne(AgentStage nextStage) {
        return new AgentWitnessTurnDirective(
                AgentWitnessTurnType.MAY_ASK_ONE,
                1,
                nextStage,
                AgentStageDecision.Reason.WITNESS_RETAINED);
    }

    public static AgentWitnessTurnDirective close(
            AgentStage nextStage, AgentStageDecision.Reason reason) {
        return new AgentWitnessTurnDirective(AgentWitnessTurnType.CLOSE, 0, nextStage, reason);
    }
}
