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
    private String stage;
    private String sessionStatus;
    private int turnCount;
    private int maxTurns;
    private boolean canContinue;
    private List<AgentMessageVO> messages;
    private String materialDraft;
    private String source;
    private String status;
    private String message;

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
}
