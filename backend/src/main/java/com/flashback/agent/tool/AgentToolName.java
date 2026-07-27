package com.flashback.agent.tool;

/**
 * Agent 工具白名单枚举（C2）。
 *
 * 边界（design.md §3、决策 3）：
 * - 本枚举是 Agent 可达工具的**全集**，枚举外的任何后端写操作对 Agent 不可达；
 * - seal / delete / unlock / location / cover / attachment / later-reflection /
 * unlock-reminder-authorization / 标签创建 **一律不在此枚举中**，Agent 只能以自然语言建议；
 * - 写工具会下发给 provider 作为 function calling tools；
 * - 读工具只作为白名单与审计的完整视图存在，数据由后端在组装 prompt 时预注入
 * （design §3.1：不做单轮内 FC 循环，故读工具无法通过模型调用获取）。
 */
public enum AgentToolName {

    /** 把对话中整理出的素材追加到草稿正文末尾（只追加，不覆写）。 */
    APPEND_RECORD_CONTENT("append_record_content", true),

    /** 在草稿既有标签基础上追加标签（只追加，不清空，不创建新标签）。 */
    ADD_RECORD_TAGS("add_record_tags", true),

    /** 为草稿设置解锁时间（可逆草稿字段，**不触发封存**）。 */
    PROPOSE_UNLOCK_AT("propose_unlock_at", true),

    /** 可选标签清单（后端预注入，不下发为 FC tool）。 */
    LIST_AVAILABLE_TAGS("list_available_tags", false),

    /** 当前草稿快照（后端预注入，不下发为 FC tool）。 */
    READ_DRAFT_SNAPSHOT("read_draft_snapshot", false);

    private final String wireName;
    private final boolean writeTool;

    AgentToolName(String wireName, boolean writeTool) {
        this.wireName = wireName;
        this.writeTool = writeTool;
    }

    /**
     * 下发给 provider 与落库审计使用的工具名。
     */
    public String wireName() {
        return wireName;
    }

    /**
     * 是否为写工具。只有写工具会被下发为 function calling tool。
     */
    public boolean isWriteTool() {
        return writeTool;
    }

    /**
     * 按 wire name 查找；未命中返回 null（调用方须按白名单外提议处理，不得猜测）。
     */
    public static AgentToolName fromWireName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        for (AgentToolName tool : values()) {
            if (tool.wireName.equals(normalized)) {
                return tool;
            }
        }
        return null;
    }
}
