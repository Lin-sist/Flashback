package com.flashback.vo;

import com.flashback.domain.TagStatus;
import com.flashback.domain.TagType;

/**
 * 标签视图。
 */
public class TagVO {

    private Long id;
    private String name;
    private TagType type;
    private TagStatus status;

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

    public TagStatus getStatus() {
        return status;
    }

    public void setStatus(TagStatus status) {
        this.status = status;
    }
}
