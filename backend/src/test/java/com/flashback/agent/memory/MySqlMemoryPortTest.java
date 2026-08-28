package com.flashback.agent.memory;

import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentSessionPurpose;
import com.flashback.domain.Record;
import com.flashback.mapper.RecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 记忆检索实现测试（C3 agent-memory-retrieval）。
 *
 * 用户隔离与「不取正文」两项按严重缺陷等级对待，各有专项用例。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MySqlMemoryPortTest {

    private static final Long USER_ID = 7001L;
    private static final Long DRAFT_ID = 88L;

    @Mock
    private RecordMapper recordMapper;

    private AppAgentProperties properties;
    private MySqlMemoryPort port;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-07-29T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        port = new MySqlMemoryPort(recordMapper, properties, clock);
    }

    private MemoryQuery query(List<String> keywords, List<Long> tagIds) {
        return new MemoryQuery(
                USER_ID, AgentSessionPurpose.WRITING_GUIDANCE, keywords, tagIds, DRAFT_ID,
                properties.getMemory().getMaxFragments());
    }

    private Record record(Long id, String aiSummary, String beliefThen, String coreQuestion, String title) {
        Record record = new Record();
        record.setId(id);
        record.setUserId(USER_ID);
        record.setAiSummary(aiSummary);
        record.setBeliefThen(beliefThen);
        record.setCoreQuestion(coreQuestion);
        record.setTitle(title);
        record.setContent("这是记录正文，检索与注入都不应该用到它");
        record.setCreatedAt(LocalDateTime.of(2026, 3, 14, 21, 30));
        return record;
    }

    // ---------- 无线索不查库 ----------

    @Test
    void shouldNotQueryWhenNoCue() {
        assertThat(port.retrieve(query(List.of(), List.of()))).isEmpty();

        verify(recordMapper, never()).selectMemoryCandidates(
                anyLong(), anyList(), anyList(), any(), any(), anyInt());
    }

    @Test
    void shouldReturnEmptyForInvalidQuery() {
        assertThat(port.retrieve(null)).isEmpty();
        assertThat(port.retrieve(new MemoryQuery(
                null, AgentSessionPurpose.WRITING_GUIDANCE, List.of("加班"), List.of(), null, 3))).isEmpty();
        assertThat(port.retrieve(new MemoryQuery(
                USER_ID, AgentSessionPurpose.WRITING_GUIDANCE, List.of("加班"), List.of(), null, 0))).isEmpty();
    }

    // ---------- 用户隔离 ----------

    @Test
    void shouldAlwaysPassUserIdToQuery() {
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of());

        port.retrieve(query(List.of("加班"), List.of(9L)));

        // userId 必须原样传入，检索层没有任何「忽略 userId」的分支。
        verify(recordMapper).selectMemoryCandidates(
                eq(USER_ID), anyList(), anyList(), eq(DRAFT_ID), any(), anyInt());
    }

    @Test
    void shouldExcludeCurrentDraftRecord() {
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of());

        port.retrieve(query(List.of("加班"), List.of()));

        ArgumentCaptor<Long> exclude = ArgumentCaptor.forClass(Long.class);
        verify(recordMapper).selectMemoryCandidates(
                anyLong(), anyList(), anyList(), exclude.capture(), any(), anyInt());
        assertThat(exclude.getValue()).isEqualTo(DRAFT_ID);
    }

    // ---------- 片段取材 ----------

    @Test
    void shouldPreferAiSummaryAsFragmentText() {
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of(record(1L, "那时在为项目排期焦虑", "我以为会顺利", "该不该换方向", "三月")));

        List<MemoryFragment> fragments = port.retrieve(query(List.of("排期"), List.of()));

        assertThat(fragments).hasSize(1);
        assertThat(fragments.get(0).text()).isEqualTo("那时在为项目排期焦虑");
    }

    @Test
    void shouldFallBackThroughFieldsWhenSummaryMissing() {
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of(
                        record(1L, null, "我以为会顺利", null, null),
                        record(2L, null, null, "该不该换方向", null),
                        record(3L, null, null, null, "三月的记录")));
        properties.getMemory().setMaxFragments(3);

        List<MemoryFragment> fragments = port.retrieve(query(List.of("方向"), List.of()));

        assertThat(fragments).extracting(MemoryFragment::text)
                .containsExactly("我以为会顺利", "该不该换方向", "三月的记录");
    }

    /**
     * 这条测试守的是隐私边界：即便记录正文可读，也不注入正文。
     * 若将来有人为「提高相关性」把 content 加进取材链，本测试会失败。
     */
    @Test
    void mustNeverUseRecordContentAsFragmentText() {
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of(record(1L, null, null, null, null)));

        List<MemoryFragment> fragments = port.retrieve(query(List.of("加班"), List.of()));

        assertThat(fragments)
                .as("没有任何说明性字段时宁可少给一条，也不退而取正文")
                .isEmpty();
    }

    @Test
    void shouldTruncateFragmentToConfiguredLength() {
        properties.getMemory().setMaxFragmentChars(5);
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of(record(1L, "一二三四五六七八九十", null, null, null)));

        List<MemoryFragment> fragments = port.retrieve(query(List.of("加班"), List.of()));

        assertThat(fragments.get(0).text()).isEqualTo("一二三四五");
    }

    @Test
    void shouldRespectMaxFragments() {
        properties.getMemory().setMaxFragments(2);
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of(
                        record(1L, "第一段", null, null, null),
                        record(2L, "第二段", null, null, null),
                        record(3L, "第三段", null, null, null)));

        assertThat(port.retrieve(query(List.of("加班"), List.of()))).hasSize(2);
    }

    // ---------- 时间锚点 ----------

    @Test
    void shouldAttachReadableTimeLabel() {
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of(record(42L, "那时在为项目排期焦虑", null, null, null)));

        MemoryFragment fragment = port.retrieve(query(List.of("排期"), List.of())).get(0);

        assertThat(fragment.timeLabel()).isEqualTo("2026年3月");
        assertThat(fragment.recordId()).isEqualTo(42L);
        assertThat(fragment.occurredAt()).isEqualTo(LocalDateTime.of(2026, 3, 14, 21, 30));
    }

    @Test
    void shouldApplyLookbackWindow() {
        properties.getMemory().setLookbackMonths(6);
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(List.of());

        port.retrieve(query(List.of("加班"), List.of()));

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(recordMapper).selectMemoryCandidates(
                anyLong(), anyList(), anyList(), any(), from.capture(), anyInt());
        assertThat(from.getValue()).isEqualTo(LocalDateTime.of(2026, 1, 29, 10, 0));
    }

    // ---------- 隐私 ----------

    @Test
    void fragmentToStringMustNotLeakText() {
        MemoryFragment fragment = new MemoryFragment(
                1L, LocalDateTime.now(), "2026年3月", "很私密的一段话", "很私密的后来说明");

        assertThat(fragment.toString())
                .as("默认 record toString 会把原文拼进字符串，一旦被日志引用即泄露")
                .doesNotContain("很私密的一段话", "很私密的后来说明");
    }

    @Test
    void shouldHandleNullCandidatesFromMapper() {
        when(recordMapper.selectMemoryCandidates(anyLong(), anyList(), anyList(), any(), any(), anyInt()))
                .thenReturn(null);

        assertThat(port.retrieve(query(List.of("加班"), List.of()))).isEmpty();
    }
}
