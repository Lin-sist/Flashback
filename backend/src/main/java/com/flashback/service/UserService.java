package com.flashback.service;

import com.flashback.dto.LoginRequest;
import com.flashback.dto.RegisterRequest;
import com.flashback.dto.UpdateUserProfileRequest;
import com.flashback.vo.LoginResponseVO;
import com.flashback.vo.RegisterResponseVO;
import com.flashback.vo.UserInfoVO;

/**
 * 用户模块业务服务。
 */
public interface UserService {

    RegisterResponseVO register(RegisterRequest request);

    LoginResponseVO login(LoginRequest request);

    UserInfoVO getCurrentUser(Long userId);

    UserInfoVO updateProfile(Long userId, UpdateUserProfileRequest request);
}
