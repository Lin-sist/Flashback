package com.flashback.controller.api;

import com.flashback.domain.User;
import com.flashback.domain.TagStatus;
import com.flashback.domain.TagType;
import com.flashback.domain.UserStatus;
import com.flashback.mapper.UserMapper;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.TagService;
import com.flashback.vo.TagVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TagControllerAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private TagService tagService;

    @Test
    void shouldReturn401WhenAccessTagApiWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldReturnEnabledTagsWhenAuthorized() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser(UserStatus.ENABLED));

        TagVO tag = new TagVO();
        tag.setId(1L);
        tag.setName("焦虑");
        tag.setType(TagType.MOOD);
        tag.setStatus(TagStatus.ENABLED);

        when(tagService.listEnabled(TagType.MOOD)).thenReturn(List.of(tag));

        mockMvc.perform(get("/api/tags")
                .header("Authorization", "Bearer " + token)
                .param("type", "MOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("焦虑"))
                .andExpect(jsonPath("$.data[0].type").value("MOOD"))
                .andExpect(jsonPath("$.data[0].status").value("ENABLED"));
    }

    @Test
    void shouldReturn400WhenTagTypeIsInvalid() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser(UserStatus.ENABLED));

        mockMvc.perform(get("/api/tags")
                .header("Authorization", "Bearer " + token)
                .param("type", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("type: 参数格式不正确"));
    }

    @Test
    void shouldReturn403WhenUserDisabled() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser(UserStatus.DISABLED));

        mockMvc.perform(get("/api/tags")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.message").value("用户已禁用"));
    }

    private User enabledUser(UserStatus status) {
        User user = new User();
        user.setId(5001L);
        user.setStatus(status);
        return user;
    }
}
