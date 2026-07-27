package com.flashback.controller.api;

import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentSessionStatus;
import com.flashback.domain.AgentStage;
import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.mapper.UserMapper;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.AgentChatService;
import com.flashback.vo.AgentMessageVO;
import com.flashback.vo.AgentSessionVO;
import com.flashback.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentControllerAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private AgentChatService agentChatService;

    @Test
    void shouldReturn401WhenStartSessionWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/agent/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldReturn401WhenSendMessageWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/agent/sessions/900/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"最近老是睡不好\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldStartSessionWhenAuthorized() throws Exception {
        String token = authorizedToken();
        when(agentChatService.startOrResume(eq(5001L), any())).thenReturn(openedSession());

        mockMvc.perform(post("/api/agent/sessions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionId").value(900))
                .andExpect(jsonPath("$.data.stage").value("EMOTION"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.messages[0].role").value("ASSISTANT"));
    }

    @Test
    void shouldSendMessageWhenAuthorized() throws Exception {
        String token = authorizedToken();
        when(agentChatService.sendMessage(eq(5001L), eq(900L), any())).thenReturn(openedSession());

        mockMvc.perform(post("/api/agent/sessions/900/messages")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"最近老是睡不好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sessionId").value(900));
    }

    @Test
    void shouldRejectBlankMessageContent() throws Exception {
        String token = authorizedToken();

        mockMvc.perform(post("/api/agent/sessions/900/messages")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void shouldReturnSafeNotFoundForOtherUsersSession() throws Exception {
        String token = authorizedToken();
        when(agentChatService.getSession(eq(5001L), eq(900L)))
                .thenThrow(new NotFoundException("会话不存在"));

        mockMvc.perform(get("/api/agent/sessions/900")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.message").value("会话不存在"));
    }

    @Test
    void shouldFinishSessionWhenAuthorized() throws Exception {
        String token = authorizedToken();
        AgentSessionVO finished = openedSession();
        finished.setSessionStatus(AgentSessionStatus.ENDED.name());
        finished.setStage(AgentStage.ENDED.name());
        finished.setMaterialDraft("工作上有点撑不住");
        finished.setCanContinue(false);
        when(agentChatService.finish(eq(5001L), eq(900L))).thenReturn(finished);

        mockMvc.perform(post("/api/agent/sessions/900/finish")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("ENDED"))
                .andExpect(jsonPath("$.data.materialDraft").value("工作上有点撑不住"))
                .andExpect(jsonPath("$.data.canContinue").value(false));
    }

    private String authorizedToken() {
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
        return jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
    }

    private AgentSessionVO openedSession() {
        AgentMessageVO message = new AgentMessageVO();
        message.setId(1L);
        message.setRole(AgentMessageRole.ASSISTANT.name());
        message.setTurnNo(0);
        message.setStage(AgentStage.EMOTION.name());
        message.setContent("今天是什么让你想写下这一刻？");
        message.setCreatedAt(LocalDateTime.of(2026, 7, 27, 10, 0));

        AgentSessionVO vo = new AgentSessionVO();
        vo.setSessionId(900L);
        vo.setStage(AgentStage.EMOTION.name());
        vo.setSessionStatus(AgentSessionStatus.ACTIVE.name());
        vo.setTurnCount(0);
        vo.setMaxTurns(8);
        vo.setCanContinue(true);
        vo.setMessages(List.of(message));
        vo.setSource("mock");
        vo.setStatus("SUCCESS");
        return vo;
    }

    private User enabledUser() {
        User user = new User();
        user.setId(5001L);
        user.setUsername("agent_user");
        user.setStatus(UserStatus.ENABLED);
        return user;
    }
}
