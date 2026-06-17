package com.flashback.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppAiPropertiesTest {

    @Test
    void shouldExposeAcceptedDefaultAiProviderConfig() {
        AppAiProperties properties = new AppAiProperties();

        assertThat(properties.getProvider()).isEqualTo("mock");
        assertThat(properties.getProviderType()).isEqualTo(AppAiProperties.Provider.MOCK);
        assertThat(properties.getBaseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(properties.getApiKey()).isEmpty();
        assertThat(properties.getModel()).isEqualTo("deepseek-v4-pro");
        assertThat(properties.getTimeoutMillis()).isEqualTo(10000);
        assertThat(properties.isRealModeMockEnabled()).isFalse();
    }

    @Test
    void shouldResolveAcceptedProviderValues() {
        assertThat(AppAiProperties.Provider.fromConfigValue("mock"))
                .isEqualTo(AppAiProperties.Provider.MOCK);
        assertThat(AppAiProperties.Provider.fromConfigValue("deepseek"))
                .isEqualTo(AppAiProperties.Provider.DEEPSEEK);
        assertThat(AppAiProperties.Provider.fromConfigValue("openai-compatible"))
                .isEqualTo(AppAiProperties.Provider.OPENAI_COMPATIBLE);
        assertThat(AppAiProperties.Provider.fromConfigValue("OPENAI_COMPATIBLE"))
                .isEqualTo(AppAiProperties.Provider.OPENAI_COMPATIBLE);
    }
}
