package com.flashback.vo;

import java.util.List;

/**
 * Agent 会话视图。
 *
 * status 沿用既有 AI 语义 SUCCESS / UNAVAILABLE / FAILED；
 * C1 不引入 FALLBACK（design.md 决策 5）。
 */
public class AgentSessionVO {

    private Long sessionId;
    private Long recordId;
    private String conversationIntent;
    private String stage;
    private String sessionStatus;
    private int turnCount;
    private int maxTurns;
    private boolean canContinue;
    /** P4.2：当前会话跨记录记忆授权；缺省与历史值均为 false。 */
    private boolean crossRecordMemoryEnabled;
    private List<AgentMessageVO> messages;
    private String materialDraft;
    private String source;
    private String status;
    private String message;
    /**
     * C2 新增（向后兼容）：当前待用户确认的工具提议；无提议时为 null。
     * C1 既有字段语义不变。
     */
    private AgentToolCallVO pendingToolCall;
    /**
     * C2 新增（向后兼容）：最近一次工具执行结果，用于前端展示成功或失败原因。
     */
    private AgentToolCallVO lastToolCallResult;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getConversationIntent() {
        return conversationIntent;
    }

    public void setConversationIntent(String conversationIntent) {
        this.conversationIntent = conversationIntent;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    public boolean isCanContinue() {
        return canContinue;
    }

    public void setCanContinue(boolean canContinue) {
        this.canContinue = canContinue;
    }

    public boolean isCrossRecordMemoryEnabled() {
        return crossRecordMemoryEnabled;
    }

    public void setCrossRecordMemoryEnabled(boolean crossRecordMemoryEnabled) {
        this.crossRecordMemoryEnabled = crossRecordMemoryEnabled;
    }

    public List<AgentMessageVO> getMessages() {
        return messages;
    }

    public void setMessages(List<AgentMessageVO> messages) {
        this.messages = messages;
    }

    public String getMaterialDraft() {
        return materialDraft;
    }

    public void setMaterialDraft(String materialDraft) {
        this.materialDraft = materialDraft;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AgentToolCallVO getPendingToolCall() {
        return pendingToolCall;
    }

    public void setPendingToolCall(AgentToolCallVO pendingToolCall) {
        this.pendingToolCall = pendingToolCall;
    }

    public AgentToolCallVO getLastToolCallResult() {
        return lastToolCallResult;
    }

    public void setLastToolCallResult(AgentToolCallVO lastToolCallResult) {
        this.lastToolCallResult = lastToolCallResult;
    }
}
