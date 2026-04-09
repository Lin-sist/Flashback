package com.flashback.mapper;

import com.flashback.domain.Reply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReplyMapper {

    int insert(Reply reply);

    Reply selectByRecordId(@Param("recordId") Long recordId);
}
