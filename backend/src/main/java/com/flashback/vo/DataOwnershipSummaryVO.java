package com.flashback.vo;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataOwnershipSummaryVO {
    private Map<String, Long> recordCounts = new LinkedHashMap<>();
    private long mediaBytes;
    private DataOperationVO activeOperation;
    public Map<String, Long> getRecordCounts() { return recordCounts; }
    public void setRecordCounts(Map<String, Long> recordCounts) { this.recordCounts = recordCounts; }
    public long getMediaBytes() { return mediaBytes; }
    public void setMediaBytes(long mediaBytes) { this.mediaBytes = mediaBytes; }
    public DataOperationVO getActiveOperation() { return activeOperation; }
    public void setActiveOperation(DataOperationVO activeOperation) { this.activeOperation = activeOperation; }
}
