package com.flashback.agent.eval;

import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.AgentConversationIntent;
import com.flashback.domain.AgentStage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一条评测用例（C6，T-06）。
 *
 * 用例以外置 YAML 表达而不是 Java 内联字面量，原因有两条：
 * 一是用例是**要被人读和 review 的资产**（里面还有解释「这条在测什么」的注释）；
 * 二是真实样本必须能走一份 gitignore 的文件，而同一个 runner 读两套输入。
 *
 * 本类只做「把 Map 变成有名字的东西」这一件事，不含任何断言逻辑——
 * 断言在 {@link AgentEvalInvariants}，快照在 {@link AgentEvalSnapshot}。
 */
final class AgentEvalCase {

    private final String caseId;
    private final AgentEvalDimension dimension;
    private final String note;
    private final Map<String, Object> setup;
    private final List<Turn> turns;
    private final Map<String, Object> expect;
    private final String source;

    private AgentEvalCase(
            String caseId,
            AgentEvalDimension dimension,
            String note,
            Map<String, Object> setup,
            List<Turn> turns,
            Map<String, Object> expect,
            String source) {
        this.caseId = caseId;
        this.dimension = dimension;
        this.note = note;
        this.setup = setup;
        this.turns = turns;
        this.expect = expect;
        this.source = source;
    }

    /**
     * 一轮编排。
     *
     * @param userInput 用户这一轮说的话
     * @param reply     编排给 provider 返回的回复；null 表示本轮 provider 失败
     * @param material  素材路径的编排产出；null 表示不编排素材
     * @param toolCall  模型返回的工具提议；null 表示不返回提议
     */
    record Turn(
            String userInput,
            String reply,
            String reflectionReply,
            boolean reflectionFailure,
            String material,
            ToolCallSpec toolCall) {
    }

    record ToolCallSpec(String name, String text, String askText) {

        /**
         * 拼成与真实 provider 同构的 arguments JSON——走的是生产解析路径，
         * 不是一条为测试特设的分支。
         */
        String argumentsJson() {
            StringBuilder builder = new StringBuilder("{");
            if (text != null) {
                builder.append("\"text\":\"").append(escape(text)).append("\"");
            }
            if (askText != null) {
                if (builder.length() > 1) {
                    builder.append(',');
                }
                builder.append("\"askText\":\"").append(escape(askText)).append("\"");
            }
            return builder.append('}').toString();
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    @SuppressWarnings("unchecked")
    static AgentEvalCase from(Map<String, Object> raw, String source) {
        String caseId = requireText(raw, "caseId", source);
        String dimensionName = requireText(raw, "dimension", source);
        AgentEvalDimension dimension;
        try {
            dimension = AgentEvalDimension.valueOf(dimensionName);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "eval case " + caseId + " in " + source + " has unknown dimension: " + dimensionName);
        }

        Object rawTurns = raw.get("turns");
        if (!(rawTurns instanceof List<?> turnList) || turnList.isEmpty()) {
            throw new IllegalStateException("eval case " + caseId + " in " + source + " has no turns");
        }
        List<Turn> turns = new ArrayList<>();
        for (Object element : turnList) {
            if (!(element instanceof Map<?, ?> turnMap)) {
                throw new IllegalStateException(
                        "eval case " + caseId + " in " + source + " has a malformed turn");
            }
            Map<String, Object> turn = (Map<String, Object>) turnMap;
            ToolCallSpec toolCall = null;
            if (turn.get("toolCall") instanceof Map<?, ?> toolMap) {
                Map<String, Object> tool = (Map<String, Object>) toolMap;
                toolCall = new ToolCallSpec(
                        text(tool.get("name")), text(tool.get("text")), text(tool.get("askText")));
            }
            String userInput = text(turn.get("userInput"));
            if (userInput == null) {
                throw new IllegalStateException(
                        "eval case " + caseId + " in " + source + " has a turn without userInput");
            }
            turns.add(new Turn(
                    userInput,
                    text(turn.get("reply")),
                    text(turn.get("reflectionReply")),
                    boolOf(turn.get("reflectionFailure")),
                    text(turn.get("material")),
                    toolCall));
        }

        Map<String, Object> setup = raw.get("setup") instanceof Map<?, ?> setupMap
                ? new LinkedHashMap<>((Map<String, Object>) setupMap)
                : Map.of();
        Map<String, Object> expect = raw.get("expect") instanceof Map<?, ?> expectMap
                ? new LinkedHashMap<>((Map<String, Object>) expectMap)
                : Map.of();

        return new AgentEvalCase(
                caseId, dimension, text(raw.get("note")), setup, List.copyOf(turns), expect, source);
    }

