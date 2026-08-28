package com.flashback.agent.memory;

import com.flashback.config.AppAgentProperties;
import com.flashback.domain.Record;
import com.flashback.mapper.RecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 上的记忆检索实现（C3 agent-memory-retrieval）。
 *
 * 检索方式（proposal Q3 定稿、design 决策 5）：标签 + 时间窗 +
 * title / core_question / ai_summary / belief_then 的 LIKE 匹配。
 * **不加全文索引、不引分词器、不引外部引擎、不匹配 record.content**。
 *
 * 相关性弱于向量检索是已接受的事实（蓝图 C3 风险栏：
 * 「初期可接受；后续独立 change 升级检索能力」）。本类的职责是把这个弱相关性
 * 做得**可解释**：命中原因只有标签相同或说明性字段含关键词两种，便于人工判断是否合理。
 *
 * 片段的取材顺序（本类的关键取舍，见类内注释）：
 * ai_summary → belief_then → core_question → title。
 * 刻意**不取 content**——即便记录已解锁、正文可读，
 * 把整段日记原文注入 prompt 也会大幅扩大高敏数据的外发面，
 * 而说明性字段已足以让 Agent 知道「那时候他在为什么烦」。
 */
@Component
public class MySqlMemoryPort implements MemoryPort {

    private static final Logger log = LoggerFactory.getLogger(MySqlMemoryPort.class);

    /**
     * 候选集放大倍数。
     *
     * 为什么要放大：SQL 只能按时间倒序取，无法表达「哪条更相关」。
     * 取回比所需更多的候选，再在内存里按可解释的规则排序，
     * 比在 SQL 里堆 CASE WHEN 权重更好测，也更容易在 T-01 覆盖率结论出来后调整。
     */
    private static final int CANDIDATE_MULTIPLIER = 4;

    /** 候选集绝对上限，防止倍数放大在配置调大后失控。 */
    private static final int CANDIDATE_HARD_CAP = 40;

    private final RecordMapper recordMapper;
    private final AppAgentProperties appAgentProperties;
    private final Clock clock;

    public MySqlMemoryPort(RecordMapper recordMapper, AppAgentProperties appAgentProperties, Clock clock) {
        this.recordMapper = recordMapper;
        this.appAgentProperties = appAgentProperties;
        this.clock = clock;
    }

    @Override
    public List<MemoryFragment> retrieve(MemoryQuery query) {
        if (query == null || query.userId() == null || !query.hasCue() || query.limit() <= 0) {
            // 无线索不查库：见 MemoryQuery.hasCue 的理由。
            return List.of();
        }

        AppAgentProperties.Memory config = appAgentProperties.getMemory();
        LocalDateTime createdFrom = LocalDateTime.now(clock).minusMonths(config.getLookbackMonths());
        int candidateLimit = Math.min(query.limit() * CANDIDATE_MULTIPLIER, CANDIDATE_HARD_CAP);

        List<Record> candidates = recordMapper.selectMemoryCandidates(
                query.userId(),
                query.keywords(),
                query.tagIds(),
                query.excludeRecordId(),
                createdFrom,
                candidateLimit);

        if (candidates == null || candidates.isEmpty()) {
            log.debug("agent memory retrieval empty userId={} keywords={} tagIds={}",
                    query.userId(), query.keywords().size(), query.tagIds().size());
            return List.of();
        }

        List<MemoryFragment> fragments = new ArrayList<>();
        for (Record record : candidates) {
            if (fragments.size() >= query.limit()) {
                break;
            }
            String text = fragmentTextOf(record, config.getMaxFragmentChars());
            if (text == null) {
                // 命中了记录，但没有任何可注入的说明性字段。
                // 宁可少给一条，也不退而取 content。
                continue;
            }
            LocalDateTime occurredAt = occurredAtOf(record);
            fragments.add(new MemoryFragment(
                    record.getId(), occurredAt, timeLabelOf(occurredAt), text,
                    blankToNull(record.getAgentMemoryContextNote())));
        }

        // 只记结构化指标，不记片段内容（agent-runtime delta 的留痕条款）。
        log.debug("agent memory retrieval userId={} candidates={} injected={}",
                query.userId(), candidates.size(), fragments.size());
        return List.copyOf(fragments);
    }

    /**
     * 选取用于注入的片段文本。
     *
     * 优先级来自「信息密度」而非字段长度：ai_summary 是对整条记录的结构化整理，
     * belief_then 是「当时以为」——两者最接近「那时候他在想什么」；
     * core_question 与 title 更短，作为兜底。
     *
     * **不取 content**：见类注释。
     */
    private String fragmentTextOf(Record record, int maxChars) {
        String text = firstNonBlank(
                record.getAiSummary(),
                record.getBeliefThen(),
                record.getCoreQuestion(),
                record.getTitle());
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.length() <= maxChars ? normalized : normalized.substring(0, maxChars);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 记录的「发生时间」。
     *
     * 用 created_at 而非 sealed_at / unlocked_at：用户关心的是「我什么时候写下这些的」，
     * 而不是系统什么时候处理的。这也让时间标签与时光轴上看到的时间一致。
     */
    private LocalDateTime occurredAtOf(Record record) {
        return record.getCreatedAt();
    }

    /**
     * 生成注入 prompt 用的可读时间标签。
     *
     * 精度只到月：日期精确到天会让 Agent 说出「你在 3 月 14 日写过」这种
     * 像系统查询结果而不像朋友回忆的话，与产品气质冲突（蓝图 §6.2）。
     */
    private String timeLabelOf(LocalDateTime occurredAt) {
        if (occurredAt == null) {
            return "以前";
        }
        return occurredAt.getYear() + "年" + occurredAt.getMonthValue() + "月";
    }
}
