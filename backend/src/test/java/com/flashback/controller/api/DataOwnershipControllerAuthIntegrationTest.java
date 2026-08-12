package com.flashback.controller.api;

import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.mapper.UserMapper;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.DataOwnershipService;
import com.flashback.vo.DataOwnershipSummaryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataOwnershipControllerAuthIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserMapper userMapper;
    @MockBean private DataOwnershipService service;

    @Test
    void rejectsAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/data-ownership/summary"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void passesAuthenticatedOwnerIdToService() throws Exception {
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
        when(service.summary(5001L)).thenReturn(new DataOwnershipSummaryVO());
        String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
        mockMvc.perform(get("/api/data-ownership/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verify(service).summary(5001L);
    }

    private User enabledUser() {
        User user = new User(); user.setId(5001L); user.setUsername("owner"); user.setNickname("owner"); user.setPasswordHash("hash");
        user.setStatus(UserStatus.ENABLED); user.setCreatedAt(LocalDateTime.now()); user.setUpdatedAt(LocalDateTime.now()); return user;
    }
}
