package com.flashback.agent.tool;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 工具参数的结构化摘要（C2）。
 *
 * 隐私边界（design.md 决策 6）：
 * append_record_content 的 text 直接来自用户日记语境，属高敏数据。
 * 已接受的 agent-runtime spec 规定日记原文只存在于被授权的业务存储
 * （agent_message / record.content），审计表不是其中之一。
 *
 * 因此本类只产出「长度 + 哈希前缀」这类可支撑判重与定位、但无法还原原文的摘要，
 * **禁止**把 text 原文写入返回值。
 */
public final class AgentToolArgsDigest {

    /** 哈希前缀长度：足够判重，又不足以支撑碰撞式还原。 */
    private static final int HASH_PREFIX_LENGTH = 12;

    private AgentToolArgsDigest() {
    }

    /**
     * 为提议生成审计摘要。
     */
    public static String of(AgentToolProposal proposal) {
        if (proposal == null) {
            return null;
        }
        return switch (proposal.tool()) {
            case APPEND_RECORD_CONTENT -> "text:len=" + lengthOf(proposal.text())
                    + ",sha256=" + hashPrefix(proposal.text());
            case ADD_RECORD_TAGS -> "tagIds=" + joinTagIds(proposal.tagIds());
            case PROPOSE_UNLOCK_AT -> "unlockAt=" + nullSafe(proposal.unlockAt());
            default -> "tool=" + proposal.tool().wireName();
        };
    }

    /**
     * 为被拒绝的提议生成摘要：只记工具名与拒绝原因，不记参数。
     */
    public static String ofRejected(String wireName, String rejectReason) {
        return "tool=" + nullSafe(wireName) + ",reject=" + nullSafe(rejectReason);
    }

    private static int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }

    private static String joinTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < tagIds.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(tagIds.get(i));
        }
        return builder.append(']').toString();
    }

    private static String hashPrefix(String value) {
        if (value == null) {
            return "none";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
                if (hex.length() >= HASH_PREFIX_LENGTH) {
                    break;
                }
            }
            return hex.substring(0, Math.min(HASH_PREFIX_LENGTH, hex.length()));
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 是 JDK 必备算法；真出现则退化为不可还原的占位符，绝不回退到原文。
            return "unavailable";
        }
    }

    private static String nullSafe(String value) {
        return value == null ? "none" : value;
    }
}
