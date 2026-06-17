package com.flashback.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 配置项。
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AppAiProperties {

    @NotBlank
    private String provider = "mock";

    @NotBlank
    private String baseUrl = "https://api.deepseek.com";

    private String apiKey = "";

    @NotBlank
    private String model = "deepseek-v4-pro";

    @Positive
    private long timeoutMillis = 10000;

    private boolean realModeMockEnabled = false;

    @Valid
    private Fallback fallback = new Fallback();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Provider getProviderType() {
        return Provider.fromConfigValue(provider);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public boolean isRealModeMockEnabled() {
        return realModeMockEnabled;
    }

    public void setRealModeMockEnabled(boolean realModeMockEnabled) {
        this.realModeMockEnabled = realModeMockEnabled;
    }

    public Fallback getFallback() {
        return fallback;
    }

    public void setFallback(Fallback fallback) {
        this.fallback = fallback;
    }

    public enum Provider {
        MOCK("mock"),
        DEEPSEEK("deepseek"),
        OPENAI_COMPATIBLE("openai-compatible");

        private final String configValue;

        Provider(String configValue) {
            this.configValue = configValue;
        }

        public String getConfigValue() {
            return configValue;
        }

        public static Provider fromConfigValue(String value) {
            if (value == null || value.trim().isEmpty()) {
                return MOCK;
            }
            String normalized = value.trim();
            for (Provider provider : values()) {
                if (provider.configValue.equalsIgnoreCase(normalized)
                        || provider.name().equalsIgnoreCase(normalized.replace('-', '_'))) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("Unsupported AI provider: " + value);
        }
    }

    public static class Fallback {

        @NotEmpty
        private List<String> writingPrompts = new ArrayList<>(List.of(
                "你此刻最担心的是什么？",
                "如果三个月后回看现在，你希望自己记住什么？",
                "今天最想先推进的一件小事是什么？"));

        @NotBlank
        private String confusion = "待补充";

        @NotBlank
        private String summary = "建议先把当前状态整理成一句简短总结";

        @NotBlank
        private String emotion = "待整理";

        @NotBlank
        private String coreQuestion = "暂无";

        @NotBlank
        private String desiredOutcome = "暂无";

        public List<String> getWritingPrompts() {
            return writingPrompts;
        }

        public void setWritingPrompts(List<String> writingPrompts) {
            this.writingPrompts = writingPrompts;
        }

        public String getConfusion() {
            return confusion;
        }

        public void setConfusion(String confusion) {
            this.confusion = confusion;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getEmotion() {
            return emotion;
        }

        public void setEmotion(String emotion) {
            this.emotion = emotion;
        }

        public String getCoreQuestion() {
            return coreQuestion;
        }

        public void setCoreQuestion(String coreQuestion) {
            this.coreQuestion = coreQuestion;
        }

        public String getDesiredOutcome() {
            return desiredOutcome;
        }

        public void setDesiredOutcome(String desiredOutcome) {
            this.desiredOutcome = desiredOutcome;
        }
    }
}
