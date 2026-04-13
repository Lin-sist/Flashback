package com.flashback.service;

import com.flashback.domain.TagType;
import com.flashback.vo.TagVO;

import java.util.List;

/**
 * 标签查询服务。
 */
public interface TagService {

    List<TagVO> listEnabled(TagType type);
}
