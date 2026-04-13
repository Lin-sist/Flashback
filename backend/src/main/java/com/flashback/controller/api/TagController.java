package com.flashback.controller.api;

import com.flashback.common.response.ApiResponse;
import com.flashback.domain.TagType;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.TagService;
import com.flashback.vo.TagVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签查询接口。
 */
@Validated
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<TagVO>> list(
            @CurrentUser AuthUser authUser,
            @RequestParam(value = "type", required = false) TagType type) {
        return ApiResponse.success(tagService.listEnabled(type));
    }
}
