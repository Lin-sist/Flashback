package com.flashback.service.impl;

import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.config.AppWechatProperties;
import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.dto.LoginRequest;
import com.flashback.dto.RegisterRequest;
import com.flashback.dto.UpdateUserProfileRequest;
import com.flashback.dto.WechatLoginRequest;
import com.flashback.mapper.UserMapper;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.UserService;
import com.flashback.vo.LoginResponseVO;
import com.flashback.vo.RegisterResponseVO;
import com.flashback.vo.UserInfoVO;
import com.flashback.wechat.WechatSession;
import com.flashback.wechat.WechatSessionClient;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 用户模块核心业务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final int OPENID_MAX_LENGTH = 100;

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppWechatProperties appWechatProperties;
    private final WechatSessionClient wechatSessionClient;
    private final Clock clock;

    public UserServiceImpl(
            UserMapper userMapper,
            JwtTokenProvider jwtTokenProvider,
            AppWechatProperties appWechatProperties,
            WechatSessionClient wechatSessionClient,
            Clock clock) {
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appWechatProperties = appWechatProperties;
        this.wechatSessionClient = wechatSessionClient;
        this.clock = clock;
    }

    @Override
    @Transactional
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
        // 普通注册链路不信任客户端直接传入 openid，避免为后续小程序登录埋下身份污染风险。
        user.setOpenid(null);
        user.setStatus(UserStatus.ENABLED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw badRequest("用户名已存在");
        }
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

        return buildLoginResponse(user);
    }

    @Override
    @Transactional
    public LoginResponseVO wechatLogin(WechatLoginRequest request) {
        String code = normalizeRequired(request.getCode(), "code不能为空");
        if (!appWechatProperties.isConfigured()) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, HttpStatus.SERVICE_UNAVAILABLE, "微信登录未配置");
        }

        WechatSession session = wechatSessionClient.exchangeCodeForSession(code);
        String openid = normalizeRequired(session.getOpenid(), "微信登录未返回有效身份");
        if (openid.length() > OPENID_MAX_LENGTH) {
            throw badRequest("openid长度不能超过100");
        }

        User user = userMapper.selectByOpenid(openid);
        if (user == null) {
            user = createWechatUser(openid);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BizException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "用户已禁用");
        }

        return buildLoginResponse(user);
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

    @Override
    @Transactional
    public UserInfoVO bindVerifiedWechatOpenid(Long userId, String openid) {
        requireUserById(userId);

        String normalizedOpenid = normalizeRequired(openid, "openid不能为空");
        if (normalizedOpenid.length() > OPENID_MAX_LENGTH) {
            throw badRequest("openid长度不能超过100");
        }

        User existing = userMapper.selectByOpenid(normalizedOpenid);
        if (existing != null && !existing.getId().equals(userId)) {
            throw badRequest("openid已绑定其他用户");
        }

        try {
            userMapper.updateOpenidById(userId, normalizedOpenid, LocalDateTime.now(clock));
        } catch (DuplicateKeyException ex) {
            throw badRequest("openid已绑定其他用户");
        }
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

    private User createWechatUser(String openid) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = new User();
        user.setUsername(buildWechatUsername(openid));
        user.setPasswordHash(hashPassword(UUID.randomUUID().toString()));
        user.setNickname("微信用户");
        user.setOpenid(openid);
        user.setStatus(UserStatus.ENABLED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            User existing = userMapper.selectByOpenid(openid);
            if (existing != null) {
                return existing;
            }
            throw badRequest("微信用户创建失败");
        }
        return user;
    }

    private String buildWechatUsername(String openid) {
        return "wx_" + sha256Hex(openid).substring(0, 32);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private LoginResponseVO buildLoginResponse(User user) {
        // Token 仅通过既有 JwtTokenProvider 签发，claims 固定 userId + role。
        String token = jwtTokenProvider.createToken(new AuthUser(user.getId(), AuthRole.USER));

        LoginResponseVO response = new LoginResponseVO();
        response.setToken(token);
        response.setTokenType(TOKEN_TYPE_BEARER);
        response.setUserInfo(toUserInfo(user));
        return response;
    }

    private boolean verifyPassword(String plainPassword, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, passwordHash);
        } catch (IllegalArgumentException ex) {
            // 兼容历史脏数据或错误初始化数据，避免将口令格式问题暴露为 500。
            return false;
        }
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
        vo.setWechatBound(normalizeOptional(user.getOpenid()) != null);
        return vo;
    }
}