    // ---------- 读取 ----------

    String caseId() {
        return caseId;
    }

    AgentEvalDimension dimension() {
        return dimension;
    }

    String note() {
        return note;
    }

    List<Turn> turns() {
        return turns;
    }

    String source() {
        return source;
    }

    /**
     * 按 setup 装配一个 harness。
     *
     * 只认已知键：写错键名（比如把 maxTurns 写成 max_turns）会**明确失败**，
     * 不静默按默认值跑——否则用例看起来通过了，而它测的根本不是声称的那件事。
     */
    AgentEvalHarness newHarness() {
        AgentEvalHarness.Builder builder = AgentEvalHarness.builder();
        for (Map.Entry<String, Object> entry : setup.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            switch (key) {
                case "purpose" -> builder.purpose(AgentSessionPurpose.valueOf(String.valueOf(value)));
                case "conversationIntent" -> builder.conversationIntent(
                        AgentConversationIntent.valueOf(String.valueOf(value)));
                case "stage" -> builder.stage(AgentStage.valueOf(String.valueOf(value)));
                case "turnCount" -> builder.turnCount(intOf(value));
                case "stageReaskCount" -> builder.stageReaskCount(intOf(value));
                case "maxTurns" -> builder.maxTurns(intOf(value));
                case "reviewMaxTurns" -> builder.reviewMaxTurns(intOf(value));
                case "maxReplyChars" -> builder.maxReplyChars(intOf(value));
                case "memoryEnabled" -> builder.memoryEnabled(boolOf(value));
                case "tagIds" -> builder.tagIds(longsOf(value));
                case "memoryCandidates" -> applyMemoryCandidates(builder, value);
                default -> throw new IllegalStateException(
                        "eval case " + caseId + " in " + source + " has unknown setup key: " + key);
            }
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private void applyMemoryCandidates(AgentEvalHarness.Builder builder, Object value) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException(
                    "eval case " + caseId + " in " + source + " has malformed memoryCandidates");
        }
        for (Object element : list) {
            Map<String, Object> candidate = (Map<String, Object>) element;
            builder.memoryCandidate(
                    Long.parseLong(String.valueOf(candidate.get("id"))),
                    String.valueOf(candidate.get("text")),
                    dateTimeOf(candidate.get("createdAt")));
        }
    }

    /**
     * 解析用例里的时间。
     *
     * 必须处理 {@code java.util.Date}：snakeyaml 会把未加引号的
     * {@code 2026-03-14T21:00:00} 按 YAML timestamp 规范**自动转成 Date**，
     * 而不是留给我们一个字符串。直接 {@code LocalDateTime.parse(String.valueOf(...))}
     * 会拿到 {@code "Sun Mar 15 05:00:00 SGT 2026"} 这种形态而解析失败。
     *
     * 顺带说明为什么不靠「在 YAML 里给时间加引号」了事：那是一条**只能靠人记得**
     * 的约定，下一个写用例的人不加引号就会踩同一个坑。在这里处理一次即可。
     */
    private static LocalDateTime dateTimeOf(Object value) {
        if (value instanceof java.util.Date date) {
            // snakeyaml 按 UTC 解析无时区的 timestamp；转回本地时区以保持
            // 与用例文件里写的字面时间一致。
            return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
        }
        return LocalDateTime.parse(String.valueOf(value));
    }

    /**
     * 期望值。缺键返回 null，由不变量断言决定该键是否必需。
     */
    Object expected(String key) {
        return expect.get(key);
    }

    boolean hasExpectation(String key) {
        return expect.containsKey(key);
    }

    /**
     * 声明了哪些期望键。用于校验没有拼错的期望键被静默忽略。
     */
    java.util.Set<String> expectationKeys() {
        return expect.keySet();
    }

    @Override
    public String toString() {
        // JUnit 的用例显示名。刻意只输出 caseId 与维度，**不输出用例输入文本**——
        // 测试报告也是一种产物，隐私边界同样适用。
        return caseId + " [" + dimension + "]";
    }

    // ---------- 小工具 ----------

    private static String requireText(Map<String, Object> raw, String key, String source) {
        String value = text(raw.get(key));
        if (value == null) {
            throw new IllegalStateException("eval case in " + source + " is missing required field: " + key);
        }
        return value;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static int intOf(Object value) {
        return Integer.parseInt(String.valueOf(value));
    }

    static boolean boolOf(Object value) {
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static List<Long> longsOf(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Object element : list) {
            result.add(Long.parseLong(String.valueOf(element)));
        }
        return List.copyOf(result);
    }
}
