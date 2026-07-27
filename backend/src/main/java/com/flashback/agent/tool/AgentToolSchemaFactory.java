package com.flashback.agent.tool;

import com.flashback.config.AppAgentProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 由白名单生成 OpenAI-compatible function calling tools 数组（C2）。
 *
 * strict mode 约束（design.md §3.2，依据 proposal F23）：
 * - object 的全部属性必须 required，且 additionalProperties=false；
 * - string 不支持 minLength / maxLength，可用 pattern；
 * - array 不支持 minItems / maxItems。
 *
 * 因此本类**只表达类型与形状**，长度与数量边界一律留给 AgentToolValidator 代码层校验。
 * 本类不得输出 maxLength / minLength / maxItems / minItems，否则 provider 服务端会拒绝整个
 * schema。
 */
@Component
public class AgentToolSchemaFactory {

    private final AgentToolRegistry registry;
    private final AppAgentProperties appAgentProperties;

    public AgentToolSchemaFactory(AgentToolRegistry registry, AppAgentProperties appAgentProperties) {
        this.registry = registry;
        this.appAgentProperties = appAgentProperties;
    }

    /**
     * 构造下发用的 tools 数组。
     *
     * @param strictMode 是否启用 strict mode（为每个 function 追加 strict=true）
     */
    public List<Map<String, Object>> buildTools(boolean strictMode) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (AgentToolSpec spec : registry.functionCallingTools()) {
            tools.add(buildTool(spec, strictMode));
        }
        return List.copyOf(tools);
    }

    private Map<String, Object> buildTool(AgentToolSpec spec, boolean strictMode) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", spec.name().wireName());
        function.put("description", spec.description());
        if (strictMode) {
            function.put("strict", true);
        }
        function.put("parameters", buildParameters(spec));

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return Map.copyOf(tool);
    }

    private Map<String, Object> buildParameters(AgentToolSpec spec) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (AgentToolSpec.AgentToolParameter parameter : spec.parameters()) {
            properties.put(parameter.name(), buildProperty(parameter));
            // strict mode 要求全部属性都在 required 中。
            required.add(parameter.name());
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", Map.copyOf(properties));
        parameters.put("required", List.copyOf(required));
        parameters.put("additionalProperties", false);
        return Map.copyOf(parameters);
    }

    private Map<String, Object> buildProperty(AgentToolSpec.AgentToolParameter parameter) {
        Map<String, Object> property = new LinkedHashMap<>();
        switch (parameter.type()) {
            case STRING -> {
                property.put("type", "string");
                property.put("description", describeWithBudget(parameter));
                if (parameter.pattern() != null) {
                    property.put("pattern", parameter.pattern());
                }
            }
            case INTEGER_ARRAY -> {
                property.put("type", "array");
                property.put("description", describeWithBudget(parameter));
                property.put("items", Map.of("type", "integer"));
            }
            default -> throw new IllegalStateException("unsupported parameter type: " + parameter.type());
        }
        return Map.copyOf(property);
    }

    /**
     * 长度与数量上限无法进 schema（strict 不支持），因此写进参数描述里作为软提示，
     * 硬约束仍由 AgentToolValidator 执行。这不是重复实现，而是「告知模型」与「强制拒绝」两件事。
     */
    private String describeWithBudget(AgentToolSpec.AgentToolParameter parameter) {
        String description = parameter.description();
        if (AgentToolRegistry.PARAM_ASK_TEXT.equals(parameter.name())) {
            return description + "（不超过 " + appAgentProperties.getMaxReplyChars() + " 个字符）";
        }
        if (AgentToolRegistry.PARAM_TEXT.equals(parameter.name())) {
            return description + "（不超过 " + appAgentProperties.getMaxToolContentChars() + " 个字符）";
        }
        if (AgentToolRegistry.PARAM_TAG_IDS.equals(parameter.name())) {
            return description + "（最多 " + appAgentProperties.getMaxToolTagIds() + " 个）";
        }
        return description;
    }
}
