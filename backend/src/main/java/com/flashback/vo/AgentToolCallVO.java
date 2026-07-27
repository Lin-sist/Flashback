package com.flashback.vo;

/**
 * 工具提议 / 执行结果视图（C2）。
 *
 * 注意：不暴露 argsDigest 与 pendingArgs——审计字段属于后端内部，
 * 前端只需要知道「Agent 想做什么」以及「做成了没有」。
 * 也不暴露参数原文：正文类提议的实际内容在执行后由记录详情反映。
 */
public class AgentToolCallVO {

    private Long toolCallId;
    private String tool;
    private String status;
    /** Agent 的征询话术，用于确认条文案。 */
    private String askText;
    /** 面向用户的结果或失败原因。 */
    private String resultSummary;
    private String failureType;
    /** 该提议涉及的标签 id，供前端在成功后局部刷新表单。 */
    private java.util.List<Long> tagIds;
    /** 该提议建议的解锁时间。 */
    private String unlockAt;

    public Long getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(Long toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAskText() {
        return askText;
    }

    public void setAskText(String askText) {
        this.askText = askText;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getFailureType() {
        return failureType;
    }

    public void setFailureType(String failureType) {
        this.failureType = failureType;
    }

    public java.util.List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(java.util.List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public String getUnlockAt() {
        return unlockAt;
    }

    public void setUnlockAt(String unlockAt) {
        this.unlockAt = unlockAt;
    }
}
