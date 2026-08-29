package com.flashback.service.impl;

import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.TimeChapterStatus;
import com.flashback.dto.ChangeTimeChapterMembersRequest;
import com.flashback.dto.CreateTimeChapterRequest;
import com.flashback.dto.TimeChapterMemberPageQuery;
import com.flashback.dto.TransferConfirmation;
import com.flashback.dto.UpdateTimeChapterRequest;
import com.flashback.dto.RecordPageQuery;
import com.flashback.service.RecordService;
import com.flashback.service.TimeChapterService;
import com.flashback.vo.TimeChapterDetailVO;
import com.flashback.vo.TimeChapterSummaryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TimeChapterServiceIntegrationTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 8, 12, 8, 0);

    @Autowired
    private TimeChapterService service;

    @Autowired
    private RecordService recordService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsOwnerScopedChapterAndDerivesCoverageWithoutChangingRecords() {
        insertUser(51001L, "chapter-owner-51001");
        insertRecord(51101L, 51001L, "SAVED", BASE.minusDays(3));
        insertRecord(51102L, 51001L, "SEALED", BASE.minusDays(1));
        insertRecord(51103L, 51001L, "UNLOCKED", BASE);
        LocalDateTime recordUpdatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM `record` WHERE id=?", LocalDateTime.class, 51102L);

        TimeChapterSummaryVO created = service.create(51001L, create("一段时间", "手动留下", 51101L, 51102L, 51103L));

        assertThat(created.getStatus()).isEqualTo(TimeChapterStatus.ACTIVE);
        assertThat(created.getMemberCount()).isEqualTo(3);
        assertThat(created.getCoverageStartAt()).isEqualTo(BASE.minusDays(3));
        assertThat(created.getCoverageEndAt()).isEqualTo(BASE);
        assertThat(created.getVersion()).isZero();
        assertThat(jdbc.queryForObject("SELECT updated_at FROM `record` WHERE id=?", LocalDateTime.class, 51102L))
                .isEqualTo(recordUpdatedAt);
        assertThat(service.page(51001L, null).getList()).extracting(TimeChapterSummaryVO::getName)
                .containsExactly("一段时间");
    }

    @Test
    void rejectsDraftAndEmptyBatchAsOneTransactionAndKeepsPassiveEmptyChapter() {
        insertUser(51011L, "chapter-owner-51011");
        insertRecord(51111L, 51011L, "SAVED", BASE);
        insertRecord(51112L, 51011L, "DRAFT", BASE.plusDays(1));

        assertThatThrownBy(() -> service.create(51011L, create("不应创建", null, 51111L, 51112L)))
                .isInstanceOf(BizException.class)
                .hasMessage("草稿不能加入篇章");
        assertThat(jdbc.queryForObject("SELECT COUNT(1) FROM time_chapter WHERE user_id=?", Integer.class, 51011L))
                .isZero();

        TimeChapterSummaryVO chapter = service.create(51011L, create("可为空", null, 51111L));
        ChangeTimeChapterMembersRequest remove = members(chapter.getVersion(), 51111L);
        TimeChapterSummaryVO empty = service.removeMembers(51011L, chapter.getId(), remove);
        assertThat(empty.getMemberCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(1) FROM `record` WHERE user_id=?", Integer.class, 51011L))
                .isEqualTo(2);
    }

    @Test
    void supportsVersionedMetadataLifecycleAndEndedRejectsIncomingMembers() {
        insertUser(51021L, "chapter-owner-51021");
        insertRecord(51121L, 51021L, "SAVED", BASE);
        insertRecord(51122L, 51021L, "SAVED", BASE.plusDays(1));
        TimeChapterSummaryVO chapter = service.create(51021L, create("原名", null, 51121L));

        UpdateTimeChapterRequest update = new UpdateTimeChapterRequest();
        update.setExpectedVersion(chapter.getVersion());
        update.setName("新名");
        update.setNote("自述");
        TimeChapterSummaryVO updated = service.update(51021L, chapter.getId(), update);
        assertThat(updated.getName()).isEqualTo("新名");
        assertThat(updated.getVersion()).isEqualTo(1L);

        assertThatThrownBy(() -> service.update(51021L, chapter.getId(), update))
                .isInstanceOf(BizException.class)
                .hasMessage("篇章状态已变更，请刷新后重试");
        TimeChapterSummaryVO ended = service.end(51021L, chapter.getId(), updated.getVersion());
        assertThat(ended.getStatus()).isEqualTo(TimeChapterStatus.ENDED);
        assertThat(ended.getEndedAt()).isNotNull();
        assertThat(ended.getVersion()).isEqualTo(2L);
        assertThat(service.end(51021L, chapter.getId(), 0L).getVersion()).isEqualTo(2L);

        assertThatThrownBy(() -> service.addMembers(51021L, chapter.getId(), members(ended.getVersion(), 51122L)))
                .isInstanceOf(BizException.class)
                .hasMessage("已结束篇章不能加入记录");
        TimeChapterSummaryVO reopened = service.reopen(51021L, chapter.getId(), ended.getVersion());
        assertThat(reopened.getStatus()).isEqualTo(TimeChapterStatus.ACTIVE);
        assertThat(reopened.getEndedAt()).isNull();
    }

    @Test
    void repeatedMemberCommandsAreIdempotentWithoutVersionBump() {
        insertUser(51025L, "chapter-owner-51025");
        insertRecord(51125L, 51025L, "SAVED", BASE);
        TimeChapterSummaryVO chapter = service.create(51025L, create("可重复操作", null, 51125L));

        TimeChapterSummaryVO repeatedAdd = service.addMembers(51025L, chapter.getId(), members(0L, 51125L));
        assertThat(repeatedAdd.getMemberCount()).isEqualTo(1);
        assertThat(repeatedAdd.getVersion()).isZero();

        TimeChapterSummaryVO empty = service.removeMembers(51025L, chapter.getId(), members(0L, 51125L));
        assertThat(empty.getMemberCount()).isZero();
        assertThat(empty.getVersion()).isEqualTo(1L);

        TimeChapterSummaryVO repeatedRemove = service.removeMembers(51025L, chapter.getId(), members(1L, 51125L));
        assertThat(repeatedRemove.getMemberCount()).isZero();
        assertThat(repeatedRemove.getVersion()).isEqualTo(1L);
    }

    @Test
    void databaseRejectsDuplicatePrimaryChapterMembership() {
        insertUser(51026L, "chapter-owner-51026");
        insertRecord(51126L, 51026L, "SAVED", BASE);
        insertRecord(51127L, 51026L, "SAVED", BASE.plusDays(1));
        service.create(51026L, create("唯一归属来源", null, 51126L));
        TimeChapterSummaryVO target = service.create(51026L, create("唯一归属目标", null, 51127L));

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO time_chapter_record(chapter_id,record_id,user_id,added_at) VALUES (?,?,?,?)",
                target.getId(), 51126L, 51026L, BASE))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void databaseRejectsCrossOwnerChapterRecordRelationship() {
        insertUser(51027L, "chapter-owner-51027");
        insertUser(51028L, "chapter-owner-51028");
        insertRecord(51128L, 51028L, "SAVED", BASE);
        insertRecord(51127L, 51027L, "SAVED", BASE.plusDays(1));
        TimeChapterSummaryVO chapter = service.create(51027L, create("甲的篇章", null, 51127L));

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO time_chapter_record(chapter_id,record_id,user_id,added_at) VALUES (?,?,?,?)",
                chapter.getId(), 51128L, 51027L, BASE))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void requiresExactTransferSourceAndMovesRelationAtomically() {
        insertUser(51031L, "chapter-owner-51031");
        insertRecord(51131L, 51031L, "SAVED", BASE);
        insertRecord(51132L, 51031L, "SAVED", BASE.plusDays(1));
        TimeChapterSummaryVO source = service.create(51031L, create("来源", null, 51131L));
        TimeChapterSummaryVO target = service.create(51031L, create("目标", null, 51132L));
        target = service.addMembers(51031L, target.getId(), membersWithTransfer(target.getVersion(), 51131L, source.getId()));

        assertThat(target.getMemberCount()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT chapter_id FROM time_chapter_record WHERE record_id=?", Long.class, 51131L))
                .isEqualTo(target.getId());
        assertThat(service.detail(51031L, source.getId(), new TimeChapterMemberPageQuery()).getMembers().getTotal())
                .isZero();
        assertThatThrownBy(() -> service.addMembers(51031L, source.getId(), members(source.getVersion(), 51131L)))
                .isInstanceOf(BizException.class)
                .hasMessage("篇章归属已变更，请刷新后重试");
    }

    @Test
    void crossOwnerReadIsNotFoundAndDeletingChapterKeepsRecord() {
        insertUser(51041L, "chapter-owner-51041");
        insertUser(51042L, "chapter-owner-51042");
        insertRecord(51141L, 51041L, "UNLOCKED", BASE);
        TimeChapterSummaryVO chapter = service.create(51041L, create("私有篇章", null, 51141L));

        assertThatThrownBy(() -> service.detail(51042L, chapter.getId(), new TimeChapterMemberPageQuery()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("篇章不存在");
        service.delete(51041L, chapter.getId(), chapter.getVersion());
        assertThat(jdbc.queryForObject("SELECT COUNT(1) FROM time_chapter WHERE id=?", Integer.class, chapter.getId()))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(1) FROM `record` WHERE id=?", Integer.class, 51141L))
                .isEqualTo(1);
    }

    @Test
    void recordDeleteCascadeRecomputesChapterCoverage() {
        insertUser(51051L, "chapter-owner-51051");
        insertRecord(51151L, 51051L, "SAVED", BASE.minusDays(2));
        insertRecord(51152L, 51051L, "SAVED", BASE);
        TimeChapterSummaryVO chapter = service.create(51051L, create("会变短", null, 51151L, 51152L));

        jdbc.update("DELETE FROM `record` WHERE id=? AND user_id=?", 51151L, 51051L);

        TimeChapterSummaryVO refreshed = service.page(51051L, null).getList().get(0);
        assertThat(refreshed.getId()).isEqualTo(chapter.getId());
        assertThat(refreshed.getMemberCount()).isEqualTo(1);
        assertThat(refreshed.getCoverageStartAt()).isEqualTo(BASE);
        assertThat(refreshed.getCoverageEndAt()).isEqualTo(BASE);
    }

    @Test
    void recordListAndDetailExposeOnlyNullableChapterSummary() {
        insertUser(51061L, "chapter-owner-51061");
        insertRecord(51161L, 51061L, "UNLOCKED", BASE);
        TimeChapterSummaryVO chapter = service.create(51061L, create("记录摘要", "不进入记录正文", 51161L));

        RecordPageQuery query = new RecordPageQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        assertThat(recordService.pageMine(51061L, query).getList().get(0).getChapter().getId())
                .isEqualTo(chapter.getId());
        assertThat(recordService.detail(51061L, 51161L).getChapter().getName()).isEqualTo("记录摘要");
        assertThat(jdbc.queryForObject("SELECT content FROM `record` WHERE id=?", String.class, 51161L))
                .isEqualTo("合成正文");
    }

    private CreateTimeChapterRequest create(String name, String note, Long... ids) {
        CreateTimeChapterRequest request = new CreateTimeChapterRequest();
        request.setName(name);
        request.setNote(note);
        request.setRecordIds(List.of(ids));
        return request;
    }

    private ChangeTimeChapterMembersRequest members(Long version, Long... ids) {
        ChangeTimeChapterMembersRequest request = new ChangeTimeChapterMembersRequest();
        request.setExpectedVersion(version);
        request.setRecordIds(List.of(ids));
        return request;
    }

    private ChangeTimeChapterMembersRequest membersWithTransfer(Long version, Long recordId, Long sourceId) {
        ChangeTimeChapterMembersRequest request = members(version, recordId);
        TransferConfirmation transfer = new TransferConfirmation();
        transfer.setRecordId(recordId);
        transfer.setFromChapterId(sourceId);
        request.setTransfers(List.of(transfer));
        return request;
    }

    private void insertUser(long id, String username) {
        jdbc.update("INSERT INTO `user`(id,username,password_hash,nickname,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",
                id, username, "hash", username, "ENABLED", BASE, BASE);
    }

    private void insertRecord(long id, long userId, String status, LocalDateTime createdAt) {
        LocalDateTime expiry = RecordStatus.DRAFT.name().equals(status) ? createdAt.plusDays(7) : null;
        jdbc.update("INSERT INTO `record`(id,user_id,title,content,record_type,status,draft_expires_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?)",
                id, userId, "合成记录-" + id, "合成正文", "MOMENT", status, expiry, createdAt, createdAt);
    }
}
