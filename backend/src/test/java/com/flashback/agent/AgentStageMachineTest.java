package com.flashback.agent;

import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentStageMachineTest {

    private static final int MAX_TURNS = 8;

    private final AgentStageMachine machine = new AgentStageMachine();

    @Test
    void shouldStartFromEmotionStage() {
        assertThat(machine.firstStage()).isEqualTo(AgentStage.EMOTION);
    }

    @Test
    void shouldAdvanceThroughGuidingSequence() {
        AgentStageDecision fromOpening = machine.decide(
                AgentStage.OPENING, "工作上有点撑不住，最近老是睡不好", 0, 1, MAX_TURNS);
        assertThat(fromOpening.nextStage()).isEqualTo(AgentStage.CONFUSION);
        assertThat(fromOpening.reason()).isEqualTo(AgentStageDecision.Reason.ADVANCE);

        assertThat(machine.decide(AgentStage.EMOTION, "有点焦虑，说不太清楚原因", 0, 2, MAX_TURNS).nextStage())
                .isEqualTo(AgentStage.CONFUSION);
        assertThat(machine.decide(AgentStage.CONFUSION, "主要是不知道该先做哪件事", 0, 3, MAX_TURNS).nextStage())
                .isEqualTo(AgentStage.CORE_QUESTION);
        assertThat(machine.decide(AgentStage.CORE_QUESTION, "我到底该不该换个方向", 0, 4, MAX_TURNS).nextStage())
                .isEqualTo(AgentStage.EXPECTATION);
        assertThat(machine.decide(AgentStage.EXPECTATION, "希望三个月后能踏实一点", 0, 5, MAX_TURNS).nextStage())
                .isEqualTo(AgentStage.CLOSING);
    }

    @Test
    void shouldReaskOnceWhenAnswerIsEvasive() {
        AgentStageDecision first = machine.decide(AgentStage.EMOTION, "嗯", 0, 1, MAX_TURNS);

        assertThat(first.nextStage()).isEqualTo(AgentStage.EMOTION);
        assertThat(first.stageReaskCount()).isEqualTo(1);
        assertThat(first.reason()).isEqualTo(AgentStageDecision.Reason.REASK);
    }

    @Test
    void shouldAdvanceInsteadOfPressingAfterReaskLimit() {
        AgentStageDecision second = machine.decide(AgentStage.EMOTION, "不知道", 1, 2, MAX_TURNS);

        assertThat(second.nextStage()).isEqualTo(AgentStage.CONFUSION);
        assertThat(second.stageReaskCount()).isZero();
        assertThat(second.reason()).isEqualTo(AgentStageDecision.Reason.ADVANCE);
    }

    @Test
    void shouldCloseImmediatelyWhenUserWantsToStop() {
        AgentStageDecision decision = machine.decide(AgentStage.CONFUSION, "今天不想聊了", 0, 2, MAX_TURNS);

        assertThat(decision.nextStage()).isEqualTo(AgentStage.CLOSING);
        assertThat(decision.reason()).isEqualTo(AgentStageDecision.Reason.USER_FINISH_INTENT);
        assertThat(decision.isClosing()).isTrue();
    }

    @Test
    void shouldForceClosingWhenTurnLimitReached() {
        AgentStageDecision decision = machine.decide(
                AgentStage.EMOTION, "还有很多话想说，感觉说不完", 0, MAX_TURNS, MAX_TURNS);

        assertThat(decision.nextStage()).isEqualTo(AgentStage.CLOSING);
        assertThat(decision.reason()).isEqualTo(AgentStageDecision.Reason.TURN_LIMIT_REACHED);
    }

    @Test
    void shouldPrioritizeFinishIntentOverTurnLimit() {
        AgentStageDecision decision = machine.decide(AgentStage.EMOTION, "算了", 0, MAX_TURNS, MAX_TURNS);

        assertThat(decision.reason()).isEqualTo(AgentStageDecision.Reason.USER_FINISH_INTENT);
    }

    @Test
    void shouldEndAfterClosingStage() {
        AgentStageDecision decision = machine.decide(AgentStage.CLOSING, "谢谢你陪我说这些话", 0, 6, MAX_TURNS);

        assertThat(decision.nextStage()).isEqualTo(AgentStage.ENDED);
        assertThat(decision.isEnded()).isTrue();
        assertThat(decision.reason()).isEqualTo(AgentStageDecision.Reason.CLOSED);
    }

    @Test
    void shouldRejectAdvancingEndedSession() {
        assertThatThrownBy(() -> machine.decide(AgentStage.ENDED, "还能再聊吗", 0, 3, MAX_TURNS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDetectEvasiveAndFinishIntent() {
        assertThat(machine.isEvasive("嗯")).isTrue();
        assertThat(machine.isEvasive("不知道")).isTrue();
        assertThat(machine.isEvasive("最近状态挺差的")).isFalse();
        assertThat(machine.hasFinishIntent("先这样吧")).isTrue();
        assertThat(machine.hasFinishIntent("我想继续说说")).isFalse();
    }
}
