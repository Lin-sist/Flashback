package com.flashback.vo;

import com.flashback.domain.TagType;

/**
 * 记录详情中的标签信息。
 */
public class RecordTagVO {

    private Long id;
    private String name;
    private TagType type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TagType getType() {
        return type;
    }

    public void setType(TagType type) {
        this.type = type;
    }
}
