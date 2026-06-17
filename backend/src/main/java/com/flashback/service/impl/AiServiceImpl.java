package com.flashback.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.config.AppAiProperties;
import com.flashback.dto.AiSummarizeRecordRequest;
import com.flashback.dto.AiWritingPromptsRequest;
import com.flashback.service.AiService;
import com.flashback.vo.AiSummaryVO;
import com.flashback.vo.AiWritingPromptsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AI 服务默认实现。
 *
 * AI 服务默认实现。
 */
@Service
public class AiServiceImpl implements AiService {

    private static final int PROMPT_LIMIT = 3;
    private static final int CONTEXT_PREVIEW_LIMIT = 20;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_FALLBACK = "FALLBACK";

    private final AppAiProperties appAiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public AiServiceImpl(AppAiProperties appAiProperties) {
        this(appAiProperties, new ObjectMapper(), HttpClient.newHttpClient());
    }

    AiServiceImpl(AppAiProperties appAiProperties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.appAiProperties = appAiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public AiWritingPromptsVO generateWritingPrompts(Long userId, AiWritingPromptsRequest request) {
        AppAiProperties.Provider provider = resolveProviderType();
        if (provider == null) {
            return unavailablePrompts("AI provider配置不支持");
        }
        if (provider == AppAiProperties.Provider.MOCK) {
            try {
                List<String> prompts = invokeWritingPromptsModel(request);
                if (prompts == null || prompts.isEmpty()) {
                    return fallbackPrompts();
                }
                AiWritingPromptsVO vo = new AiWritingPromptsVO();
                vo.setPrompts(prompts);
                vo.setSource(resolveProvider());
                vo.setStatus(STATUS_SUCCESS);
                return vo;
            } catch (Exception ex) {
                return fallbackPrompts();
            }
        }

        String configError = realProviderConfigError();
        if (configError != null) {
            return unavailablePrompts(configError);
        }

        try {
            String content = invokeChatCompletion(buildWritingPromptsMessages(request));
            List<String> prompts = parsePrompts(content);
            if (prompts.isEmpty()) {
                return failedPrompts("AI返回内容无效");
            }
            AiWritingPromptsVO vo = new AiWritingPromptsVO();
            vo.setPrompts(prompts);
            vo.setSource(resolveProvider());
            vo.setStatus(STATUS_SUCCESS);
            return vo;
        } catch (Exception ex) {
            return failedPrompts("AI服务暂时不可用");
        }
    }

    @Override
    public AiSummaryVO summarizeRecord(Long userId, AiSummarizeRecordRequest request) {
        AppAiProperties.Provider provider = resolveProviderType();
        if (provider == null) {
            return unavailableSummary("AI provider配置不支持");
        }
        if (provider == AppAiProperties.Provider.MOCK) {
            try {
                AiSummaryVO summary = invokeSummaryModel(request);
                if (!isCompleteSummary(summary)) {
                    return fallbackSummary();
                }
                summary.setSource(resolveProvider());
                summary.setStatus(STATUS_SUCCESS);
                return summary;
            } catch (Exception ex) {
                return fallbackSummary();
            }
        }

        String configError = realProviderConfigError();
        if (configError != null) {
            return unavailableSummary(configError);
        }

        try {
            String content = invokeChatCompletion(buildSummaryMessages(request));
            AiSummaryVO summary = parseSummary(content);
            if (!isCompleteSummary(summary)) {
                return failedSummary("AI返回内容无效");
            }
            summary.setSource(resolveProvider());
            summary.setStatus(STATUS_SUCCESS);
            return summary;
        } catch (Exception ex) {
            return failedSummary("AI服务暂时不可用");
        }
    }

    private List<String> invokeWritingPromptsModel(AiWritingPromptsRequest request) {
        if (appAiProperties.getProviderType() != AppAiProperties.Provider.MOCK) {
            throw new IllegalStateException("provider not supported in current stage");
        }

        String content = normalizeOptional(request.getContent());
        String recordType = normalizeOptional(request.getRecordType());
        String coreQuestion = normalizeOptional(request.getCoreQuestion());

        List<String> prompts = new ArrayList<>();
        if (!isBlank(recordType)) {
            prompts.add("关于" + recordType + "，你此刻最在意的是什么？");
        }
        if (!isBlank(coreQuestion)) {
            prompts.add("如果先聚焦“" + preview(coreQuestion, CONTEXT_PREVIEW_LIMIT) + "”，你最想想明白什么？");
        }
        if (!isBlank(content)) {
            prompts.add("围绕“" + preview(content, CONTEXT_PREVIEW_LIMIT) + "”，你最想先展开记录哪部分？");
        }
        prompts.add("如果三个月后的你回看现在，你最希望留下哪句话？");
        prompts.add("今天你最想先推动的一件小事是什么？");

        return prompts.stream().limit(PROMPT_LIMIT).toList();
    }

    private AiSummaryVO invokeSummaryModel(AiSummarizeRecordRequest request) {
        if (appAiProperties.getProviderType() != AppAiProperties.Provider.MOCK) {
            throw new IllegalStateException("provider not supported in current stage");
        }

        String content = normalizeOptional(request.getContent());
        if (content == null) {
            return fallbackSummary();
        }

        String coreQuestion = normalizeOptional(request.getCoreQuestion());

        AiSummaryVO vo = new AiSummaryVO();
        vo.setSummary(buildSummary(content));
        vo.setConfusion("你当前最困扰的点可能与“" + preview(content, 24) + "”有关");
        vo.setEmotion(inferEmotion(content));
        vo.setCoreQuestion(!isBlank(coreQuestion) ? coreQuestion : inferCoreQuestion(content));
        vo.setDesiredOutcome(inferDesiredOutcome(content));
        vo.setBeliefThen(inferBeliefThen(content));
        return vo;
    }

    private AiWritingPromptsVO fallbackPrompts() {
        AiWritingPromptsVO vo = new AiWritingPromptsVO();
        vo.setPrompts(List.copyOf(appAiProperties.getFallback().getWritingPrompts()));
        vo.setSource("fallback");
        vo.setStatus(STATUS_FALLBACK);
        vo.setMessage("AI暂不可用，已使用本地提示");
        return vo;
    }

    private AiSummaryVO fallbackSummary() {
        AppAiProperties.Fallback fallback = appAiProperties.getFallback();
        AiSummaryVO vo = new AiSummaryVO();
        vo.setSummary(fallback.getSummary());
        vo.setConfusion(fallback.getConfusion());
        vo.setEmotion(fallback.getEmotion());
        vo.setCoreQuestion(fallback.getCoreQuestion());
        vo.setDesiredOutcome(fallback.getDesiredOutcome());
        vo.setBeliefThen("那时的我可能以为，只要把眼前的问题想清楚，就能立刻知道下一步。");
        vo.setSource("fallback");
        vo.setStatus(STATUS_FALLBACK);
        vo.setMessage("AI暂不可用，已使用本地整理");
        return vo;
    }

    private AiWritingPromptsVO unavailablePrompts(String message) {
        AiWritingPromptsVO vo = new AiWritingPromptsVO();
        vo.setPrompts(List.of());
        vo.setSource(resolveProviderSafely());
        vo.setStatus(STATUS_UNAVAILABLE);
        vo.setMessage(message);
        return vo;
    }

    private AiWritingPromptsVO failedPrompts(String message) {
        AiWritingPromptsVO vo = new AiWritingPromptsVO();
        vo.setPrompts(List.of());
        vo.setSource(resolveProviderSafely());
        vo.setStatus(STATUS_FAILED);
        vo.setMessage(message);
        return vo;
    }

    private AiSummaryVO unavailableSummary(String message) {
        AiSummaryVO vo = new AiSummaryVO();
        vo.setSource(resolveProviderSafely());
        vo.setStatus(STATUS_UNAVAILABLE);
        vo.setMessage(message);
        return vo;
    }

    private AiSummaryVO failedSummary(String message) {
        AiSummaryVO vo = new AiSummaryVO();
        vo.setSource(resolveProviderSafely());
        vo.setStatus(STATUS_FAILED);
        vo.setMessage(message);
        return vo;
    }

    private List<Map<String, String>> buildWritingPromptsMessages(AiWritingPromptsRequest request) {
        String prompt = """
                请为一条私密时间记录生成3个温和、克制、可继续书写的问题。
                只输出JSON，格式为{"prompts":["问题1","问题2","问题3"]}。
                记录类型：%s
                核心问题：%s
                正文：%s
                """.formatted(
                firstPresent(request.getRecordType(), "未指定"),
                firstPresent(request.getCoreQuestion(), "未填写"),
                firstPresent(request.getContent(), "未填写"));
        return List.of(
                Map.of("role", "system", "content", "你是《时光回序》的写作辅助，只帮助用户温和地整理当下，不做诊断或评价。"),
                Map.of("role", "user", "content", prompt));
    }

    private List<Map<String, String>> buildSummaryMessages(AiSummarizeRecordRequest request) {
        String prompt = """
                请整理这条私密时间记录中“你当时以为”的相关内容。
                只输出JSON，格式为{"summary":"...","confusion":"...","emotion":"...","coreQuestion":"...","desiredOutcome":"...","beliefThen":"..."}。
                不要替换用户原文，不要做心理诊断，不要输出JSON之外的文本。
                核心问题：%s
                正文：%s
                """.formatted(
                firstPresent(request.getCoreQuestion(), "未填写"),
                firstPresent(request.getContent(), "未填写"));
        return List.of(
                Map.of("role", "system", "content", "你是《时光回序》的内容整理助手，表达安静、私密、克制。"),
                Map.of("role", "user", "content", prompt));
    }

    protected String invokeChatCompletion(List<Map<String, String>> messages) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
                "model", appAiProperties.getModel().trim(),
                "messages", messages,
                "response_format", Map.of("type", "json_object"),
                "stream", false);
        String requestBody = objectMapper.writeValueAsString(body);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(chatCompletionsUrl()))
                .timeout(Duration.ofMillis(appAiProperties.getTimeoutMillis()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + appAiProperties.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("AI provider returned HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || isBlank(content.asText())) {
            throw new IOException("AI provider response missing content");
        }
        return content.asText();
    }

    private String chatCompletionsUrl() {
        String baseUrl = appAiProperties.getBaseUrl().trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/chat/completions";
    }

    private List<String> parsePrompts(String content) throws IOException {
        JsonNode promptsNode = objectMapper.readTree(content).path("prompts");
        if (!promptsNode.isArray()) {
            return List.of();
        }
        List<String> prompts = new ArrayList<>();
        for (JsonNode promptNode : promptsNode) {
            String prompt = normalizeOptional(promptNode.asText(null));
            if (prompt != null) {
                prompts.add(prompt);
            }
            if (prompts.size() == PROMPT_LIMIT) {
                break;
            }
        }
        return prompts;
    }

    private AiSummaryVO parseSummary(String content) throws IOException {
        JsonNode root = objectMapper.readTree(content);
        AiSummaryVO vo = new AiSummaryVO();
        vo.setSummary(text(root, "summary"));
        vo.setConfusion(text(root, "confusion"));
        vo.setEmotion(text(root, "emotion"));
        vo.setCoreQuestion(text(root, "coreQuestion"));
        vo.setDesiredOutcome(text(root, "desiredOutcome"));
        vo.setBeliefThen(text(root, "beliefThen"));
        return vo;
    }

    private String text(JsonNode root, String fieldName) {
        return normalizeOptional(root.path(fieldName).asText(null));
    }

    private boolean isCompleteSummary(AiSummaryVO summary) {
        return summary != null
                && !isBlank(summary.getSummary())
                && !isBlank(summary.getConfusion())
                && !isBlank(summary.getEmotion())
                && !isBlank(summary.getCoreQuestion())
                && !isBlank(summary.getDesiredOutcome())
                && !isBlank(summary.getBeliefThen());
    }

    private String realProviderConfigError() {
        if (isBlank(appAiProperties.getBaseUrl())
                || isBlank(appAiProperties.getModel())
                || isBlank(appAiProperties.getApiKey())) {
            return "AI服务未配置";
        }
        return null;
    }

    private AppAiProperties.Provider resolveProviderType() {
        try {
            return appAiProperties.getProviderType();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String buildSummary(String content) {
        return "当前记录主要围绕“" + preview(content, 20) + "”，建议先聚焦最影响你的那个问题。";
    }

    private String inferEmotion(String content) {
        String normalized = content.toLowerCase(Locale.ROOT);
        if (normalized.contains("焦虑") || normalized.contains("担心") || normalized.contains("害怕")) {
            return "偏焦虑，伴随对结果不确定的担心";
        }
        if (normalized.contains("迷茫") || normalized.contains("不知道")) {
            return "偏迷茫，需要先明确一个小目标";
        }
        if (normalized.contains("开心") || normalized.contains("期待")) {
            return "整体积极，带有对下一步的期待";
        }
        return "情绪相对复杂，建议先描述最强烈的那一种感受";
    }

    private String inferCoreQuestion(String content) {
        return "现在最需要先解决的问题是什么？（参考：" + preview(content, 18) + "）";
    }

    private String inferDesiredOutcome(String content) {
        return "希望先把“" + preview(content, 16) + "”相关事项推进到可执行状态";
    }

    private String inferBeliefThen(String content) {
        return "那时的我可能以为，最重要的是先解决“" + preview(content, 18) + "”带来的不确定感。";
    }

    private String preview(String value, int limit) {
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit) + "...";
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstPresent(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized == null ? fallback : normalized;
    }

    private String resolveProvider() {
        return appAiProperties.getProviderType().getConfigValue();
    }

    private String resolveProviderSafely() {
        try {
            return resolveProvider();
        } catch (IllegalArgumentException ex) {
            return "unknown";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
