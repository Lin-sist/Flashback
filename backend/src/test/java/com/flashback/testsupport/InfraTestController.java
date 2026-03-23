package com.flashback.testsupport;

import com.flashback.common.page.PageQuery;
import com.flashback.common.page.PageResult;
import com.flashback.common.response.ApiResponse;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Profile("test")
@Validated
@RestController
public class InfraTestController {

    @GetMapping("/api/test/protected")
    public ApiResponse<String> apiProtected() {
        return ApiResponse.success("ok");
    }

    @GetMapping("/admin/test/protected")
    public ApiResponse<String> adminProtected() {
        return ApiResponse.success("ok");
    }

    @PostMapping("/api/test/validation")
    public ApiResponse<String> validate(@Valid @RequestBody ValidateRequest request) {
        return ApiResponse.success("ok");
    }

    @GetMapping("/api/test/current-user")
    public ApiResponse<Map<String, Object>> currentUser(@CurrentUser AuthUser authUser) {
        return ApiResponse.success(Map.of(
                "userId", authUser.getUserId(),
                "role", authUser.getRole().name()));
    }

    @GetMapping("/api/test/page")
    public ApiResponse<PageResult<String>> page(@Valid PageQuery pageQuery) {
        return ApiResponse.success(PageResult.of(
                List.of(),
                0L,
                pageQuery.getPageNum(),
                pageQuery.getPageSize()));
    }

    public record ValidateRequest(@NotBlank(message = "name不能为空") String name) {
    }
}
