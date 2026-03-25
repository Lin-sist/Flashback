package com.flashback.controller.api;

import com.flashback.dto.UpdateUserProfileRequest;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.UserService;
import com.flashback.vo.UserInfoVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserService userService;

    @Test
    void shouldReturn401WhenAccessMeWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldReturnCurrentUserWhenAccessMeWithToken() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(4001L, AuthRole.USER));
        when(userService.getCurrentUser(4001L)).thenReturn(mockUserInfo("Neo"));

        mockMvc.perform(get("/api/user/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(4001))
                .andExpect(jsonPath("$.data.nickname").value("Neo"));
    }

    @Test
    void shouldReturnUpdatedUserAfterUpdateProfile() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(4001L, AuthRole.USER));
        when(userService.updateProfile(eq(4001L), any(UpdateUserProfileRequest.class)))
                .thenReturn(mockUserInfo("Neo-Updated"));

        mockMvc.perform(put("/api/user/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"nickname\":\"Neo-Updated\",\"email\":\"neo@example.com\",\"avatar\":\"https://img.test/neo.png\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("Neo-Updated"))
                .andExpect(jsonPath("$.data.email").value("neo@example.com"));
    }

    private UserInfoVO mockUserInfo(String nickname) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(4001L);
        vo.setUsername("neo");
        vo.setNickname(nickname);
        vo.setEmail("neo@example.com");
        vo.setAvatar("https://img.test/neo.png");
        vo.setCreatedAt(LocalDateTime.of(2026, 3, 25, 10, 0, 0));
        vo.setUpdatedAt(LocalDateTime.of(2026, 3, 25, 10, 5, 0));
        return vo;
    }
}
