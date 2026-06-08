package com.flashback.service.impl;

import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.mapper.RecordMapper;
import com.flashback.service.StageSummaryService;
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

    private final RecordMapper recordMapper;
    private final Clock clock;

    public StageSummaryServiceImpl(RecordMapper recordMapper, Clock clock) {
        this.recordMapper = recordMapper;
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
        vo.setSource("fallback");
        vo.setSummary(buildSummary(recordCount, unlockedCount, lifeNodeCount, recentRecords));
        return vo;
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
