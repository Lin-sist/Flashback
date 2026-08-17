package com.flashback.agent;

import com.flashback.domain.AgentStage;

/**
 * 阶段推进结果。
 *
 * @param nextStage       推进后的阶段
 * @param stageReaskCount 推进后该阶段已追问次数
 * @param reason          推进原因，便于测试与后续可观测（C5）复用
 */
public record AgentStageDecision(AgentStage nextStage, int stageReaskCount, Reason reason) {

    public enum Reason {
        /** 正常前进到下一阶段。 */
        ADVANCE,
        /** 同阶段追问一次。 */
        REASK,
        /** 用户明确表达结束意图。 */
        USER_FINISH_INTENT,
        /** 达到轮次上限强制收束。 */
        TURN_LIMIT_REACHED,
        /** P4.1：见证型会话保持当前 WITNESS/REVIEW 语义。 */
        WITNESS_RETAINED,
        /** 收束完成，会话结束。 */
        CLOSED
    }

    public boolean isClosing() {
        return nextStage == AgentStage.CLOSING;
    }

    public boolean isEnded() {
        return nextStage == AgentStage.ENDED;
    }
}
