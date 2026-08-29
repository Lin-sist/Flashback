package com.flashback.controller.api;

import com.flashback.common.page.PageResult;
import com.flashback.domain.TimeChapterStatus;
import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.dto.ChangeTimeChapterMembersRequest;
import com.flashback.dto.TimeChapterPageQuery;
import com.flashback.mapper.UserMapper;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.TimeChapterService;
import com.flashback.vo.TimeChapterSummaryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TimeChapterControllerAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private TimeChapterService chapterService;

    @Test
    void shouldReturn401WhenAccessChapterApiWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/time-chapters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldReturnOwnedPagedChaptersWithSummaryFields() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(52001L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
        TimeChapterSummaryVO chapter = new TimeChapterSummaryVO();
        chapter.setId(52101L);
        chapter.setName("一段时间");
        chapter.setStatus(TimeChapterStatus.ACTIVE);
        chapter.setMemberCount(2);
        chapter.setCoverageStartAt(LocalDateTime.of(2026, 8, 1, 8, 0));
        chapter.setCoverageEndAt(LocalDateTime.of(2026, 8, 12, 8, 0));
        chapter.setVersion(0L);
        when(chapterService.page(eq(52001L), any(TimeChapterPageQuery.class)))
                .thenReturn(PageResult.of(List.of(chapter), 1L, 1, 10));

        mockMvc.perform(get("/api/time-chapters")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(52101))
                .andExpect(jsonPath("$.data.list[0].memberCount").value(2))
                .andExpect(jsonPath("$.data.list[0].status").value("ACTIVE"));
    }

    @Test
    void shouldPassExpectedVersionAndTransferPayloadToService() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(52002L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
        TimeChapterSummaryVO result = new TimeChapterSummaryVO();
        result.setId(52201L);
        result.setVersion(2L);
        when(chapterService.addMembers(eq(52002L), eq(52201L), any(ChangeTimeChapterMembersRequest.class)))
                .thenReturn(result);

        mockMvc.perform(post("/api/time-chapters/52201/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"expectedVersion":1,"recordIds":[52301],"transfers":[{"recordId":52301,"fromChapterId":52200}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.version").value(2));

        verify(chapterService).addMembers(eq(52002L), eq(52201L), any(ChangeTimeChapterMembersRequest.class));
    }

    @Test
    void shouldMapDatabaseConcurrencyFailureToRefreshConflict() throws Exception {
        String token = jwtTokenProvider.createToken(new AuthUser(52003L, AuthRole.USER));
        when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
        when(chapterService.addMembers(eq(52003L), eq(52203L), any(ChangeTimeChapterMembersRequest.class)))
                .thenThrow(new CannotAcquireLockException("synthetic deadlock"));

        mockMvc.perform(post("/api/time-chapters/52203/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"expectedVersion":1,"recordIds":[52303]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("篇章状态已变更，请刷新后重试"));
    }

    private User enabledUser() {
        User user = new User();
        user.setId(52001L);
        user.setUsername("chapter-auth-user");
        user.setNickname("篇章用户");
        user.setStatus(UserStatus.ENABLED);
        return user;
    }
}
