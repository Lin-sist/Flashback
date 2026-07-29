package com.flashback.agent;

import com.flashback.domain.AgentStage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Agent 引导阶段推进逻辑。
 *
 * 设计约定（design.md 决策 4）：
 * 1. 推进权在后端，不交给模型，保证「追问上限 / 强制收束 / 不逼问」可被单测覆盖；
 * 2. 本类为纯逻辑，无 IO、无持久化、无 provider 调用；
 * 3. 每阶段默认 1 轮；用户回答被判定为回避时，同阶段最多再追问 1 次即前进。
 */
@Component
public class AgentStageMachine {

    /** 同一阶段允许的最大追问次数，超过则前进，避免逼问。 */
    public static final int MAX_REASK_PER_STAGE = 1;

    /** 被判定为「回避型回答」的字符长度阈值（不含空白）。 */
    static final int EVASIVE_ANSWER_LENGTH = 4;

    /** 引导阶段顺序。 */
    private static final List<AgentStage> GUIDING_SEQUENCE = List.of(
            AgentStage.EMOTION,
            AgentStage.CONFUSION,
            AgentStage.CORE_QUESTION,
            AgentStage.EXPECTATION);

    /** 明确的结束意图关键词。 */
    private static final List<String> FINISH_INTENT_KEYWORDS = List.of(
            "不想聊", "不聊了", "先这样", "就这样", "结束", "算了", "停下", "不说了", "够了", "到这里");

    /**
     * 会话开启时的首个引导阶段。
     */
    public AgentStage firstStage() {
        return AgentStage.EMOTION;
    }

    /**
     * 计算用户提交一轮回答后的目标阶段。
     *
     * @param currentStage    当前阶段
     * @param userInput       用户本轮输入
     * @param stageReaskCount 当前阶段已追问次数
     * @param turnCount       该轮计入后的累计轮次
     * @param maxTurns        会话轮次上限
     */
    public AgentStageDecision decide(
            AgentStage currentStage,
            String userInput,
            int stageReaskCount,
            int turnCount,
            int maxTurns) {
        // C3b：REVIEW 与 ENDED 一样不可推进。回看会话刻意不经本状态机
        // （design 决策 2），若它走到这里说明编排层的模式判定出了问题，
        // 应当快速失败而不是静默按引导阶段处理。
        if (currentStage == null || currentStage == AgentStage.ENDED || currentStage == AgentStage.REVIEW) {
            throw new IllegalArgumentException("stage not advanceable: " + currentStage);
        }

        // 用户明确表达结束意图：立即收束，优先级高于一切推进规则。
        if (hasFinishIntent(userInput)) {
            return new AgentStageDecision(AgentStage.CLOSING, 0, AgentStageDecision.Reason.USER_FINISH_INTENT);
        }

        // 达到轮次上限：强制收束，不无限延长对话。
        if (turnCount >= maxTurns) {
            return new AgentStageDecision(AgentStage.CLOSING, 0, AgentStageDecision.Reason.TURN_LIMIT_REACHED);
        }

        // 已在收束阶段：本轮之后结束。
        if (currentStage == AgentStage.CLOSING) {
            return new AgentStageDecision(AgentStage.ENDED, 0, AgentStageDecision.Reason.CLOSED);
        }

        // 开场阶段的第一轮回答按第一个引导阶段处理。
        AgentStage effectiveStage = currentStage == AgentStage.OPENING ? firstStage() : currentStage;

        // 回避型回答且该阶段追问未用尽：同阶段再问一次。
        if (isEvasive(userInput) && stageReaskCount < MAX_REASK_PER_STAGE) {
            return new AgentStageDecision(effectiveStage, stageReaskCount + 1, AgentStageDecision.Reason.REASK);
        }

        return new AgentStageDecision(nextOf(effectiveStage), 0, AgentStageDecision.Reason.ADVANCE);
    }

    /**
     * 判断用户输入是否表达了结束意图。
     */
    public boolean hasFinishIntent(String userInput) {
        String normalized = normalize(userInput);
        if (normalized.isEmpty()) {
            return false;
        }
        return FINISH_INTENT_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    /**
     * 判断是否为回避型极短回答（例如「嗯」「不知道」）。
     */
    public boolean isEvasive(String userInput) {
        return normalize(userInput).replaceAll("\\s", "").length() <= EVASIVE_ANSWER_LENGTH;
    }

    private AgentStage nextOf(AgentStage stage) {
        int index = GUIDING_SEQUENCE.indexOf(stage);
        if (index < 0 || index == GUIDING_SEQUENCE.size() - 1) {
            return AgentStage.CLOSING;
        }
        return GUIDING_SEQUENCE.get(index + 1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
