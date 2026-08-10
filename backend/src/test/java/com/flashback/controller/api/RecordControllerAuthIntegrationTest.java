package com.flashback.controller.api;

import com.flashback.common.page.PageResult;
import com.flashback.domain.RecordLocationSource;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.domain.User;
import com.flashback.domain.UserStatus;
import com.flashback.dto.RecordPageQuery;
import com.flashback.dto.RecordTimelineQuery;
import com.flashback.mapper.UserMapper;
import com.flashback.security.auth.AuthRole;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.jwt.JwtTokenProvider;
import com.flashback.service.RecordService;
import com.flashback.vo.RecordDetailVO;
import com.flashback.vo.RecordListItemVO;
import com.flashback.vo.RecordLocationVO;
import com.flashback.vo.RecordTagVO;
import com.flashback.vo.TimelineGroupVO;
import com.flashback.vo.TimelineItemVO;
import com.flashback.vo.TimelinePageVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecordControllerAuthIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @MockBean
        private UserMapper userMapper;

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
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
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
        void shouldSaveOwnedDraftWhenAuthorized() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
                RecordDetailVO saved = mockDetail();
                saved.setStatus(RecordStatus.SAVED);
                when(recordService.save(5001L, 9001L)).thenReturn(saved);

                mockMvc.perform(post("/api/records/9001/save")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0))
                                .andExpect(jsonPath("$.data.status").value("SAVED"));

                verify(recordService).save(5001L, 9001L);
        }

        @Test
        void shouldPassMineRecordFiltersToService() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
                when(recordService.pageMine(org.mockito.ArgumentMatchers.eq(5001L), org.mockito.ArgumentMatchers.any()))
                                .thenReturn(PageResult.of(List.of(), 0L, 2, 20));

                mockMvc.perform(get("/api/records")
                                .header("Authorization", "Bearer " + token)
                                .param("pageNum", "2")
                                .param("pageSize", "20")
                                .param("status", "SEALED")
                                .param("recordType", "FUTURE_LETTER")
                                .param("tagId", "12")
                                .param("keyword", "阶段"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0));

                ArgumentCaptor<RecordPageQuery> queryCaptor = ArgumentCaptor.forClass(RecordPageQuery.class);
                verify(recordService).pageMine(org.mockito.ArgumentMatchers.eq(5001L), queryCaptor.capture());
                RecordPageQuery query = queryCaptor.getValue();
                assertThat(query.getPageNum()).isEqualTo(2);
                assertThat(query.getPageSize()).isEqualTo(20);
                assertThat(query.getStatus()).isEqualTo(RecordStatus.SEALED);
                assertThat(query.getRecordType()).isEqualTo(RecordType.FUTURE_LETTER);
                assertThat(query.getTagId()).isEqualTo(12L);
                assertThat(query.getKeyword()).isEqualTo("阶段");
        }

        @Test
        void shouldReturn400WhenRecordPageSizeTooLarge() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());

                mockMvc.perform(get("/api/records")
                                .header("Authorization", "Bearer " + token)
                                .param("pageSize", "201"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(40000))
                                .andExpect(jsonPath("$.message").value("pageSize: pageSize 最大为 200"));
        }

        @Test
        void shouldReturn400WhenRecordStatusIsInvalid() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());

                mockMvc.perform(get("/api/records")
                                .header("Authorization", "Bearer " + token)
                                .param("status", "INVALID"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(40000))
                                .andExpect(jsonPath("$.message").value("status: 参数格式不正确"));
        }

        @Test
        void shouldReturnUnlockedRecordsWhenAuthorized() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
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
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());

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
                                .thenReturn(TimelinePageVO.of(List.of(group), 1L, 1, 20));

                mockMvc.perform(get("/api/records/timeline")
                                .header("Authorization", "Bearer " + token)
                                .param("year", "2026"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0))
                                .andExpect(jsonPath("$.data.total").value(1))
                                .andExpect(jsonPath("$.data.pageSize").value(20))
                                .andExpect(jsonPath("$.data.hasMore").value(false))
                                .andExpect(jsonPath("$.data.groups[0].yearMonth").value("2026-03"))
                                .andExpect(jsonPath("$.data.groups[0].items[0].id").value(9001))
                                .andExpect(jsonPath("$.data.groups[0].items[0].tagNames[0]").value("焦虑"));
        }

        @Test
        void shouldPassTimelineFiltersToService() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
                when(recordService.timeline(org.mockito.ArgumentMatchers.eq(5001L), org.mockito.ArgumentMatchers.any()))
                                .thenReturn(TimelinePageVO.of(List.of(), 0L, 2, 15));

                mockMvc.perform(get("/api/records/timeline")
                                .header("Authorization", "Bearer " + token)
                                .param("year", "2026")
                                .param("month", "6")
                                .param("day", "22")
                                .param("tagId", "12")
                                .param("pageNum", "2")
                                .param("pageSize", "15"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0));

                ArgumentCaptor<RecordTimelineQuery> queryCaptor = ArgumentCaptor.forClass(RecordTimelineQuery.class);
                verify(recordService).timeline(org.mockito.ArgumentMatchers.eq(5001L), queryCaptor.capture());
                RecordTimelineQuery query = queryCaptor.getValue();
                assertThat(query.getYear()).isEqualTo(2026);
                assertThat(query.getMonth()).isEqualTo(6);
                assertThat(query.getDay()).isEqualTo(22);
                assertThat(query.getTagId()).isEqualTo(12L);
                assertThat(query.getPageNum()).isEqualTo(2);
                assertThat(query.getPageSize()).isEqualTo(15);
        }

        @Test
        void shouldReturn400WhenTimelineYearTooSmall() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());

                mockMvc.perform(get("/api/records/timeline")
                                .header("Authorization", "Bearer " + token)
                                .param("year", "1969"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(40000))
                                .andExpect(jsonPath("$.message").value("year: year最小为1970"));
        }

        @Test
        void shouldReturn400WhenTimelineDateDependencyIsInvalid() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());

                mockMvc.perform(get("/api/records/timeline")
                                .header("Authorization", "Bearer " + token)
                                .param("month", "6"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(40000));
        }

        @Test
        void shouldReturn400WhenTimelineDateDoesNotExist() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());

                mockMvc.perform(get("/api/records/timeline")
                                .header("Authorization", "Bearer " + token)
                                .param("year", "2026")
                                .param("month", "2")
                                .param("day", "30"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(40000));
        }

        @Test
        void shouldReturn400WhenTimelinePageSizeExceedsLimit() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());

                mockMvc.perform(get("/api/records/timeline")
                                .header("Authorization", "Bearer " + token)
                                .param("pageSize", "51"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value(40000));
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
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
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
        void shouldUpdateRecordLocationWhenAuthorized() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
                RecordDetailVO detail = mockDetail();
                RecordLocationVO location = new RecordLocationVO();
                location.setSource(RecordLocationSource.MAP_PICKER);
                location.setName("人民公园");
                location.setAddress("上海市黄浦区南京西路");
                detail.setLocation(location);
                when(recordService.updateLocation(org.mockito.ArgumentMatchers.eq(5001L),
                                org.mockito.ArgumentMatchers.eq(9001L),
                                org.mockito.ArgumentMatchers.any()))
                                .thenReturn(detail);

                mockMvc.perform(put("/api/records/9001/location")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("""
                                                {
                                                  "source": "MAP_PICKER",
                                                  "name": "人民公园",
                                                  "address": "上海市黄浦区南京西路",
                                                  "latitude": 31.2317,
                                                  "longitude": 121.4746
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0))
                                .andExpect(jsonPath("$.data.location.source").value("MAP_PICKER"))
                                .andExpect(jsonPath("$.data.location.name").value("人民公园"));
        }

        @Test
        void shouldDeleteRecordLocationWhenAuthorized() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
                when(recordService.deleteLocation(5001L, 9001L)).thenReturn(mockDetail());

                mockMvc.perform(delete("/api/records/9001/location")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0));

                verify(recordService).deleteLocation(5001L, 9001L);
        }

        @Test
        void shouldUpdateRecordCoverWhenAuthorized() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
                when(recordService.updateCover(org.mockito.ArgumentMatchers.eq(5001L),
                                org.mockito.ArgumentMatchers.eq(9001L),
                                org.mockito.ArgumentMatchers.any()))
                                .thenReturn(mockDetail());

                mockMvc.perform(put("/api/records/9001/cover")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("""
                                                {
                                                  "attachmentId": 7001
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(0));

                verify(recordService).updateCover(org.mockito.ArgumentMatchers.eq(5001L),
                                org.mockito.ArgumentMatchers.eq(9001L),
                                org.mockito.ArgumentMatchers.argThat(request ->
                                                request.getAttachmentId().equals(7001L)));
        }

        @Test
        void shouldReturnNotFoundWhenAccessOthersRecord() throws Exception {
                String token = jwtTokenProvider.createToken(new AuthUser(5001L, AuthRole.USER));
                when(userMapper.selectById(anyLong())).thenReturn(enabledUser());
                when(recordService.detail(5001L, 9999L))
                                .thenThrow(new com.flashback.common.exception.NotFoundException("记录不存在"));

                mockMvc.perform(get("/api/records/9999")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(40400));
        }

        private User enabledUser() {
                User user = new User();
                user.setId(5001L);
                user.setStatus(UserStatus.ENABLED);
                return user;
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
