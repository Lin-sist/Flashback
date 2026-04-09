package com.flashback.controller.api;

import com.flashback.common.exception.NotFoundException;
import com.flashback.domain.ReplyType;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.ReplyService;
import com.flashback.vo.ReplyVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReplyControllerAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ReplyService replyService;

    @Test
    void shouldReturn401WhenAccessReplyApiWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/records/9001/reply"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldCreateReplyWhenAuthorized() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(replyService.create(eq(5001L), eq(9001L), any()))
                .thenReturn(mockReplyVO());

        mockMvc.perform(post("/api/records/9001/reply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          \"content\": \"你做得不错，继续保持\",
                          \"replyType\": \"SHORT_REPLY\"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(3001))
                .andExpect(jsonPath("$.data.recordId").value(9001))
                .andExpect(jsonPath("$.data.replyType").value("SHORT_REPLY"));
    }

    @Test
    void shouldReturn400WhenContentBlank() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));

        mockMvc.perform(post("/api/records/9001/reply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          \"content\": "   ",
                          \"replyType\": \"SHORT_REPLY\"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void shouldReturn400WhenContentTooLong() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        String tooLong = "a".repeat(501);
        String body = "{\"content\":\"" + tooLong + "\",\"replyType\":\"SHORT_REPLY\"}";

        mockMvc.perform(post("/api/records/9001/reply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void shouldReturn400WhenReplyTypeInvalid() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));

        mockMvc.perform(post("/api/records/9001/reply")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          \"content\": \"ok\",
                          \"replyType\": \"LONG_REPLY\"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void shouldReturnNullWhenNoReply() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(replyService.detail(5001L, 9002L)).thenReturn(null);

        mockMvc.perform(get("/api/records/9002/reply")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void shouldReturn404WhenRecordNotFound() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(replyService.detail(5001L, 9999L)).thenThrow(new NotFoundException("记录不存在"));

        mockMvc.perform(get("/api/records/9999/reply")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    private ReplyVO mockReplyVO() {
        ReplyVO vo = new ReplyVO();
        vo.setId(3001L);
        vo.setRecordId(9001L);
        vo.setContent("你做得不错，继续保持");
        vo.setReplyType(ReplyType.SHORT_REPLY);
        vo.setCreatedAt(LocalDateTime.of(2026, 4, 9, 20, 0, 0));
        return vo;
    }
}
