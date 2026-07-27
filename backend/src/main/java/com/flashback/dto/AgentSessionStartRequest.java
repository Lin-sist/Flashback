package com.flashback.dto;

/**
 * 开启或恢复 Agent 会话请求。
 *
 * recordId 可选：指向用户自己的草稿记录；为空表示不与具体记录关联。
 */
public class AgentSessionStartRequest {

    private Long recordId;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }
}
