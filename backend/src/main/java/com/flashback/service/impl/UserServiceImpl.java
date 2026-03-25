package com.flashback.service.impl;

import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.dto.LoginRequest;
import com.flashback.dto.RegisterRequest;
import com.flashback.dto.UpdateUserProfileRequest;
import com.flashback.mapper.UserMapper;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.UserService;
import com.flashback.vo.LoginResponseVO;
import com.flashback.vo.RegisterResponseVO;
import com.flashback.vo.UserInfoVO;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 用户模块核心业务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock;

    public UserServiceImpl(UserMapper userMapper, JwtTokenProvider jwtTokenProvider, Clock clock) {
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.clock = clock;
    }

    @Override
    public RegisterResponseVO register(RegisterRequest request) {
        String username = normalizeRequired(request.getUsername(), "username不能为空");
        String password = normalizeRequired(request.getPassword(), "password不能为空");
        String nickname = normalizeRequired(request.getNickname(), "nickname不能为空");

        User existed = userMapper.selectByUsername(username);
        if (existed != null) {
            throw badRequest("用户名已存在");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashPassword(password));
        user.setNickname(nickname);
        user.setEmail(normalizeOptional(request.getEmail()));
        user.setAvatar(normalizeOptional(request.getAvatar()));
        user.setOpenid(normalizeOptional(request.getOpenid()));
        user.setStatus(UserStatus.ENABLED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userMapper.insert(user);
        return new RegisterResponseVO(user.getId());
    }

    @Override
    public LoginResponseVO login(LoginRequest request) {
        String username = normalizeRequired(request.getUsername(), "username不能为空");
        String password = normalizeRequired(request.getPassword(), "password不能为空");

        User user = userMapper.selectByUsername(username);
        // 登录校验统一在 service 层集中，避免密码逻辑散落。
        if (user == null || !verifyPassword(password, user.getPasswordHash())) {
            throw badRequest("用户名或密码错误");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BizException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "用户已禁用");
        }

        // Token 仅通过既有 JwtTokenProvider 签发，claims 固定 userId + role。
        String token = jwtTokenProvider.createToken(new AuthUser(user.getId(), AuthRole.USER));

        LoginResponseVO response = new LoginResponseVO();
        response.setToken(token);
        response.setTokenType(TOKEN_TYPE_BEARER);
        response.setUserInfo(toUserInfo(user));
        return response;
    }

    @Override
    public UserInfoVO getCurrentUser(Long userId) {
        User user = requireUserById(userId);
        return toUserInfo(user);
    }

    @Override
    public UserInfoVO updateProfile(Long userId, UpdateUserProfileRequest request) {
        User current = requireUserById(userId);

        String nickname = current.getNickname();
        if (request.getNickname() != null) {
            nickname = normalizeRequired(request.getNickname(), "nickname不能为空");
        }

        String email = request.getEmail() == null ? current.getEmail() : normalizeOptional(request.getEmail());
        String avatar = request.getAvatar() == null ? current.getAvatar() : normalizeOptional(request.getAvatar());

        userMapper.updateProfileById(userId, nickname, email, avatar, LocalDateTime.now(clock));
        return toUserInfo(requireUserById(userId));
    }

    private User requireUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "登录信息不存在或已失效");
        }
        return user;
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    private boolean verifyPassword(String plainPassword, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, passwordHash);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw badRequest(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private BizException badRequest(String message) {
        return new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, message);
    }

    private UserInfoVO toUserInfo(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
