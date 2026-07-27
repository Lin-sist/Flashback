package com.flashback.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.config.AppAiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public AgentModelClient(AppAiProperties appAiProperties) {
        this(appAiProperties, new ObjectMapper(), HttpClient.newHttpClient());
    }

    AgentModelClient(AppAiProperties appAiProperties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.appAiProperties = appAiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * provider 名称；配置非法时返回 unknown。
     */
    public String provider() {
        AppAiProperties.Provider provider = resolveProviderType();
        return provider == null ? "unknown" : provider.getConfigValue();
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
        Map<String, Object> body = Map.of(
                "model", appAiProperties.getModel().trim(),
                "messages", messages,
                "response_format", Map.of("type", "json_object"),
                "stream", false);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(chatCompletionsUrl()))
                .timeout(Duration.ofMillis(appAiProperties.getTimeoutMillis()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + appAiProperties.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("AI provider returned HTTP " + response.statusCode());
        }
        JsonNode content = objectMapper.readTree(response.body())
                .path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || isBlank(content.asText())) {
            throw new IOException("AI provider response missing content");
        }
        return content.asText();
    }

    /**
     * 从 provider 返回的 JSON 文本中取出指定字段。
     */
    public String extractText(String content, String fieldName) throws IOException {
        String value = objectMapper.readTree(content).path(fieldName).asText(null);
        return isBlank(value) ? null : value.trim();
    }

    private String chatCompletionsUrl() {
        String baseUrl = appAiProperties.getBaseUrl().trim();
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
