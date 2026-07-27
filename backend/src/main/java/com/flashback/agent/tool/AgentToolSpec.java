package com.flashback.agent.tool;

import java.util.List;

/**
 * 单个工具的规格声明（C2）。
 *
 * 说明：
 * - description 与参数描述会进入下发给 provider 的 tools schema，因此措辞需符合产品气质
 * （克制、建议不代决），不得出现诊断或代决语义；
 * - askText 是每个写工具的必需参数：FC 场景下 provider 可能只返回 tool_calls 而 content 为空，
 * 此时用 askText 作为该轮 Agent 回复，避免出现「有确认条但 Agent 没说话」的空白气泡
 * （design.md 数据流 2.1 要点二）。
 *
 * @param name        工具枚举
 * @param description 面向模型的工具用途说明
 * @param parameters  参数规格（含 askText）
 */
public record AgentToolSpec(
        AgentToolName name,
        String description,
        List<AgentToolParameter> parameters) {

    public AgentToolSpec {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }

    /**
     * 参数类型。仅覆盖 strict mode 支持且本 change 需要的类型。
     */
    public enum ParameterType {
        STRING,
        INTEGER_ARRAY
    }

    /**
     * 单个参数规格。
     *
     * @param name        参数名
     * @param type        参数类型
     * @param description 面向模型的参数说明
     * @param pattern     可选正则约束；strict mode 支持 pattern，但不支持 minLength/maxLength，
     *                    因此长度与数量边界必须由 AgentToolValidator 在代码层校验（design §3.2）
     */
    public record AgentToolParameter(
            String name,
            ParameterType type,
            String description,
            String pattern) {

        public static AgentToolParameter string(String name, String description) {
            return new AgentToolParameter(name, ParameterType.STRING, description, null);
        }

        public static AgentToolParameter string(String name, String description, String pattern) {
            return new AgentToolParameter(name, ParameterType.STRING, description, pattern);
        }

        public static AgentToolParameter integerArray(String name, String description) {
            return new AgentToolParameter(name, ParameterType.INTEGER_ARRAY, description, null);
        }
    }
}
