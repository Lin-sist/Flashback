package com.flashback.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.config.AppAgentProperties;
import com.flashback.config.AppAiProperties;
import com.flashback.agent.resilience.AgentCallBudget;
import com.flashback.agent.resilience.AgentProviderException;
import com.flashback.agent.resilience.AgentProviderFailureCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 对话的模型调用客户端。
 *
 * 边界（design.md 决策 3）：
 * - 复用 app.ai 的 provider / secret 配置，不新增凭证字段、不复制配置读取逻辑；
 * - 沿用既有 OpenAI-compatible /chat/completions 请求形状；
 * - 不改动 AiServiceImpl 现有三个方法的行为。
 */
@Component
public class AgentModelClient {

    private final AppAiProperties appAiProperties;
    private final AppAgentProperties appAgentProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String strictModeBaseUrl;

    @Autowired
    public AgentModelClient(AppAiProperties appAiProperties, AppAgentProperties appAgentProperties) {
        this(appAiProperties, appAgentProperties, new ObjectMapper(), HttpClient.newHttpClient());
    }

    AgentModelClient(
            AppAiProperties appAiProperties,
            AppAgentProperties appAgentProperties,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.appAiProperties = appAiProperties;
        this.appAgentProperties = appAgentProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.strictModeBaseUrl = appAgentProperties.getStrictModeBaseUrl();
    }

    /**
     * C2：工具调用是否可用；返回 null 表示可用，否则为不可用原因。
     *
     * 判定顺序（design.md §3.3，依据 proposal F29）：
     * 1) 基础 AI 配置必须可用；
     * 2) 总开关 toolCallingEnabled；
     * 3) 当前 model 必须在已确认支持 FC 的白名单内——不得假设任意 provider / model 都支持；
     * 4) 启用 strict mode 时必须配了独立地址，否则视为配置错误而非静默降级。
     *
     * 注意：本方法返回非 null 时，Agent 退回 C1 纯对话行为，
     * **不会**降级到任何自研提议协议（design.md 决策 1：无降级）。
     */
    public String toolCallingUnavailableReason() {
        String base = unavailableReason();
        if (base != null) {
            return base;
        }
        if (!appAgentProperties.isToolCallingEnabled()) {
            return "Agent工具调用未启用";
        }
        if (isMockProvider()) {
            // mock provider 由 AgentMockResponder 伪造 tool_calls，用于零外调端到端测试。
            return null;
        }
        String model = appAiProperties.getModel();
        if (!isFunctionCallingModel(model)) {
            return "当前模型未确认支持function calling";
        }
        if (appAgentProperties.isStrictModeEnabled() && isBlank(strictModeBaseUrl)) {
            return "strict mode缺少base url配置";
        }
        return null;
    }

    /**
     * 当前 model 是否在已确认支持 FC 的配置白名单内。
     */
    public boolean isFunctionCallingModel(String model) {
        if (isBlank(model)) {
            return false;
        }
        List<String> allowed = appAgentProperties.getFunctionCallingModels();
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        String normalized = model.trim();
        return allowed.stream()
                .filter(candidate -> candidate != null)
                .anyMatch(candidate -> candidate.trim().equalsIgnoreCase(normalized));
    }

    /**
     * 是否应以 strict mode 发起请求。
     */
    public boolean useStrictMode() {
        return appAgentProperties.isStrictModeEnabled() && !isBlank(strictModeBaseUrl);
    }

    /**
     * provider 名称；配置非法时返回 unknown。
     */
    public String provider() {
        AppAiProperties.Provider provider = resolveProviderType();
        return provider == null ? "unknown" : provider.getConfigValue();
    }

    /**
     * C5：当前配置的 model 标识，供决策轨迹的版本锚点使用。
     *
     * 只读**配置值**，不涉及凭证；未配置时返回 unknown 而非空串，
     * 使轨迹里「没配 model」与「字段没采集到」可区分。
     */
    public String model() {
        String model = appAiProperties.getModel();
        return isBlank(model) ? "unknown" : model.trim();
    }

    public boolean isMockProvider() {
        return resolveProviderType() == AppAiProperties.Provider.MOCK;
    }

