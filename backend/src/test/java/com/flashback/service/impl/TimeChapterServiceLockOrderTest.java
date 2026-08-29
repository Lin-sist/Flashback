package com.flashback.service.impl;

import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.TimeChapter;
import com.flashback.domain.TimeChapterRecord;
import com.flashback.domain.TimeChapterStatus;
import com.flashback.dto.ChangeTimeChapterMembersRequest;
import com.flashback.dto.TransferConfirmation;
import com.flashback.mapper.RecordAttachmentMapper;
import com.flashback.mapper.RecordMapper;
import com.flashback.mapper.TagMapper;
import com.flashback.mapper.TimeChapterMapper;
import com.flashback.mapper.TimeChapterRecordMapper;
import com.flashback.service.data.DataOwnershipMutationGuard;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeChapterServiceLockOrderTest {

    @Test
    void locksAllChapterIdsInAscendingOrderBeforeRecordRowsDuringTransfer() {
        TimeChapterMapper chapterMapper = mock(TimeChapterMapper.class);
        TimeChapterRecordMapper relationMapper = mock(TimeChapterRecordMapper.class);
        RecordMapper recordMapper = mock(RecordMapper.class);
        TagMapper tagMapper = mock(TagMapper.class);
        RecordAttachmentMapper attachmentMapper = mock(RecordAttachmentMapper.class);
        DataOwnershipMutationGuard mutationGuard = mock(DataOwnershipMutationGuard.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-28T08:00:00Z"), ZoneOffset.UTC);
        TimeChapterServiceImpl service = new TimeChapterServiceImpl(chapterMapper, relationMapper, recordMapper,
                tagMapper, attachmentMapper, mutationGuard, clock);

        long userId = 1L;
        long sourceId = 10L;
        long targetId = 20L;
        long recordId = 100L;
        TimeChapterRecord sourceRelation = relation(sourceId, recordId, userId);
        when(relationMapper.selectByRecordIdsAndUserId(List.of(recordId), userId))
                .thenReturn(List.of(sourceRelation));
        when(relationMapper.selectByRecordIdAndUserIdForUpdate(recordId, userId))
                .thenReturn(sourceRelation);
        when(chapterMapper.selectByIdAndUserIdForUpdate(anyLong(), anyLong()))
                .thenAnswer(invocation -> chapter(invocation.getArgument(0), userId));
        when(chapterMapper.selectByIdAndUserId(targetId, userId)).thenReturn(chapter(targetId, userId));
        Record record = new Record();
        record.setId(recordId);
        record.setUserId(userId);
        record.setStatus(RecordStatus.SAVED);
        when(recordMapper.selectByIdAndUserIdForChapterUpdate(recordId, userId)).thenReturn(record);

        ChangeTimeChapterMembersRequest request = new ChangeTimeChapterMembersRequest();
        request.setExpectedVersion(0L);
        request.setRecordIds(List.of(recordId));
        TransferConfirmation transfer = new TransferConfirmation();
        transfer.setRecordId(recordId);
        transfer.setFromChapterId(sourceId);
        request.setTransfers(List.of(transfer));

        service.addMembers(userId, targetId, request);

        InOrder locks = inOrder(chapterMapper, recordMapper);
        locks.verify(chapterMapper).selectByIdAndUserIdForUpdate(sourceId, userId);
        locks.verify(chapterMapper).selectByIdAndUserIdForUpdate(targetId, userId);
        locks.verify(recordMapper).selectByIdAndUserIdForChapterUpdate(recordId, userId);
    }

    private TimeChapter chapter(Long id, Long userId) {
        TimeChapter chapter = new TimeChapter();
        chapter.setId(id);
        chapter.setUserId(userId);
        chapter.setName("合成篇章");
        chapter.setStatus(TimeChapterStatus.ACTIVE);
        chapter.setVersion(0L);
        chapter.setCreatedAt(LocalDateTime.of(2026, 8, 28, 8, 0));
        chapter.setUpdatedAt(chapter.getCreatedAt());
        chapter.setMemberCount(1);
        return chapter;
    }

    private TimeChapterRecord relation(Long chapterId, Long recordId, Long userId) {
        TimeChapterRecord relation = new TimeChapterRecord();
        relation.setChapterId(chapterId);
        relation.setRecordId(recordId);
        relation.setUserId(userId);
        relation.setAddedAt(LocalDateTime.of(2026, 8, 28, 8, 0));
        return relation;
    }
}
