package com.flashback.agent.eval;

import com.flashback.agent.AgentRawToolCall;
import com.flashback.agent.resilience.AgentProviderFailureCategory;
import com.flashback.agent.trace.AgentTraceCollector;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 一条用例跑完之后的结果（C6）。
 *
 * 把「编排 → 驱动 → 收集」这段固定动作收在一处，使不变量断言与快照派生
 * 面对的是同一份结果对象，不会各自再跑一遍（那样两者就可能看到不同的运行）。
 */
final class AgentEvalRun {

    private final AgentEvalCase evalCase;
    private final AgentEvalHarness harness;
    private final List<AgentTraceCollector> traces;

    private AgentEvalRun(AgentEvalCase evalCase, AgentEvalHarness harness) {
        this.evalCase = evalCase;
        this.harness = harness;
        this.traces = harness.sink().traces();
    }

    /**
     * 按用例编排 provider 响应，逐轮驱动，返回结果。
     */
    static AgentEvalRun execute(AgentEvalCase evalCase) {
        AgentEvalHarness harness = evalCase.newHarness();

        for (AgentEvalCase.Turn turn : evalCase.turns()) {
            if (turn.reply() == null) {
                // 未编排回复 = 本轮 provider 失败。用固定异常类型，
                // 使 causeType 可被断言（轨迹只记类型不记消息）。
                harness.client().scriptReply(ScriptedAgentModelClient.Scripted.failure(
                        AgentProviderFailureCategory.UPSTREAM_UNAVAILABLE));
            } else if (turn.toolCall() != null) {
                harness.client().scriptReply(ScriptedAgentModelClient.Scripted.replyWithTool(
                        turn.reply(),
                        new AgentRawToolCall(turn.toolCall().name(), turn.toolCall().argumentsJson())));
            } else {
                harness.client().scriptReply(ScriptedAgentModelClient.Scripted.reply(turn.reply()));
            }
            if (turn.reflectionReply() != null) {
                harness.client().scriptReply(
                        ScriptedAgentModelClient.Scripted.reply(turn.reflectionReply()));
            } else if (turn.reflectionFailure()) {
                harness.client().scriptReply(ScriptedAgentModelClient.Scripted.failure(
                        AgentProviderFailureCategory.UPSTREAM_UNAVAILABLE));
            }
            if (turn.material() != null) {
                harness.client().scriptMaterial(turn.material());
            }
        }

        for (AgentEvalCase.Turn turn : evalCase.turns()) {
            harness.turn(turn.userInput());
        }
        return new AgentEvalRun(evalCase, harness);
    }

    AgentEvalCase evalCase() {
        return evalCase;
    }

    AgentEvalHarness harness() {
        return harness;
    }

    List<AgentTraceCollector> traces() {
        return traces;
    }

    /**
     * 最后一轮的轨迹。多数期望针对它——用例的最后一轮才是它想验的那一轮。
     */
    AgentTraceCollector lastTrace() {
        return traces.isEmpty() ? null : traces.get(traces.size() - 1);
    }

    // ---------- 轨迹读取 ----------

    List<String> stepTypes(AgentTraceCollector trace) {
        List<String> types = new ArrayList<>();
        for (Map<String, Object> step : trace.steps()) {
            types.add(String.valueOf(step.get("step")));
        }
        return List.copyOf(types);
    }

    /**
     * 取某个步骤类型下某个键的值；不存在返回 null。
     */
    Object stepValue(AgentTraceCollector trace, String stepType, String key) {
        for (Map<String, Object> step : trace.steps()) {
            if (stepType.equals(step.get("step")) && step.containsKey(key)) {
                return step.get(key);
            }
        }
        return null;
    }

    boolean hasStep(AgentTraceCollector trace, String stepType) {
        return stepTypes(trace).contains(stepType);
    }

    /**
     * 各轮的阶段判定结论序列。回看轮不产生 stage-decision，故不计入。
     */
    List<String> stageReasons() {
        List<String> reasons = new ArrayList<>();
        for (AgentTraceCollector trace : traces) {
            Object reason = stepValue(trace, "stage-decision", "reason");
            if (reason != null) {
                reasons.add(String.valueOf(reason));
            }
        }
        return List.copyOf(reasons);
    }

    /**
     * 各轮的目标阶段序列。
     */
    List<String> stagePath() {
        List<String> path = new ArrayList<>();
        for (AgentTraceCollector trace : traces) {
            Object to = stepValue(trace, "stage-decision", "to");
            if (to != null) {
                path.add(String.valueOf(to));
            }
        }
        return List.copyOf(path);
    }

    /**
     * 同一阶段最多连续被追问几次。
     *
     * 判据取「连续出现 REASK 的最长长度」而非「REASK 总数」：
     * 不同阶段各追问一次是合法的，只有同阶段连续追问才受上限约束。
     */
    int maxConsecutiveReask() {
        int max = 0;
        int current = 0;
        for (String reason : stageReasons()) {
            if ("REASK".equals(reason)) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 0;
            }
        }
        return max;
    }

    /**
     * 最后一轮 Agent 回复的字符数。
     */
    int lastReplyLength() {
        List<AgentMessage> messages = harness.messages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            AgentMessage message = messages.get(i);
            if (message.getRole() == AgentMessageRole.ASSISTANT && message.getTurnNo() > 0) {
                return message.getContent() == null ? 0 : message.getContent().length();
            }
        }
        return 0;
    }

    /**
     * 最后一轮用户输入的字符数。
     */
    int lastUserInputLength() {
        List<AgentMessage> messages = harness.messages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            AgentMessage message = messages.get(i);
            if (message.getRole() == AgentMessageRole.USER) {
                return message.getContent() == null ? 0 : message.getContent().length();
            }
        }
        return 0;
    }

    /**
     * 回复与用户输入的长度比，保留两位小数。
     *
     * 它是「克制」这件事唯一可量化的近似：Agent 的话不该长于用户的表达
     * （护栏规则第五条）。之所以只进快照层而不做不变量：合法回复偶尔比
     * 一句极短的用户输入长是正常的（用户只说「嗯」时 Agent 仍要问一个完整问题），
     * 把它做成硬上限会逼出一堆无意义的失败。
     */
    double replyToInputRatio() {
        int input = lastUserInputLength();
        if (input == 0) {
            return 0d;
        }
        return Math.round((double) lastReplyLength() / input * 100d) / 100d;
    }
}
