package com.flashback.mapper;

import com.flashback.domain.RecordTagName;
import com.flashback.domain.Tag;
import com.flashback.domain.TagType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TagMapper {

    List<Tag> selectEnabledByType(@Param("type") TagType type);

    long countEnabledByIds(@Param("tagIds") List<Long> tagIds);

    List<Tag> selectTagsByRecordId(@Param("recordId") Long recordId);

    List<RecordTagName> selectTagNamesByRecordIds(@Param("recordIds") List<Long> recordIds);
}
