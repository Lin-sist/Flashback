package com.flashback.vo;

/**
 * AI 内容整理结果。
 */
public class AiSummaryVO {

    private String summary;
    private String confusion;
    private String emotion;
    private String coreQuestion;
    private String desiredOutcome;
    private String beliefThen;
    private String source;
    private String status;
    private String message;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getConfusion() {
        return confusion;
    }

    public void setConfusion(String confusion) {
        this.confusion = confusion;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public String getCoreQuestion() {
        return coreQuestion;
    }

    public void setCoreQuestion(String coreQuestion) {
        this.coreQuestion = coreQuestion;
    }

    public String getDesiredOutcome() {
        return desiredOutcome;
    }

    public void setDesiredOutcome(String desiredOutcome) {
        this.desiredOutcome = desiredOutcome;
    }

    public String getBeliefThen() {
        return beliefThen;
    }

    public void setBeliefThen(String beliefThen) {
        this.beliefThen = beliefThen;
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
