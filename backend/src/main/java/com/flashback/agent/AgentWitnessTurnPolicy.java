package com.flashback.agent;

import com.flashback.domain.AgentConversationIntent;
import com.flashback.domain.AgentStage;

import java.util.Objects;

/**
 * P4.1：见证型会话的纯逻辑 turn policy，无 IO、无 provider、无持久化。
 *
 * 复用既有结束关键词与极短回答阈值，避免同一产品语义出现两套分类器。
 */
public final class AgentWitnessTurnPolicy {

    private final AgentStageMachine stageMachine;

    public AgentWitnessTurnPolicy(AgentStageMachine stageMachine) {
        this.stageMachine = Objects.requireNonNull(stageMachine, "stageMachine");
    }

    /** 新写作会话的开场：LISTEN 不问，UNTANGLE 至多问一个。 */
    public AgentWitnessTurnDirective opening(AgentConversationIntent intent) {
        return normalized(intent) == AgentConversationIntent.UNTANGLE
                ? AgentWitnessTurnDirective.mayAskOne(AgentStage.WITNESS)
                : AgentWitnessTurnDirective.reflectOnly(AgentStage.WITNESS);
    }

    /** 写作会话用户提交一轮后的权威策略。 */
    public AgentWitnessTurnDirective decide(
            AgentConversationIntent intent, String userInput, int turnNo, int maxTurns) {
        if (stageMachine.hasFinishIntent(userInput)) {
            return AgentWitnessTurnDirective.close(
                    AgentStage.CLOSING, AgentStageDecision.Reason.USER_FINISH_INTENT);
        }
        if (turnNo >= maxTurns) {
            return AgentWitnessTurnDirective.close(
                    AgentStage.CLOSING, AgentStageDecision.Reason.TURN_LIMIT_REACHED);
        }
        if (normalized(intent) == AgentConversationIntent.LISTEN || stageMachine.isEvasive(userInput)) {
            return AgentWitnessTurnDirective.reflectOnly(AgentStage.WITNESS);
        }
        return AgentWitnessTurnDirective.mayAskOne(AgentStage.WITNESS);
    }

    /** 回看不获得写作 intent，但继承 witness role 与 0/1 问题上限。 */
    public AgentWitnessTurnDirective reviewOpening() {
        return AgentWitnessTurnDirective.mayAskOne(AgentStage.REVIEW);
    }

    /** 回看保持 REVIEW stage；最后一轮或明确停止时返回无问题的 CLOSE 指令。 */
    public AgentWitnessTurnDirective reviewTurn(String userInput, int turnNo, int maxTurns) {
        if (stageMachine.hasFinishIntent(userInput)) {
            return AgentWitnessTurnDirective.close(
                    AgentStage.REVIEW, AgentStageDecision.Reason.USER_FINISH_INTENT);
        }
        if (turnNo >= maxTurns) {
            return AgentWitnessTurnDirective.close(
                    AgentStage.REVIEW, AgentStageDecision.Reason.TURN_LIMIT_REACHED);
        }
        if (stageMachine.isEvasive(userInput)) {
            return AgentWitnessTurnDirective.reflectOnly(AgentStage.REVIEW);
        }
        return AgentWitnessTurnDirective.mayAskOne(AgentStage.REVIEW);
    }

    private AgentConversationIntent normalized(AgentConversationIntent intent) {
        return intent == null ? AgentConversationIntent.LISTEN : intent;
    }
}
