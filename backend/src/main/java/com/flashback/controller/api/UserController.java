package com.flashback.controller.api;

import com.flashback.common.response.ApiResponse;
import com.flashback.dto.UpdateUserProfileRequest;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.UserService;
import com.flashback.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 普通用户资料接口。
 */
@Validated
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoVO> me(@CurrentUser AuthUser authUser) {
        return ApiResponse.success(userService.getCurrentUser(authUser.getUserId()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserInfoVO> updateProfile(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(authUser.getUserId(), request));
    }
}
