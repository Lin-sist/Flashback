package com.flashback.service.impl;

import com.flashback.domain.Tag;
import com.flashback.domain.TagType;
import com.flashback.mapper.TagMapper;
import com.flashback.service.TagService;
import com.flashback.vo.TagVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签查询服务实现。
 */
@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    public TagServiceImpl(TagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    @Override
    public List<TagVO> listEnabled(TagType type) {
        List<Tag> tags = tagMapper.selectEnabledByType(type);
        return tags.stream().map(this::toTagVO).toList();
    }

    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setType(tag.getType());
        vo.setStatus(tag.getStatus());
        return vo;
    }
}
