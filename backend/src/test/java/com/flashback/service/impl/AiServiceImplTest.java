package com.flashback.service.impl;

import com.flashback.config.AppAiProperties;
import com.flashback.dto.AiSummarizeRecordRequest;
import com.flashback.dto.AiWritingPromptsRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiServiceImplTest {

    @Test
    void shouldReturnMockWritingPromptsWhenProviderIsMock() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("mock");
        properties.setRealModeMockEnabled(true);
        AiServiceImpl aiService = new AiServiceImpl(properties);

        AiWritingPromptsRequest request = new AiWritingPromptsRequest();
        request.setContent("秋招压力很大，不知道怎么开始");
        request.setCoreQuestion("我该先投简历还是补项目");
        request.setRecordType("NODE_RECORD");

        var result = aiService.generateWritingPrompts(5001L, request);

        assertThat(result.getSource()).isEqualTo("mock");
        assertThat(result.getPrompts()).hasSize(3);
        assertThat(result.getPrompts().get(0)).contains("NODE_RECORD");
        assertThat(result.getPrompts().get(1)).contains("我该先投简历还是补项目");
    }

    @Test
    void shouldFallbackWritingPromptsWhenProviderUnsupported() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("openai");
        AiServiceImpl aiService = new AiServiceImpl(properties);

        AiWritingPromptsRequest request = new AiWritingPromptsRequest();
        request.setContent("test");
        request.setRecordType("NODE_RECORD");

        var result = aiService.generateWritingPrompts(5001L, request);

        assertThat(result.getSource()).isEqualTo("unknown");
        assertThat(result.getStatus()).isEqualTo("UNAVAILABLE");
        assertThat(result.getPrompts()).isEmpty();
    }

    @Test
    void shouldReturnMockSummaryWhenProviderIsMock() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("mock");
        properties.setRealModeMockEnabled(true);
        AiServiceImpl aiService = new AiServiceImpl(properties);

        AiSummarizeRecordRequest request = new AiSummarizeRecordRequest();
        request.setContent("最近有点焦虑，担心秋招准备不够，想尽快形成自己的节奏。");
        request.setCoreQuestion("我应该先做哪件事？");

        var result = aiService.summarizeRecord(5001L, request);

        assertThat(result.getSource()).isEqualTo("mock");
        assertThat(result.getSummary()).contains("当前记录主要围绕");
        assertThat(result.getEmotion()).contains("焦虑");
        assertThat(result.getCoreQuestion()).isEqualTo("我应该先做哪件事？");
        assertThat(result.getDesiredOutcome()).isNotBlank();
        assertThat(result.getBeliefThen()).contains("那时的我可能以为");
    }

    @Test
    void shouldReturnUnavailableWhenMockProviderNotExplicitlyEnabled() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("mock");
        properties.setRealModeMockEnabled(false);
        AiServiceImpl aiService = new AiServiceImpl(properties);

        AiWritingPromptsRequest request = new AiWritingPromptsRequest();
        request.setContent("真实路径不应返回mock成功");

        var result = aiService.generateWritingPrompts(5001L, request);

        assertThat(result.getSource()).isEqualTo("mock");
        assertThat(result.getStatus()).isEqualTo("UNAVAILABLE");
        assertThat(result.getMessage()).isEqualTo("AI mock provider未启用");
        assertThat(result.getPrompts()).isEmpty();
    }

    @Test
    void shouldFallbackSummaryWhenProviderUnsupported() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("openai");
        AiServiceImpl aiService = new AiServiceImpl(properties);

        AiSummarizeRecordRequest request = new AiSummarizeRecordRequest();
        request.setContent("内容");

        var result = aiService.summarizeRecord(5001L, request);

        assertThat(result.getSource()).isEqualTo("unknown");
        assertThat(result.getStatus()).isEqualTo("UNAVAILABLE");
        assertThat(result.getMessage()).contains("provider");
    }

    @Test
    void shouldReturnUnavailableWhenRealProviderMissingApiKey() {
        AppAiProperties properties = realProviderProperties();
        properties.setApiKey("");
        AiServiceImpl aiService = new AiServiceImpl(properties);

        AiWritingPromptsRequest request = new AiWritingPromptsRequest();
        request.setContent("内容");

        var result = aiService.generateWritingPrompts(5001L, request);

        assertThat(result.getSource()).isEqualTo("deepseek");
        assertThat(result.getStatus()).isEqualTo("UNAVAILABLE");
        assertThat(result.getMessage()).isEqualTo("AI服务未配置");
        assertThat(result.getPrompts()).isEmpty();
    }

    @Test
    void shouldReturnFailedWhenRealProviderCallFails() {
        AppAiProperties properties = realProviderProperties();
        AiServiceImpl aiService = new StubAiService(properties, null, new IOException("upstream unavailable"));

        AiSummarizeRecordRequest request = new AiSummarizeRecordRequest();
        request.setContent("内容");

        var result = aiService.summarizeRecord(5001L, request);

        assertThat(result.getSource()).isEqualTo("deepseek");
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getMessage()).isEqualTo("AI服务暂时不可用");
    }

    @Test
    void shouldParseOpenAiCompatiblePromptResponse() {
        AppAiProperties properties = realProviderProperties();
        AiServiceImpl aiService = new StubAiService(
                properties,
                "{\"prompts\":[\"先写下最在意的一点？\",\"这一刻最想保留什么？\",\"未来的你需要知道什么？\"]}",
                null);

        AiWritingPromptsRequest request = new AiWritingPromptsRequest();
        request.setContent("最近有点焦虑");

        var result = aiService.generateWritingPrompts(5001L, request);

        assertThat(result.getSource()).isEqualTo("deepseek");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getPrompts()).containsExactly(
                "先写下最在意的一点？",
                "这一刻最想保留什么？",
                "未来的你需要知道什么？");
    }

    @Test
    void shouldParseOpenAiCompatibleSummaryResponse() {
        AppAiProperties properties = realProviderProperties();
        AiServiceImpl aiService = new StubAiService(
                properties,
                """
                        {
                          "summary": "这段记录在整理当下的焦虑",
                          "confusion": "担心准备不够",
                          "emotion": "焦虑但仍想推进",
                          "coreQuestion": "下一步先做什么",
                          "desiredOutcome": "形成一周计划",
                          "beliefThen": "那时的我以为只要足够快就不会落后"
                        }
                        """,
                null);

        AiSummarizeRecordRequest request = new AiSummarizeRecordRequest();
        request.setContent("最近有点焦虑");

        var result = aiService.summarizeRecord(5001L, request);

        assertThat(result.getSource()).isEqualTo("deepseek");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getSummary()).isEqualTo("这段记录在整理当下的焦虑");
        assertThat(result.getBeliefThen()).isEqualTo("那时的我以为只要足够快就不会落后");
    }

    private AppAiProperties realProviderProperties() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("deepseek");
        properties.setBaseUrl("https://api.deepseek.com");
        properties.setApiKey("test-key");
        properties.setModel("deepseek-v4-pro");
        return properties;
    }

    private static class StubAiService extends AiServiceImpl {

        private final String responseContent;
        private final Exception exception;

        StubAiService(AppAiProperties appAiProperties, String responseContent, Exception exception) {
            super(appAiProperties);
            this.responseContent = responseContent;
            this.exception = exception;
        }

        @Override
        protected String invokeChatCompletion(List<Map<String, String>> messages) throws IOException, InterruptedException {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            if (exception instanceof InterruptedException interruptedException) {
                throw interruptedException;
            }
            return responseContent;
        }
    }
}
