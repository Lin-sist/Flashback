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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
