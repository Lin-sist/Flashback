package com.flashback.service;

import com.flashback.dto.CreateReplyRequest;
import com.flashback.vo.ReplyVO;

/**
 * 回信模块业务服务。
 */
public interface ReplyService {

    ReplyVO create(Long userId, Long recordId, CreateReplyRequest request);

    ReplyVO detail(Long userId, Long recordId);
}
