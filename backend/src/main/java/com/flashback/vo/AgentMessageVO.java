package com.flashback.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单条对话消息。
 */
public class AgentMessageVO {

    private Long id;
    private String role;
    private int turnNo;
    private String stage;
    private String content;
    private LocalDateTime createdAt;
    /** P4.2：仅 assistant message 可能非空；用户消息与旧消息为空数组。 */
    private List<AgentMemorySourceVO> memorySources = List.of();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getTurnNo() {
        return turnNo;
    }

    public void setTurnNo(int turnNo) {
        this.turnNo = turnNo;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<AgentMemorySourceVO> getMemorySources() {
        return memorySources;
    }

    public void setMemorySources(List<AgentMemorySourceVO> memorySources) {
        this.memorySources = memorySources;
    }
}
