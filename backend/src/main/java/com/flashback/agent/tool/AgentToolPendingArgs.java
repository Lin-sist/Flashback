package com.flashback.agent.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 待确认提议的瞬态执行参数序列化（C2）。
 *
 * 为什么需要它（design.md 决策 6 的落地细节）：
 * 执行 append_record_content 需要原始 text，但审计表按决策 6 只存不可还原的摘要。
 * 三种可选做法：
 * (a) 让前端在 confirm 时回传参数 —— 等于让客户端绕过白名单与校验，否决；
 * (b) 存在内存 —— 重启即丢、多实例不可用，否决；
 * (c) 存为**瞬态**列，提议终结时由 SQL 置 NULL —— 采用。
 *
 * 因此 pending_args 只在 PROPOSED 期间存在，审计表不留日记文本的长期副本。
 */
public final class AgentToolPendingArgs {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private AgentToolPendingArgs() {
    }

    /**
     * 序列化执行所需参数。askText 不在其中——它已单列存放。
     */
    public static String serialize(AgentToolProposal proposal) {
        if (proposal == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        switch (proposal.tool()) {
            case APPEND_RECORD_CONTENT -> payload.put(AgentToolRegistry.PARAM_TEXT, proposal.text());
            case ADD_RECORD_TAGS -> payload.put(AgentToolRegistry.PARAM_TAG_IDS, proposal.tagIds());
            case PROPOSE_UNLOCK_AT -> payload.put(AgentToolRegistry.PARAM_UNLOCK_AT, proposal.unlockAt());
            default -> {
                return null;
            }
        }
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 还原提议。参数缺失或损坏时返回 null，由调用方按「提议已失效」显式失败处理。
     */
    public static AgentToolProposal deserialize(AgentToolName tool, String askText, String pendingArgs) {
        if (tool == null || pendingArgs == null || pendingArgs.isBlank()) {
            return null;
        }
        Map<String, Object> payload;
        try {
            payload = MAPPER.readValue(pendingArgs, MAP_TYPE);
        } catch (Exception ex) {
            return null;
        }
        return switch (tool) {
            case APPEND_RECORD_CONTENT -> {
                String text = asString(payload.get(AgentToolRegistry.PARAM_TEXT));
                yield text == null ? null : AgentToolProposal.appendContent(askText, text);
            }
            case ADD_RECORD_TAGS -> {
                List<Long> tagIds = asLongList(payload.get(AgentToolRegistry.PARAM_TAG_IDS));
                yield tagIds.isEmpty() ? null : AgentToolProposal.addTags(askText, tagIds);
            }
            case PROPOSE_UNLOCK_AT -> {
                String unlockAt = asString(payload.get(AgentToolRegistry.PARAM_UNLOCK_AT));
                yield unlockAt == null ? null : AgentToolProposal.proposeUnlockAt(askText, unlockAt);
            }
            default -> null;
        };
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static List<Long> asLongList(Object value) {
        if (!(value instanceof List<?> raw)) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof Number number) {
                result.add(number.longValue());
            }
        }
        return List.copyOf(result);
    }
}
