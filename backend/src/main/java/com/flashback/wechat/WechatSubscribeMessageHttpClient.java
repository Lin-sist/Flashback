package com.flashback.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.config.AppWechatProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal WeChat subscribe-message sender for M3 unlock reminders.
 */
@Component
public class WechatSubscribeMessageHttpClient implements WechatSubscribeMessageClient {

    private static final DateTimeFormatter REMINDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Logger LOGGER = LoggerFactory.getLogger(WechatSubscribeMessageHttpClient.class);

    private final AppWechatProperties properties;
    private final ObjectMapper objectMapper;

    public WechatSubscribeMessageHttpClient(AppWechatProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendUnlockReminder(String openid, Long recordId, LocalDateTime unlockedAt) {
        String accessToken = fetchAccessToken();
        URI uri = UriComponentsBuilder.fromUriString(properties.getSubscribeMessageSendUrl())
                .queryParam("access_token", accessToken)
                .build()
                .toUri();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openid);
        body.put("template_id", properties.getUnlockReminderTemplateId());
        body.put("page", buildRecordDetailPage(recordId));
        body.put("data", buildUnlockReminderData(unlockedAt));

        sendJson(uri, body, "微信订阅消息发送失败");
    }

    private String fetchAccessToken() {
        URI uri = UriComponentsBuilder.fromUriString(properties.getAccessTokenUrl())
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getSecret())
                .build()
                .toUri();
        JsonNode root = sendGet(uri, "微信 access_token 获取失败");
        String accessToken = text(root, "access_token");
        if (accessToken == null) {
            throw unavailable("微信 access_token 缺失");
        }
        return accessToken;
    }

    private JsonNode sendGet(URI uri, String failureMessage) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(properties.getTimeoutMillis()))
                .GET()
                .build();
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return parseWechatResponse(response, failureMessage);
        } catch (IOException ex) {
            throw unavailable(failureMessage);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable(failureMessage);
        }
    }

    private void sendJson(URI uri, Map<String, Object> body, String failureMessage) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(properties.getTimeoutMillis()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            parseWechatResponse(response, failureMessage);
        } catch (IOException ex) {
            throw unavailable(failureMessage);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable(failureMessage);
        }
    }

    private JsonNode parseWechatResponse(HttpResponse<String> response, String failureMessage) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOGGER.warn("WeChat API returned non-2xx status={}, operation={}", response.statusCode(), failureMessage);
            throw unavailable(failureMessage);
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
            LOGGER.warn(
                    "WeChat API rejected request, operation={}, errcode={}, errmsg={}",
                    failureMessage,
                    root.get("errcode").asInt(),
                    text(root, "errmsg"));
            throw unavailable(failureMessage);
        }
        return root;
    }

    private Map<String, Object> buildUnlockReminderData(LocalDateTime unlockedAt) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(
                requiredTemplateDataKey(properties.getUnlockReminderThingKey(), "thing1"),
                Map.of("value", "有一条记录已经抵达"));
        data.put(
                requiredTemplateDataKey(properties.getUnlockReminderTimeKey(), "time2"),
                Map.of("value", unlockedAt == null ? "" : unlockedAt.format(REMINDER_TIME_FORMATTER)));
        return data;
    }

    private String requiredTemplateDataKey(String configuredKey, String fallbackKey) {
        String key = normalize(configuredKey);
        return key == null ? fallbackKey : key;
    }

    private String buildRecordDetailPage(Long recordId) {
        String page = normalize(properties.getUnlockReminderPage());
        if (page == null) {
            page = "pages/record-detail/index";
        }
        if (recordId == null) {
            return page;
        }
        return page + (page.contains("?") ? "&" : "?") + "id=" + recordId;
    }

    private String text(JsonNode root, String field) {
        if (!root.hasNonNull(field)) {
            return null;
        }
        return normalize(root.get(field).asText());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private BizException unavailable(String message) {
        return new BizException(ErrorCode.INTERNAL_ERROR, HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
