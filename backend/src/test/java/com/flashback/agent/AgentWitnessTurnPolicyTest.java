package com.flashback.agent;

import com.flashback.domain.AgentConversationIntent;
import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentWitnessTurnPolicyTest {

    private final AgentWitnessTurnPolicy policy = new AgentWitnessTurnPolicy(new AgentStageMachine());

    @Test
    void listenMustAlwaysReflectWithoutQuestions() {
        AgentWitnessTurnDirective directive = policy.decide(
                AgentConversationIntent.LISTEN, "最近几件事情都压在一起", 1, 4);

        assertThat(directive.type()).isEqualTo(AgentWitnessTurnType.REFLECT_ONLY);
        assertThat(directive.maxQuestions()).isZero();
        assertThat(directive.nextStage()).isEqualTo(AgentStage.WITNESS);
    }

    @Test
    void untangleMayAskOneButBriefInputMustNotBePressed() {
        assertThat(policy.decide(
                AgentConversationIntent.UNTANGLE, "最近几件事情都压在一起", 1, 4).maxQuestions())
                .isEqualTo(1);
        AgentWitnessTurnDirective brief = policy.decide(
                AgentConversationIntent.UNTANGLE, "嗯", 1, 4);
        assertThat(brief.type()).isEqualTo(AgentWitnessTurnType.REFLECT_ONLY);
        assertThat(brief.maxQuestions()).isZero();
    }

    @Test
    void finishIntentAndTurnLimitMustCloseWithoutQuestions() {
        AgentWitnessTurnDirective finish = policy.decide(
                AgentConversationIntent.UNTANGLE, "先这样吧", 1, 4);
        AgentWitnessTurnDirective limit = policy.decide(
                AgentConversationIntent.UNTANGLE, "还有一点", 4, 4);

        assertThat(finish.nextStage()).isEqualTo(AgentStage.CLOSING);
        assertThat(finish.reason()).isEqualTo(AgentStageDecision.Reason.USER_FINISH_INTENT);
        assertThat(limit.reason()).isEqualTo(AgentStageDecision.Reason.TURN_LIMIT_REACHED);
        assertThat(finish.maxQuestions()).isZero();
        assertThat(limit.maxQuestions()).isZero();
    }
}
