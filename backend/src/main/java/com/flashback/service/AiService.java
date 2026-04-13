package com.flashback.service;

import com.flashback.dto.AiSummarizeRecordRequest;
import com.flashback.dto.AiWritingPromptsRequest;
import com.flashback.vo.AiSummaryVO;
import com.flashback.vo.AiWritingPromptsVO;

/**
 * AI 辅助能力服务。
 */
public interface AiService {

    AiWritingPromptsVO generateWritingPrompts(Long userId, AiWritingPromptsRequest request);

    AiSummaryVO summarizeRecord(Long userId, AiSummarizeRecordRequest request);
}
