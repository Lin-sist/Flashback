package com.flashback.controller.api;

import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.AiService;
import com.flashback.vo.AiSummaryVO;
import com.flashback.vo.AiWritingPromptsVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiControllerAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AiService aiService;

    @Test
    void shouldReturn401WhenAccessAiApiWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/ai/writing-prompts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldReturnWritingPromptsWhenAuthorized() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(aiService.generateWritingPrompts(eq(5001L), any())).thenReturn(mockPrompts());

        mockMvc.perform(post("/api/ai/writing-prompts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          \"content\": \"最近求职很焦虑\",
                          \"recordType\": \"NODE_RECORD\",
                          \"coreQuestion\": \"我该先优化简历还是补项目\"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.source").value("mock"))
                .andExpect(jsonPath("$.data.prompts[0]").value("你现在最担心什么？"));
    }

    @Test
    void shouldReturn400WhenSummarizeContentBlank() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));

        mockMvc.perform(post("/api/ai/summarize-record")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          \"content\": \"   \",
                          \"coreQuestion\": \"下一步怎么走\"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void shouldReturnSummaryWhenAuthorized() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(aiService.summarizeRecord(eq(5001L), any())).thenReturn(mockSummary());

        mockMvc.perform(post("/api/ai/summarize-record")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          \"content\": \"最近有些迷茫，想尽快梳理方向\",
                          \"coreQuestion\": \"我应该先做什么\"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.source").value("mock"))
                .andExpect(jsonPath("$.data.summary").value("当前主要困惑集中在职业方向选择"))
                .andExpect(jsonPath("$.data.confusion").value("职业方向不清晰"));
    }

    private AiWritingPromptsVO mockPrompts() {
        AiWritingPromptsVO vo = new AiWritingPromptsVO();
        vo.setSource("mock");
        vo.setPrompts(List.of("你现在最担心什么？", "你最希望未来告诉你什么？", "今天你想先做哪一步？"));
        return vo;
    }

    private AiSummaryVO mockSummary() {
        AiSummaryVO vo = new AiSummaryVO();
        vo.setSummary("当前主要困惑集中在职业方向选择");
        vo.setConfusion("职业方向不清晰");
        vo.setEmotion("偏迷茫");
        vo.setCoreQuestion("我应该先做什么");
        vo.setDesiredOutcome("形成一周可执行计划");
        vo.setSource("mock");
        return vo;
    }
}
