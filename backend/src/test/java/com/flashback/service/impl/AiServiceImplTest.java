package com.flashback.service.impl;

import com.flashback.config.AppAiProperties;
import com.flashback.dto.AiSummarizeRecordRequest;
import com.flashback.dto.AiWritingPromptsRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiServiceImplTest {

    @Test
    void shouldReturnMockWritingPromptsWhenProviderIsMock() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("mock");
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

        assertThat(result.getSource()).isEqualTo("fallback");
        assertThat(result.getPrompts()).isEqualTo(properties.getFallback().getWritingPrompts());
    }

    @Test
    void shouldReturnMockSummaryWhenProviderIsMock() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("mock");
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
    }

    @Test
    void shouldFallbackSummaryWhenProviderUnsupported() {
        AppAiProperties properties = new AppAiProperties();
        properties.setProvider("openai");
        AiServiceImpl aiService = new AiServiceImpl(properties);

        AiSummarizeRecordRequest request = new AiSummarizeRecordRequest();
        request.setContent("内容");

        var result = aiService.summarizeRecord(5001L, request);

        assertThat(result.getSource()).isEqualTo("fallback");
        assertThat(result.getSummary()).isEqualTo(properties.getFallback().getSummary());
        assertThat(result.getConfusion()).isEqualTo(properties.getFallback().getConfusion());
        assertThat(result.getEmotion()).isEqualTo(properties.getFallback().getEmotion());
        assertThat(result.getCoreQuestion()).isEqualTo(properties.getFallback().getCoreQuestion());
        assertThat(result.getDesiredOutcome()).isEqualTo(properties.getFallback().getDesiredOutcome());
    }
}
