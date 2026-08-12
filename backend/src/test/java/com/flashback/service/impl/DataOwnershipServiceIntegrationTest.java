package com.flashback.service.impl;

import com.flashback.domain.DataDeletionScope;
import com.flashback.domain.DataOperationStatus;
import com.flashback.service.DataOwnershipService;
import com.flashback.mapper.RecordMapper;
import com.flashback.service.data.DataOwnershipMutationGuard;
import com.flashback.storage.ObjectStorageProvider;
import com.flashback.storage.ObjectStorageRegistry;
import com.flashback.vo.DataOperationVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataOwnershipServiceIntegrationTest {
    @Autowired private DataOwnershipService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RecordMapper recordMapper;
    @Autowired private DataOwnershipMutationGuard mutationGuard;
    @MockBean private ObjectStorageRegistry storageRegistry;

    @Test
    void clearAllDeletesEveryRecordStateAndRecordLinkedDerivedData() {
        long userId = 98001L;
        insertUser(userId, "owner-98001");
        insertRecord(98101L, userId, "DRAFT");
        insertRecord(98102L, userId, "SAVED");
        insertRecord(98103L, userId, "SEALED");
        insertRecord(98104L, userId, "UNLOCKED");
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 8, 0);
        jdbc.update("INSERT INTO record_location(record_id,user_id,source,name,created_at,updated_at) VALUES (?,?,?,?,?,?)", 98102L, userId, "MANUAL", "合成位置", now, now);
        jdbc.update("INSERT INTO reply(record_id,user_id,content,reply_type,created_at) VALUES (?,?,?,?,?)", 98104L, userId, "合成回信", "SHORT_REPLY", now);
        jdbc.update("INSERT INTO agent_session(id,user_id,record_id,purpose,stage,status,turn_count,stage_reask_count,last_active_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                98201L, userId, 98103L, "WRITING_GUIDANCE", "OPENING", "ACTIVE", 0, 0, now, now, now);
        jdbc.update("INSERT INTO agent_message(session_id,user_id,role,turn_no,stage,content,created_at) VALUES (?,?,?,?,?,?,?)",
                98201L, userId, "USER", 1, "OPENING", "合成消息", now);
        ObjectStorageProvider provider = org.mockito.Mockito.mock(ObjectStorageProvider.class);
        when(storageRegistry.getRequired(any())).thenReturn(provider);
        jdbc.update("INSERT INTO record_attachment(record_id,user_id,type,storage_provider,bucket,storage_key,file_name,mime_type,size_bytes,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                98102L, userId, "IMAGE", "QINIU", "synthetic-bucket", "synthetic-key", "synthetic.jpg", "image/jpeg", 16L, "AVAILABLE", now, now);
        jdbc.update("INSERT INTO tag(id,name,type,status,created_at) VALUES (?,?,?,?,?)", 98211L, "合成标签", "TOPIC", "ENABLED", now);
        jdbc.update("INSERT INTO record_tag(record_id,tag_id) VALUES (?,?)", 98102L, 98211L);
        jdbc.update("INSERT INTO record_reminder(record_id,user_id,template_type,reminder_status,created_at,updated_at) VALUES (?,?,?,?,?,?)", 98103L, userId, "UNLOCK_REMINDER", "AUTHORIZED", now, now);
        jdbc.update("INSERT INTO unlock_notice_log(record_id,user_id,notice_type,notice_status,created_at) VALUES (?,?,?,?,?)", 98103L, userId, "SYSTEM_UNLOCK", "SUCCESS", now);
        jdbc.update("INSERT INTO agent_tool_call(session_id,user_id,record_id,turn_no,tool_name,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)",
                98201L, userId, 98103L, 1, "append_record_content", "PROPOSED", now, now);
        jdbc.update("INSERT INTO agent_turn_trace(trace_id,session_id,user_id,record_id,turn_no,attempt_no,purpose,stage,outcome,created_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                "0123456789abcdef0123456789abcdef", 98201L, userId, 98103L, 1, 1, "WRITING_GUIDANCE", "OPENING", "SUCCESS", now);

        DataOperationVO intent = service.prepareDeletion(userId, DataDeletionScope.ALL_RECORDS, null);
        assertEquals(DataOperationStatus.PREPARED, intent.getStatus());
        assertEquals(4, intent.getTotalItems());
        assertNotNull(intent.getConfirmationText());
        assertNotEquals(intent.getConfirmationText(), jdbc.queryForObject("SELECT confirmation_nonce_hash FROM data_operation WHERE id=?", String.class, intent.getId()));

        DataOperationVO completed = service.confirmDeletion(userId, intent.getId(), intent.getConfirmationText());
        assertEquals(DataOperationStatus.SUCCEEDED, completed.getStatus());
        assertEquals(4, completed.getProcessedItems());
        assertEquals(0, count("record", userId));
        assertEquals(0, count("record_location", userId));
        assertEquals(0, count("reply", userId));
        assertEquals(0, count("agent_session", userId));
        assertEquals(0, count("agent_message", userId));
        assertEquals(0, count("record_attachment", userId));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(1) FROM record_tag WHERE record_id IN (98101,98102,98103,98104)", Integer.class));
        assertEquals(0, count("record_reminder", userId));
        assertEquals(0, count("unlock_notice_log", userId));
        assertEquals(0, count("agent_tool_call", userId));
        assertEquals(0, count("agent_turn_trace", userId));
        verify(provider).deleteObject("synthetic-bucket", "synthetic-key");
        assertEquals(DataOperationStatus.SUCCEEDED, service.confirmDeletion(userId, intent.getId(), intent.getConfirmationText()).getStatus());
    }

    @Test
    void singleRecordIntentIsOwnerScoped() {
        insertUser(98011L, "owner-98011"); insertUser(98012L, "owner-98012");
        insertRecord(98111L, 98011L, "SAVED");
        assertThrows(RuntimeException.class, () -> service.prepareDeletion(98012L, DataDeletionScope.RECORD, 98111L));
        assertEquals(1, count("record", 98011L));
    }

    @Test
    void confirmedTargetIsHiddenAndClearAllBlocksMutations() {
        long userId = 98021L; long recordId = 98121L;
        insertUser(userId, "owner-98021"); insertRecord(recordId, userId, "SAVED");
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 8, 0);
        jdbc.update("INSERT INTO data_operation(id,user_id,operation_type,status,total_items,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",
                98321L, userId, "CLEAR_ALL_RECORDS", "RUNNING", 1, now, now);
        jdbc.update("INSERT INTO data_operation_record(operation_id,user_id,record_id,item_status,created_at,updated_at) VALUES (?,?,?,?,?,?)",
                98321L, userId, recordId, "RUNNING", now, now);
        assertNull(recordMapper.selectByIdAndUserId(recordId, userId));
        assertNotNull(recordMapper.selectByIdAndUserIdForDeletion(recordId, userId));
        assertThrows(RuntimeException.class, () -> mutationGuard.assertWritable(userId));
    }

    @Test
    void activeIntentBlocksASecondOperationAndExpiryFailsClosed() {
        long userId = 98031L; insertUser(userId, "owner-98031"); insertRecord(98131L, userId, "SAVED");
        DataOperationVO intent = service.prepareDeletion(userId, DataDeletionScope.RECORD, 98131L);
        assertThrows(RuntimeException.class, () -> service.createExport(userId, com.flashback.domain.SealedContentPolicy.RESPECT_SEAL));
        jdbc.update("UPDATE data_operation SET confirmation_expires_at=? WHERE id=?", LocalDateTime.of(2020, 1, 1, 0, 0), intent.getId());
        assertThrows(RuntimeException.class, () -> service.confirmDeletion(userId, intent.getId(), intent.getConfirmationText()));
        assertEquals(DataOperationStatus.EXPIRED, service.getOperation(userId, intent.getId()).getStatus());
    }

    private void insertUser(long id, String username) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 8, 0);
        jdbc.update("INSERT INTO `user`(id,username,password_hash,nickname,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?)", id, username, "hash", username, "ENABLED", now, now);
    }
    private void insertRecord(long id, long userId, String status) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 8, 0);
        LocalDateTime expiry = "DRAFT".equals(status) ? now.plusDays(7) : null;
        jdbc.update("INSERT INTO `record`(id,user_id,title,content,record_type,status,draft_expires_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?)",
                id, userId, "合成记录", "仅测试事务内合成", "MOMENT", status, expiry, now, now);
    }
    private int count(String table, long userId) { return jdbc.queryForObject("SELECT COUNT(1) FROM " + table + " WHERE user_id=?", Integer.class, userId); }
}
