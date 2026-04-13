package com.flashback.domain;

/**
 * 记录-标签名称投影，用于批量组装列表标签名。
 */
public class RecordTagName {

    private Long recordId;
    private String tagName;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
