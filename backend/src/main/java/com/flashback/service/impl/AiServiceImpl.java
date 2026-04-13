package com.flashback.service.impl;

import com.flashback.config.AppAiProperties;
import com.flashback.dto.AiSummarizeRecordRequest;
import com.flashback.dto.AiWritingPromptsRequest;
import com.flashback.service.AiService;
import com.flashback.vo.AiSummaryVO;
import com.flashback.vo.AiWritingPromptsVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AI 服务默认实现。
 *
 * 当前阶段默认使用 mock 生成；当 provider 非 mock 或出现异常时回落到兜底结果。
 */
@Service
public class AiServiceImpl implements AiService {

    private static final int PROMPT_LIMIT = 3;
    private static final int CONTEXT_PREVIEW_LIMIT = 20;

    private final AppAiProperties appAiProperties;

    public AiServiceImpl(AppAiProperties appAiProperties) {
        this.appAiProperties = appAiProperties;
    }

    @Override
    public AiWritingPromptsVO generateWritingPrompts(Long userId, AiWritingPromptsRequest request) {
        try {
            List<String> prompts = invokeWritingPromptsModel(request);
            if (prompts == null || prompts.isEmpty()) {
                return fallbackPrompts();
            }
            AiWritingPromptsVO vo = new AiWritingPromptsVO();
            vo.setPrompts(prompts);
            vo.setSource(resolveProvider());
            return vo;
        } catch (Exception ex) {
            return fallbackPrompts();
        }
    }

    @Override
    public AiSummaryVO summarizeRecord(Long userId, AiSummarizeRecordRequest request) {
        try {
            AiSummaryVO summary = invokeSummaryModel(request);
            if (isBlank(summary.getSummary())
                    || isBlank(summary.getConfusion())
                    || isBlank(summary.getEmotion())
                    || isBlank(summary.getCoreQuestion())
                    || isBlank(summary.getDesiredOutcome())) {
                return fallbackSummary();
            }
            summary.setSource(resolveProvider());
            return summary;
        } catch (Exception ex) {
            return fallbackSummary();
        }
    }

    private List<String> invokeWritingPromptsModel(AiWritingPromptsRequest request) {
        if (!"mock".equalsIgnoreCase(resolveProvider())) {
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
        if (!"mock".equalsIgnoreCase(resolveProvider())) {
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
        return vo;
    }

    private AiWritingPromptsVO fallbackPrompts() {
        AiWritingPromptsVO vo = new AiWritingPromptsVO();
        vo.setPrompts(List.copyOf(appAiProperties.getFallback().getWritingPrompts()));
        vo.setSource("fallback");
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
        vo.setSource("fallback");
        return vo;
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

    private String resolveProvider() {
        return normalizeOptional(appAiProperties.getProvider()) == null
                ? "mock"
                : appAiProperties.getProvider().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
