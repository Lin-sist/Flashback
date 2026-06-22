package com.flashback.service.impl;

import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.mapper.RecordMapper;
import com.flashback.service.AiService;
import com.flashback.vo.AiSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StageSummaryServiceImplTest {

    @Mock
    private RecordMapper recordMapper;

    @Mock
    private AiService aiService;

    private StageSummaryServiceImpl stageSummaryService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-07T04:00:00Z"), ZoneId.of("Asia/Shanghai"));
        stageSummaryService = new StageSummaryServiceImpl(recordMapper, aiService, clock);
    }

    @Test
    void shouldGenerateUserScopedSummaryFromRecentRecords() {
        Record latest = new Record();
        latest.setId(1L);
        latest.setUserId(100L);
        latest.setTitle("毕业前的选择");
        latest.setContent("最近在想毕业之后要去哪里");
        latest.setRecordType(RecordType.NODE_RECORD);
        latest.setStatus(RecordStatus.UNLOCKED);
        latest.setBeliefThen("那时以为只要拿到一个确定答案就不会焦虑");
        latest.setRealityLater("后来其实我可以先允许自己慢慢选择");

        when(recordMapper.countByUserAndCondition(100L, null, null, null, null)).thenReturn(3L);
        when(recordMapper.countByUserAndCondition(100L, RecordStatus.UNLOCKED, null, null, null)).thenReturn(1L);
        when(recordMapper.countByUserAndCondition(100L, null, RecordType.NODE_RECORD, null, null)).thenReturn(2L);
        when(recordMapper.selectPageByUserAndCondition(100L, null, null, null, null, 0, 20))
                .thenReturn(List.of(latest));
        AiSummaryVO unavailable = new AiSummaryVO();
        unavailable.setStatus("UNAVAILABLE");
        unavailable.setMessage("AI服务未配置");
        when(aiService.generateStageSummary(eq(100L), anyString())).thenReturn(unavailable);

        var result = stageSummaryService.generate(100L);

        assertThat(result.getRecordCount()).isEqualTo(3L);
        assertThat(result.getUnlockedCount()).isEqualTo(1L);
        assertThat(result.getLifeNodeCount()).isEqualTo(2L);
        assertThat(result.getGeneratedAt()).isEqualTo(LocalDateTime.of(2026, 6, 7, 12, 0, 0));
        assertThat(result.getSource()).isEqualTo("fallback");
        assertThat(result.getStatus()).isEqualTo("FALLBACK");
        assertThat(result.getMessage()).isEqualTo("AI服务未配置");
        assertThat(result.getSummary()).contains("这一阶段你留下了 3 条记录");
        assertThat(result.getSummary()).contains("毕业前的选择");
        assertThat(result.getSummary()).contains("那时的你曾以为");
        assertThat(result.getSummary()).contains("后来你补充了");
        verify(aiService).generateStageSummary(eq(100L), anyString());
    }

    @Test
    void shouldUseRealProviderSummaryWhenAiSucceeds() {
        when(recordMapper.countByUserAndCondition(100L, null, null, null, null)).thenReturn(2L);
        when(recordMapper.countByUserAndCondition(100L, RecordStatus.UNLOCKED, null, null, null)).thenReturn(1L);
        when(recordMapper.countByUserAndCondition(100L, null, RecordType.NODE_RECORD, null, null)).thenReturn(1L);
        when(recordMapper.selectPageByUserAndCondition(100L, null, null, null, null, 0, 20))
                .thenReturn(List.of());
        AiSummaryVO aiSummary = new AiSummaryVO();
        aiSummary.setSummary("这一阶段，你在不确定里慢慢确认了自己的方向。");
        aiSummary.setSource("deepseek");
        aiSummary.setStatus("SUCCESS");
        when(aiService.generateStageSummary(eq(100L), anyString())).thenReturn(aiSummary);

        var result = stageSummaryService.generate(100L);

        assertThat(result.getSummary()).isEqualTo("这一阶段，你在不确定里慢慢确认了自己的方向。");
        assertThat(result.getSource()).isEqualTo("deepseek");
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getMessage()).isNull();
    }

    @Test
    void shouldReturnSafeSummaryWhenNoRecordsExist() {
        when(recordMapper.countByUserAndCondition(100L, null, null, null, null)).thenReturn(0L);
        when(recordMapper.countByUserAndCondition(100L, RecordStatus.UNLOCKED, null, null, null)).thenReturn(0L);
        when(recordMapper.countByUserAndCondition(100L, null, RecordType.NODE_RECORD, null, null)).thenReturn(0L);
        when(recordMapper.selectPageByUserAndCondition(100L, null, null, null, null, 0, 20))
                .thenReturn(List.of());
        when(aiService.generateStageSummary(eq(100L), anyString())).thenReturn(null);

        var result = stageSummaryService.generate(100L);

        assertThat(result.getSummary()).contains("这一阶段还没有记录");
        assertThat(result.getRecordCount()).isZero();
        assertThat(result.getStatus()).isEqualTo("FALLBACK");
        assertThat(result.getMessage()).isEqualTo("AI暂不可用，已使用本地总结");
    }
}
