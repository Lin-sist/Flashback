package com.flashback.agent.tool;

import java.util.List;

/**
 * 从 provider tool_calls 的 arguments JSON 解析出的原始参数（C2）。
 *
 * 说明：本类刻意保持「宽松载体」——解析层只负责取出字段，
 * 合法性判断全部交给 AgentToolValidator，避免两处各判一半。
 *
 * @param askText  征询话术
 * @param text     append_record_content 的素材
 * @param tagIds   add_record_tags 的标签 id
 * @param unlockAt propose_unlock_at 的解锁时间原文
 */
public record AgentToolRawArguments(
        String askText,
        String text,
        List<Long> tagIds,
        String unlockAt) {

    public AgentToolRawArguments {
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }
}
