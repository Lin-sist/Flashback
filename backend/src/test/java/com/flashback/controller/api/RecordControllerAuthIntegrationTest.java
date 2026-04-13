package com.flashback.controller.api;

import com.flashback.common.page.PageResult;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.RecordService;
import com.flashback.vo.RecordDetailVO;
import com.flashback.vo.RecordListItemVO;
import com.flashback.vo.RecordTagVO;
import com.flashback.vo.TimelineGroupVO;
import com.flashback.vo.TimelineItemVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecordControllerAuthIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @MockBean
        private RecordService recordService;

        @Test
        void shouldReturn401WhenAccessRecordApiWithoutLogin() throws Exception {
                mockMvc.perform(get("/api/records"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value(40100));
        }

        @Test
        void shouldReturnPagedMineRecordsWhenAuthorized() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(recordService.pageMine(org.mockito.ArgumentMatchers.eq(5001L), org.mockito.ArgumentMatchers.any()))
                                .thenReturn(PageResult.of(List.of(mockListItem()), 1L, 1, 10));

                mockMvc.perform(get("/api/records")
                                .header("Authorization", "Bearer " + token)
                                .param("pageNum", "1")
                                .param("pageSize", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0))
                                .andExpect(jsonPath("$.data.pageNum").value(1))
                                .andExpect(jsonPath("$.data.pageSize").value(10))
                                .andExpect(jsonPath("$.data.total").value(1))
                                .andExpect(jsonPath("$.data.list[0].id").value(9001))
                                .andExpect(jsonPath("$.data.list[0].status").value("DRAFT"));
        }

        @Test
        void shouldReturnUnlockedRecordsWhenAuthorized() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                RecordListItemVO item = mockListItem();
                item.setStatus(RecordStatus.UNLOCKED);

                when(recordService.pageMyUnlocked(org.mockito.ArgumentMatchers.eq(5001L),
                                org.mockito.ArgumentMatchers.any()))
                                .thenReturn(PageResult.of(List.of(item), 1L, 1, 10));

                mockMvc.perform(get("/api/records/unlocked")
                                .header("Authorization", "Bearer " + token)
                                .param("pageNum", "1")
                                .param("pageSize", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0))
                                .andExpect(jsonPath("$.data.total").value(1))
                                .andExpect(jsonPath("$.data.list[0].status").value("UNLOCKED"));
        }

        @Test
        void shouldReturn401WhenAccessUnlockedApiWithoutLogin() throws Exception {
                mockMvc.perform(get("/api/records/unlocked"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value(40100));
        }

        @Test
        void shouldReturnTimelineWhenAuthorized() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));

                TimelineItemVO item = new TimelineItemVO();
                item.setId(9001L);
                item.setTitle("节点记录");
                item.setRecordType(RecordType.NODE_RECORD);
                item.setStatus(RecordStatus.SEALED);
                item.setCreatedAt(LocalDateTime.of(2026, 3, 26, 9, 0, 0));
                item.setTagNames(List.of("焦虑"));

                TimelineGroupVO group = new TimelineGroupVO();
                group.setYearMonth("2026-03");
                group.setItems(List.of(item));

                when(recordService.timeline(org.mockito.ArgumentMatchers.eq(5001L), org.mockito.ArgumentMatchers.any()))
                                .thenReturn(List.of(group));

                mockMvc.perform(get("/api/records/timeline")
                                .header("Authorization", "Bearer " + token)
                                .param("year", "2026"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0))
                                .andExpect(jsonPath("$.data[0].yearMonth").value("2026-03"))
                                .andExpect(jsonPath("$.data[0].items[0].id").value(9001))
                                .andExpect(jsonPath("$.data[0].items[0].tagNames[0]").value("焦虑"));
        }

        @Test
        void shouldReturn401WhenAccessTimelineWithoutLogin() throws Exception {
                mockMvc.perform(get("/api/records/timeline"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value(40100));
        }

        @Test
        void shouldReturnRecordDetailWithAiAndReplyFlagsWhenAuthorized() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(recordService.detail(5001L, 9001L)).thenReturn(mockDetail());

                mockMvc.perform(get("/api/records/9001")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0))
                                .andExpect(jsonPath("$.data.id").value(9001))
                                .andExpect(jsonPath("$.data.aiSummary").value("当前主要困惑在求职节奏"))
                                .andExpect(jsonPath("$.data.aiPromptResults[0]").value("你现在最担心什么？"))
                                .andExpect(jsonPath("$.data.aiPromptResults[1]").value("你今天最想推进哪一步？"))
                                .andExpect(jsonPath("$.data.canReply").value(true))
                                .andExpect(jsonPath("$.data.hasReply").value(false))
                                .andExpect(jsonPath("$.data.tags[0].name").value("焦虑"));
        }

        @Test
        void shouldReturnNotFoundWhenAccessOthersRecord() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(recordService.detail(5001L, 9999L))
                                .thenThrow(new com.flashback.common.exception.NotFoundException("记录不存在"));

                mockMvc.perform(get("/api/records/9999")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(40400));
        }

        private RecordListItemVO mockListItem() {
                RecordListItemVO vo = new RecordListItemVO();
                vo.setId(9001L);
                vo.setTitle("节点记录");
                vo.setContentPreview("今天完成了MVP主链路...");
                vo.setRecordType(RecordType.NODE_RECORD);
                vo.setStatus(RecordStatus.DRAFT);
                vo.setUnlockAt(LocalDateTime.of(2026, 4, 1, 10, 0, 0));
                vo.setCreatedAt(LocalDateTime.of(2026, 3, 26, 9, 0, 0));
                return vo;
        }

        private RecordDetailVO mockDetail() {
                RecordTagVO tag = new RecordTagVO();
                tag.setId(1L);
                tag.setName("焦虑");
                tag.setType(com.flashback.domain.TagType.MOOD);

                RecordDetailVO vo = new RecordDetailVO();
                vo.setId(9001L);
                vo.setTitle("节点记录");
                vo.setContent("最近在求职和项目之间来回拉扯");
                vo.setRecordType(RecordType.NODE_RECORD);
                vo.setCoreQuestion("我应该先补项目还是投递");
                vo.setStatus(RecordStatus.UNLOCKED);
                vo.setUnlockAt(LocalDateTime.of(2026, 4, 1, 10, 0, 0));
                vo.setSealedAt(LocalDateTime.of(2026, 3, 26, 10, 0, 0));
                vo.setUnlockedAt(LocalDateTime.of(2026, 4, 1, 10, 0, 0));
                vo.setAiSummary("当前主要困惑在求职节奏");
                vo.setAiPromptResults(List.of("你现在最担心什么？", "你今天最想推进哪一步？"));
                vo.setCanReply(true);
                vo.setHasReply(false);
                vo.setTags(List.of(tag));
                vo.setCreatedAt(LocalDateTime.of(2026, 3, 26, 9, 0, 0));
                vo.setUpdatedAt(LocalDateTime.of(2026, 3, 26, 10, 0, 0));
                return vo;
        }
}
