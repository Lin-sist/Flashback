package com.flashback.mapper;

import com.flashback.domain.UnlockNoticeLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UnlockNoticeLogMapper {

    int insert(UnlockNoticeLog unlockNoticeLog);
}
