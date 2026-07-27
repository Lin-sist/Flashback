package com.flashback.agent.tool;

import com.flashback.config.AppAgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * tools schema 生成测试（C2）。
 *
 * 核心目的：守住 strict mode 的硬约束。若生成的 schema 含 maxLength / maxItems / minItems，
 * provider 服务端会拒绝**整个** schema，导致工具调用彻底不可用——
 * 这类错误在闸门 3 真实联调前只能靠本测试拦住。
 */
class AgentToolSchemaFactoryTest {

    /** strict mode 明确不支持的关键字（proposal F23）。 */
    private static final List<String> FORBIDDEN_KEYWORDS = List.of("maxLength", "minLength", "maxItems", "minItems");

    private AgentToolSchemaFactory factory;

    @BeforeEach
    void setUp() {
        AppAgentProperties properties = new AppAgentProperties();
        properties.setMaxToolContentChars(300);
        properties.setMaxToolTagIds(5);
        properties.setMaxReplyChars(120);
        factory = new AgentToolSchemaFactory(new AgentToolRegistry(), properties);
    }

    @Test
    void shouldOnlyExposeWriteTools() {
        List<Map<String, Object>> tools = factory.buildTools(true);

        List<String> names = tools.stream().map(this::functionName).toList();
        assertThat(names).containsExactlyInAnyOrder(
                "append_record_content", "add_record_tags", "propose_unlock_at");
        // 读工具由后端预注入，不得出现在下发的 tools 中（design §3.1）。
        assertThat(names).doesNotContain("list_available_tags", "read_draft_snapshot");
    }

    /**
     * 不可逆操作不得出现在 schema 中——这是白名单的对外表达面。
     */
    @Test
    void shouldNotExposeIrreversibleOperations() {
        List<String> names = factory.buildTools(true).stream().map(this::functionName).toList();

        assertThat(names).noneMatch(name -> name.contains("seal")
                || name.contains("delete")
                || name.contains("unlock_record")
                || name.contains("location")
                || name.contains("cover")
                || name.contains("attachment"));
    }

    @Test
    void shouldMarkStrictWhenStrictModeEnabled() {
        for (Map<String, Object> tool : factory.buildTools(true)) {
            assertThat(function(tool)).containsEntry("strict", true);
        }
    }

    @Test
    void shouldOmitStrictWhenStrictModeDisabled() {
        for (Map<String, Object> tool : factory.buildTools(false)) {
            assertThat(function(tool)).doesNotContainKey("strict");
        }
    }

    /**
     * strict mode 要求：全部属性 required + additionalProperties=false。
     */
    @Test
    void shouldRequireAllPropertiesAndForbidAdditional() {
        for (Map<String, Object> tool : factory.buildTools(true)) {
            Map<String, Object> parameters = parameters(tool);
            assertThat(parameters).containsEntry("additionalProperties", false);
            assertThat(parameters).containsEntry("type", "object");

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) parameters.get("required");

            assertThat(required)
                    .as("工具 %s 的全部属性都必须 required", functionName(tool))
                    .containsExactlyInAnyOrderElementsOf(properties.keySet());
        }
    }

    /**
     * 守门测试：schema 中出现 strict 不支持的关键字会让 provider 拒绝整个 schema。
     */
    @Test
    void shouldNotUseKeywordsUnsupportedByStrictMode() {
        String serialized = flatten(factory.buildTools(true));

        for (String keyword : FORBIDDEN_KEYWORDS) {
            assertThat(serialized)
                    .as("strict mode 不支持 %s，出现即会导致 schema 被服务端拒绝", keyword)
                    .doesNotContain(keyword);
        }
    }

    @Test
    void shouldRequireAskTextOnEveryWriteTool() {
        for (Map<String, Object> tool : factory.buildTools(true)) {
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) parameters(tool).get("required");
            assertThat(required)
                    .as("工具 %s 必须带 askText，用于 content 为空时兜底回复", functionName(tool))
                    .contains("askText");
        }
    }

    @Test
    void shouldConstrainUnlockAtWithPattern() {
        Map<String, Object> tool = factory.buildTools(true).stream()
                .filter(candidate -> "propose_unlock_at".equals(functionName(candidate)))
                .findFirst()
                .orElseThrow();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters(tool).get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> unlockAt = (Map<String, Object>) properties.get("unlockAt");

        assertThat(unlockAt).containsEntry("type", "string");
        assertThat(unlockAt).containsKey("pattern");
    }

    @Test
    void shouldDeclareTagIdsAsIntegerArray() {
        Map<String, Object> tool = factory.buildTools(true).stream()
                .filter(candidate -> "add_record_tags".equals(functionName(candidate)))
                .findFirst()
                .orElseThrow();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters(tool).get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> tagIds = (Map<String, Object>) properties.get("tagIds");

        assertThat(tagIds).containsEntry("type", "array");
        assertThat(tagIds).containsEntry("items", Map.of("type", "integer"));
    }

    // ---------- helpers ----------

    @SuppressWarnings("unchecked")
    private Map<String, Object> function(Map<String, Object> tool) {
        assertThat(tool).containsEntry("type", "function");
        return (Map<String, Object>) tool.get("function");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parameters(Map<String, Object> tool) {
        return (Map<String, Object>) function(tool).get("parameters");
    }

    private String functionName(Map<String, Object> tool) {
        return String.valueOf(function(tool).get("name"));
    }

    /**
     * 递归展开为字符串，便于断言不含被禁关键字。
     */
    private String flatten(Object node) {
        List<String> parts = new ArrayList<>();
        collect(node, parts);
        return String.join("|", parts);
    }

    private void collect(Object node, List<String> parts) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                parts.add(String.valueOf(entry.getKey()));
                collect(entry.getValue(), parts);
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                collect(item, parts);
            }
        } else {
            parts.add(String.valueOf(node));
        }
    }
}
