package com.flashback.controller.api;

import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.mapper.UserMapper;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.RecordAttachmentService;
import com.flashback.vo.AttachmentUploadTokenVO;
import com.flashback.vo.RecordAttachmentVO;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecordAttachmentControllerAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private RecordAttachmentService recordAttachmentService;

    @Test
    void shouldReturn401WhenRequestUploadTokenWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/records/9001/attachments/upload-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldCreateUploadTokenWhenAuthorized() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
        when(recordAttachmentService.createUploadToken(eq(5001L), eq(9001L), any()))
                .thenReturn(uploadTokenVO());

        mockMvc.perform(post("/api/records/9001/attachments/upload-token")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "type": "IMAGE",
                          "fileName": "example.jpg",
                          "mimeType": "image/jpeg",
                          "sizeBytes": 123456
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.provider").value("QINIU"))
                .andExpect(jsonPath("$.data.bucket").value("flashback-private"))
                .andExpect(jsonPath("$.data.key").value("flashback/users/5001/records/9001/image/token.jpg"))
                .andExpect(jsonPath("$.data.uploadToken").value("token"))
                .andExpect(jsonPath("$.data.maxFileSizeBytes").value(41943040));
    }

    @Test
    void shouldReturn400WhenUploadTokenRequestMissingRequiredFields() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser());

        mockMvc.perform(post("/api/records/9001/attachments/upload-token")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "type": "IMAGE",
                          "fileName": "",
                          "mimeType": "image/jpeg",
                          "sizeBytes": 0
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void shouldCommitAttachmentWhenAuthorized() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
        when(recordAttachmentService.commitAttachment(eq(5001L), eq(9001L), any()))
                .thenReturn(recordAttachmentVO());

        mockMvc.perform(post("/api/records/9001/attachments/commit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "type": "IMAGE",
                          "key": "flashback/users/5001/records/9001/image/token.jpg",
                          "fileName": "example.jpg",
                          "mimeType": "image/jpeg",
                          "sizeBytes": 123456,
                          "width": 1200,
                          "height": 800,
                          "durationSeconds": null
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(7001))
                .andExpect(jsonPath("$.data.recordId").value(9001))
                .andExpect(jsonPath("$.data.type").value("IMAGE"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    private User enabledUser() {
        User user = new User();
        user.setId(5001L);
        user.setStatus(UserStatus.ENABLED);
        return user;
    }

    private AttachmentUploadTokenVO uploadTokenVO() {
        AttachmentUploadTokenVO vo = new AttachmentUploadTokenVO();
        vo.setProvider("QINIU");
        vo.setBucket("flashback-private");
        vo.setKey("flashback/users/5001/records/9001/image/token.jpg");
        vo.setUploadToken("token");
        vo.setUploadUrl("https://upload.qiniup.com");
        vo.setExpiresAt(LocalDateTime.of(2026, 6, 18, 10, 10, 0));
        vo.setMaxFileSizeBytes(41943040L);
        return vo;
    }

    private RecordAttachmentVO recordAttachmentVO() {
        RecordAttachmentVO vo = new RecordAttachmentVO();
        vo.setId(7001L);
        vo.setRecordId(9001L);
        vo.setType(com.flashback.domain.RecordAttachmentType.IMAGE);
        vo.setStatus(com.flashback.domain.RecordAttachmentStatus.AVAILABLE);
        vo.setFileName("example.jpg");
        vo.setMimeType("image/jpeg");
        vo.setSizeBytes(123456L);
        vo.setWidth(1200);
        vo.setHeight(800);
        vo.setSortOrder(0);
        vo.setCreatedAt(LocalDateTime.of(2026, 6, 18, 10, 0, 0));
        vo.setAccessUrl(null);
        return vo;
    }
}