    public boolean isMockEnabled() {
        return appAiProperties.isRealModeMockEnabled();
    }

    /**
     * 返回不可用原因；为 null 表示配置可用。
     */
    public String unavailableReason() {
        AppAiProperties.Provider provider = resolveProviderType();
        if (provider == null) {
            return "AI provider配置不支持";
        }
        if (provider == AppAiProperties.Provider.MOCK) {
            return appAiProperties.isRealModeMockEnabled() ? null : "AI mock provider未启用";
        }
        if (isBlank(appAiProperties.getBaseUrl())
                || isBlank(appAiProperties.getModel())
                || isBlank(appAiProperties.getApiKey())) {
            return "AI服务未配置";
        }
        return null;
    }

    /**
     * 调用真实 provider，返回 message content 原文（约定为 JSON 字符串）。
     */
    public String complete(List<Map<String, String>> messages) throws IOException, InterruptedException {
        return complete(messages, newCallBudget());
    }

    /** C8：使用 request-scope 共享预算调用 material provider。 */
    public String complete(
            List<Map<String, String>> messages,
            AgentCallBudget budget) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
                "model", appAiProperties.getModel().trim(),
                "messages", messages,
                "response_format", Map.of("type", "json_object"),
                "stream", false);
        HttpRequest httpRequest = buildRequest(chatCompletionsUrl(), body, budget);
        HttpResponse<String> response = send(httpRequest);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw AgentProviderException.forHttpStatus(response.statusCode());
        }
        JsonNode content;
        try {
            content = objectMapper.readTree(response.body())
                    .path("choices").path(0).path("message").path("content");
        } catch (IOException ex) {
            throw AgentProviderException.invalidResponse(ex);
        }
        if (!content.isTextual() || isBlank(content.asText())) {
            throw AgentProviderException.invalidResponse(null);
        }
        return content.asText();
    }

    /**
     * 从 provider 返回的 JSON 文本中取出指定字段。
     */
    public String extractText(String content, String fieldName) throws IOException {
        try {
            String value = objectMapper.readTree(content).path(fieldName).asText(null);
            return isBlank(value) ? null : value.trim();
        } catch (IOException ex) {
            throw AgentProviderException.invalidResponse(ex);
        }
    }

    /**
     * C2：带 function calling tools 的对话调用。
     *
     * 与 {@link #complete} 的差异（design.md 决策 1、数据流 2.1 要点三）：
     * - 本方法**不下发** response_format：FC 路径下自然语言回复走 message.content、
     * 工具提议走 message.tool_calls，二者天然并存，不需要 json_object；
     * - {@link #complete} 与三个既有单轮 AI 端点保持 json_object 不变，本方法不影响它们；
     * - stream 固定 false（proposal F30：streaming 会加剧 tool_calls 解析不稳）。
     *
     * @param messages   对话消息
     * @param tools      工具定义；为空表示本轮不下发工具
     * @param strictMode 是否走 strict mode 专用地址
     */
    public AgentModelResponse completeWithTools(
            List<Map<String, String>> messages,
            List<Map<String, Object>> tools,
            boolean strictMode) throws IOException, InterruptedException {
        return completeWithTools(messages, tools, strictMode, newCallBudget());
    }

    /** C8：initial / reflection 共享同一个 request-scope budget。 */
    public AgentModelResponse completeWithTools(
            List<Map<String, String>> messages,
            List<Map<String, Object>> tools,
            boolean strictMode,
            AgentCallBudget budget) throws IOException, InterruptedException {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", appAiProperties.getModel().trim());
        body.put("messages", messages);
        body.put("stream", false);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }

        HttpRequest httpRequest = buildRequest(chatCompletionsUrl(strictMode), body, budget);
        HttpResponse<String> response = send(httpRequest);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw AgentProviderException.forHttpStatus(response.statusCode());
        }

        JsonNode message;
        try {
            message = objectMapper.readTree(response.body())
                    .path("choices").path(0).path("message");
        } catch (IOException ex) {
            throw AgentProviderException.invalidResponse(ex);
        }

        JsonNode contentNode = message.path("content");
        String content = contentNode.isTextual() && !isBlank(contentNode.asText())
                ? contentNode.asText().trim()
                : null;

        List<AgentRawToolCall> toolCalls = new ArrayList<>();
        JsonNode toolCallsNode = message.path("tool_calls");
        if (toolCallsNode.isArray()) {
            for (JsonNode node : toolCallsNode) {
                JsonNode function = node.path("function");
                String name = function.path("name").asText(null);
                String arguments = function.path("arguments").asText(null);
                if (!isBlank(name)) {
                    toolCalls.add(new AgentRawToolCall(name.trim(), arguments));
                }
            }
        }

        if (content == null && toolCalls.isEmpty()) {
            // 既没有话也没有提议：视为无效响应，交由上层按显式失败处理，不伪造内容。
            throw AgentProviderException.invalidResponse(null);
        }
        return new AgentModelResponse(content, List.copyOf(toolCalls));
    }

    private HttpRequest buildRequest(
            String url,
            Map<String, Object> body,
            AgentCallBudget budget) throws AgentProviderException {
        try {
            return HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout(budget))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + appAiProperties.getApiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        } catch (AgentProviderException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw AgentProviderException.of(AgentProviderFailureCategory.AUTH_CONFIGURATION, ex);
        } catch (IOException ex) {
            throw AgentProviderException.of(AgentProviderFailureCategory.UNKNOWN, ex);
        }
    }

    Duration requestTimeout(AgentCallBudget budget) throws AgentProviderException {
        long timeoutMillis = requireBudget(budget)
                .nextCallTimeoutMillis(appAiProperties.getTimeoutMillis());
        return Duration.ofMillis(timeoutMillis);
    }

    private HttpResponse<String> send(HttpRequest request) throws AgentProviderException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException ex) {
            throw AgentProviderException.of(AgentProviderFailureCategory.TIMEOUT, ex);
        } catch (ConnectException ex) {
            throw AgentProviderException.of(AgentProviderFailureCategory.UPSTREAM_UNAVAILABLE, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw AgentProviderException.of(AgentProviderFailureCategory.INTERRUPTED, ex);
        } catch (IOException ex) {
            throw AgentProviderException.of(AgentProviderFailureCategory.UNKNOWN, ex);
        }
    }

    private AgentCallBudget requireBudget(AgentCallBudget budget) throws AgentProviderException {
        if (budget == null) {
            throw AgentProviderException.of(AgentProviderFailureCategory.AUTH_CONFIGURATION, null);
        }
        return budget;
    }

    private AgentCallBudget newCallBudget() {
        return AgentCallBudget.start(appAgentProperties.getResilience().getProviderWorkTimeoutMillis());
    }

    /**
     * 从 tool_calls 的 arguments JSON 中读取字符串字段。
     */
    public String readArgumentText(String arguments, String fieldName) {
        JsonNode node = readArguments(arguments);
        if (node == null) {
            return null;
        }
        String value = node.path(fieldName).asText(null);
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 从 tool_calls 的 arguments JSON 中读取整数数组字段。
     * 非数字元素被忽略；由 validator 判定「数量为 0」是否算参数非法。
     */
    public List<Long> readArgumentLongArray(String arguments, String fieldName) {
        JsonNode node = readArguments(arguments);
        if (node == null) {
            return List.of();
        }
        JsonNode array = node.path(fieldName);
        if (!array.isArray()) {
            return List.of();
        }
        List<Long> values = new ArrayList<>();
        for (JsonNode item : array) {
            if (item.canConvertToLong()) {
                values.add(item.asLong());
            }
        }
        return List.copyOf(values);
    }

    private JsonNode readArguments(String arguments) {
        if (isBlank(arguments)) {
            return null;
        }
        try {
            return objectMapper.readTree(arguments);
        } catch (IOException ex) {
            return null;
        }
    }

    private String chatCompletionsUrl() {
        return chatCompletionsUrl(false);
    }

    private String chatCompletionsUrl(boolean strictMode) {
        String baseUrl = strictMode && !isBlank(strictModeBaseUrl)
                ? strictModeBaseUrl.trim()
                : appAiProperties.getBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/chat/completions";
    }

    private AppAiProperties.Provider resolveProviderType() {
        try {
            return appAiProperties.getProviderType();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
