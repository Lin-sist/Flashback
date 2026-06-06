package com.flashback.service.impl;

import com.flashback.common.exception.BizException;
import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.dto.LoginRequest;
import com.flashback.dto.RegisterRequest;
import com.flashback.mapper.UserMapper;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.vo.LoginResponseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-25T08:00:00Z"), ZoneId.of("Asia/Shanghai"));
        userService = new UserServiceImpl(userMapper, jwtTokenProvider, clock);
    }

    @Test
    void shouldFailWhenRegisterWithDuplicatedUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setNickname("Alice");

        User existed = new User();
        existed.setId(1L);
        existed.setUsername("alice");
        when(userMapper.selectByUsername("alice")).thenReturn(existed);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名已存在");
    }

    @Test
    void shouldMapDuplicateKeyWhenConcurrentRegister() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setNickname("Alice");

        when(userMapper.selectByUsername("alice")).thenReturn(null);
        doThrow(new DuplicateKeyException("duplicate")).when(userMapper).insert(any(User.class));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名已存在");
    }

    @Test
    void shouldIgnoreOpenidWhenRegisterNormally() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret123");
        request.setNickname("Alice");
        request.setOpenid("wx-openid-from-client");

        when(userMapper.selectByUsername("alice")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getOpenid()).isNull();
    }

    @Test
    void shouldReturnTokenWhenLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setUsername("bob");
        request.setPassword("secret123");

        User user = new User();
        user.setId(100L);
        user.setUsername("bob");
        user.setPasswordHash(BCrypt.hashpw("secret123", BCrypt.gensalt()));
        user.setNickname("Bob");
        user.setStatus(UserStatus.ENABLED);
        when(userMapper.selectByUsername("bob")).thenReturn(user);
        when(jwtTokenProvider.createToken(any())).thenReturn("jwt-token");

        LoginResponseVO response = userService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserInfo().getUsername()).isEqualTo("bob");
    }

    @Test
    void shouldBindVerifiedWechatOpenid() {
        User current = enabledUser(7L, "bind_user", null);
        User bound = enabledUser(7L, "bind_user", "wx-openid-7");
        when(userMapper.selectById(7L)).thenReturn(current, bound);
        when(userMapper.selectByOpenid("wx-openid-7")).thenReturn(null);

        var response = userService.bindVerifiedWechatOpenid(7L, " wx-openid-7 ");

        assertThat(response.isWechatBound()).isTrue();
        verify(userMapper).updateOpenidById(eq(7L), eq("wx-openid-7"), any());
    }

    @Test
    void shouldRejectBlankOpenidWhenBindingWechatIdentity() {
        when(userMapper.selectById(7L)).thenReturn(enabledUser(7L, "bind_user", null));

        assertThatThrownBy(() -> userService.bindVerifiedWechatOpenid(7L, " "))
                .isInstanceOf(BizException.class)
                .hasMessage("openid不能为空");

        verify(userMapper, never()).updateOpenidById(any(), any(), any());
    }

    @Test
    void shouldRejectOpenidAlreadyBoundToAnotherUser() {
        when(userMapper.selectById(7L)).thenReturn(enabledUser(7L, "bind_user", null));
        when(userMapper.selectByOpenid("wx-openid-shared"))
                .thenReturn(enabledUser(8L, "other_user", "wx-openid-shared"));

        assertThatThrownBy(() -> userService.bindVerifiedWechatOpenid(7L, "wx-openid-shared"))
                .isInstanceOf(BizException.class)
                .hasMessage("openid已绑定其他用户");

        verify(userMapper, never()).updateOpenidById(any(), any(), any());
    }

    @Test
    void shouldMapDuplicateOpenidRaceWhenBindingWechatIdentity() {
        when(userMapper.selectById(7L)).thenReturn(enabledUser(7L, "bind_user", null));
        when(userMapper.selectByOpenid("wx-openid-race")).thenReturn(null);
        doThrow(new DuplicateKeyException("duplicate"))
                .when(userMapper).updateOpenidById(any(), any(), any());

        assertThatThrownBy(() -> userService.bindVerifiedWechatOpenid(7L, "wx-openid-race"))
                .isInstanceOf(BizException.class)
                .hasMessage("openid已绑定其他用户");
    }

    @Test
    void shouldFailWhenPasswordIncorrect() {
        LoginRequest request = new LoginRequest();
        request.setUsername("carol");
        request.setPassword("wrong-pass");

        User user = new User();
        user.setId(101L);
        user.setUsername("carol");
        user.setPasswordHash(BCrypt.hashpw("secret123", BCrypt.gensalt()));
        user.setNickname("Carol");
        user.setStatus(UserStatus.ENABLED);
        when(userMapper.selectByUsername("carol")).thenReturn(user);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void shouldFailWhenStoredPasswordHashMalformed() {
        LoginRequest request = new LoginRequest();
        request.setUsername("dave");
        request.setPassword("secret123");

        User user = new User();
        user.setId(102L);
        user.setUsername("dave");
        user.setPasswordHash("plain-text-password");
        user.setNickname("Dave");
        user.setStatus(UserStatus.ENABLED);
        when(userMapper.selectByUsername("dave")).thenReturn(user);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BizException.class)
                .hasMessage("用户名或密码错误");
    }

    private User enabledUser(Long id, String username, String openid) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(BCrypt.hashpw("secret123", BCrypt.gensalt()));
        user.setNickname(username);
        user.setOpenid(openid);
        user.setStatus(UserStatus.ENABLED);
        return user;
    }
}
