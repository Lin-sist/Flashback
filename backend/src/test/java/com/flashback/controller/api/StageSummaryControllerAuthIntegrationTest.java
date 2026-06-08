package com.flashback.controller.api;

import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.mapper.UserMapper;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.StageSummaryService;
import com.flashback.vo.StageSummaryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StageSummaryControllerAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private StageSummaryService stageSummaryService;

    @Test
    void shouldReturn401WhenGenerateWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/stage-summaries/generate"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldGenerateStageSummaryWhenAuthorized() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(100L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
        when(stageSummaryService.generate(100L)).thenReturn(mockSummary());

        mockMvc.perform(post("/api/stage-summaries/generate")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.summary").value("这一阶段你留下了 3 条记录"))
                .andExpect(jsonPath("$.data.recordCount").value(3))
                .andExpect(jsonPath("$.data.source").value("fallback"));
    }

    private User enabledUser() {
        User user = new User();
        user.setId(100L);
        user.setStatus(UserStatus.ENABLED);
        return user;
    }

    private StageSummaryVO mockSummary() {
        StageSummaryVO vo = new StageSummaryVO();
        vo.setSummary("这一阶段你留下了 3 条记录");
        vo.setSource("fallback");
        vo.setRecordCount(3);
        vo.setUnlockedCount(1);
        vo.setLifeNodeCount(2);
        vo.setGeneratedAt(LocalDateTime.of(2026, 6, 7, 12, 0, 0));
        return vo;
    }
}
