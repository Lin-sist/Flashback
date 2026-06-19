package com.flashback.service.impl;

import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.dto.AiSummarizeRecordRequest;
import com.flashback.mapper.RecordMapper;
import com.flashback.service.AiService;
import com.flashback.service.StageSummaryService;
import com.flashback.vo.AiSummaryVO;
import com.flashback.vo.StageSummaryVO;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight on-demand stage summary for the M3 demo.
 */
@Service
public class StageSummaryServiceImpl implements StageSummaryService {

    private static final int RECENT_RECORD_LIMIT = 20;
    private static final int AI_CONTEXT_LIMIT = 5000;

    private final RecordMapper recordMapper;
    private final AiService aiService;
    private final Clock clock;

    public StageSummaryServiceImpl(RecordMapper recordMapper, AiService aiService, Clock clock) {
        this.recordMapper = recordMapper;
        this.aiService = aiService;
        this.clock = clock;
    }

    @Override
    public StageSummaryVO generate(Long userId) {
        long recordCount = recordMapper.countByUserAndCondition(userId, null, null, null, null);
        long unlockedCount = recordMapper.countByUserAndCondition(userId, RecordStatus.UNLOCKED, null, null, null);
        long lifeNodeCount = recordMapper.countByUserAndCondition(userId, null, RecordType.NODE_RECORD, null, null);
        List<Record> recentRecords = recordMapper.selectPageByUserAndCondition(
                userId,
                null,
                null,
                null,
                null,
                0,
                RECENT_RECORD_LIMIT);

        StageSummaryVO vo = new StageSummaryVO();
        vo.setRecordCount(recordCount);
        vo.setUnlockedCount(unlockedCount);
        vo.setLifeNodeCount(lifeNodeCount);
        vo.setGeneratedAt(LocalDateTime.now(clock));
        applyAiOrFallbackSummary(vo, userId, recordCount, unlockedCount, lifeNodeCount, recentRecords);
        return vo;
    }

    private void applyAiOrFallbackSummary(
            StageSummaryVO vo,
            Long userId,
            long recordCount,
            long unlockedCount,
            long lifeNodeCount,
            List<Record> recentRecords) {
        AiSummarizeRecordRequest request = new AiSummarizeRecordRequest();
        request.setCoreQuestion("请整理这一阶段的记录与抵达，帮助用户理解当时的自己");
        request.setContent(buildAiContext(recordCount, unlockedCount, lifeNodeCount, recentRecords));

        AiSummaryVO aiSummary = aiService.summarizeRecord(userId, request);
        if (aiSummary != null && "SUCCESS".equals(aiSummary.getStatus()) && hasText(aiSummary.getSummary())) {
            vo.setSummary(aiSummary.getSummary());
            vo.setSource(aiSummary.getSource());
            vo.setStatus("SUCCESS");
            return;
        }

        vo.setSource("fallback");
        vo.setStatus("FALLBACK");
        vo.setMessage(aiSummary == null || !hasText(aiSummary.getMessage())
                ? "AI暂不可用，已使用本地总结"
                : aiSummary.getMessage());
        vo.setSummary(buildSummary(recordCount, unlockedCount, lifeNodeCount, recentRecords));
    }

    private String buildAiContext(
            long recordCount,
            long unlockedCount,
            long lifeNodeCount,
            List<Record> recentRecords) {
        StringBuilder context = new StringBuilder();
        context.append("记录总数：").append(recordCount)
                .append("；已抵达：").append(unlockedCount)
                .append("；人生节点：").append(lifeNodeCount).append("。\n");
        if (recentRecords != null) {
            for (Record record : recentRecords) {
                appendRecordContext(context, record);
                if (context.length() >= AI_CONTEXT_LIMIT) {
                    break;
                }
            }
        }
        return context.substring(0, Math.min(context.length(), AI_CONTEXT_LIMIT));
    }

    private void appendRecordContext(StringBuilder context, Record record) {
        context.append("记录：")
                .append(preview(firstPresent(record.getTitle(), record.getContent()), 80));
        if (hasText(record.getBeliefThen())) {
            context.append("；那时以为：").append(preview(record.getBeliefThen(), 120));
        }
        if (hasText(record.getRealityLater())) {
            context.append("；后来其实：").append(preview(record.getRealityLater(), 120));
        }
        context.append("。\n");
    }

    private String buildSummary(long recordCount, long unlockedCount, long lifeNodeCount, List<Record> recentRecords) {
        if (recordCount == 0) {
            return "这一阶段还没有记录。可以先从写下此刻开始，留给未来的自己一个安静的起点。";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("这一阶段你留下了 ")
                .append(recordCount)
                .append(" 条记录，其中 ")
                .append(unlockedCount)
                .append(" 条已经抵达，")
                .append(lifeNodeCount)
                .append(" 条与人生节点有关。");

        Record latest = recentRecords == null || recentRecords.isEmpty() ? null : recentRecords.get(0);
        if (latest != null) {
            summary.append(" 最近的一条记录围绕“")
                    .append(preview(firstPresent(latest.getTitle(), latest.getContent()), 24))
                    .append("”。");
            if (hasText(latest.getBeliefThen())) {
                summary.append(" 那时的你曾以为：")
                        .append(preview(latest.getBeliefThen(), 36))
                        .append("。");
            }
            if (hasText(latest.getRealityLater())) {
                summary.append(" 后来你补充了：")
                        .append(preview(latest.getRealityLater(), 36))
                        .append("。");
            }
        }

        summary.append(" 可以带着这些痕迹，继续温柔地理解当时的自己。");
        return summary.toString();
    }

    private String firstPresent(String first, String second) {
        return hasText(first) ? first.trim() : second == null ? "" : second.trim();
    }

    private String preview(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
