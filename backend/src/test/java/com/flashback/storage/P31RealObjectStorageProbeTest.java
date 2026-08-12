package com.flashback.storage;

import com.flashback.common.exception.BizException;
import com.flashback.config.AppStorageProperties;
import com.flashback.domain.RecordAttachmentStatus;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.RecordType;
import com.flashback.domain.StorageProvider;
import com.flashback.dto.CommitRecordAttachmentRequest;
import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.dto.CreateRecordRequest;
import com.flashback.dto.UpdateRecordRequest;
import com.flashback.service.DraftCleanupResult;
import com.flashback.service.DraftCleanupWorker;
import com.flashback.service.RecordAttachmentService;
import com.flashback.service.RecordService;
import com.flashback.vo.AttachmentAccessUrlVO;
import com.flashback.vo.AttachmentUploadTokenVO;
import com.flashback.vo.RecordAttachmentVO;
import com.flashback.vo.RecordDetailVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** P3.1 Gate 3b：真实私有对象存储的可清理合成探针。 */
@EnabledIfEnvironmentVariable(named = "P31_STORAGE_PROBE", matches = "1")
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "app.ai.provider=mock",
        "app.record.unlock-job-cron=-",
        "app.record.draft-cleanup-cron=-",
        "logging.level.com.flashback=INFO"
})
@ActiveProfiles("dev")
class P31RealObjectStorageProbeTest {

    private static final long USER_ID = 9_931_000L;
    private static final String USERNAME = "p31-storage-probe";
    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl9Z1sAAAAASUVORK5CYII=");

    @Autowired private RecordService recordService;
    @Autowired private RecordAttachmentService attachmentService;
    @Autowired private DraftCleanupWorker cleanupWorker;
    @Autowired private ObjectStorageRegistry storageRegistry;
    @Autowired private AppStorageProperties storageProperties;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final List<UploadedObject> uploadedObjects = new ArrayList<>();
    private String originalS3Secret;

    @DynamicPropertySource
    static void realDependencies(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv().getOrDefault(
                "DB_URL",
                "jdbc:mysql://127.0.0.1:3306/flashback"
                        + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("DB_USERNAME", "root"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("DB_PASSWORD", ""));
    }

    @Test
    void realStorageMustSupportMediaOnlySaveAndBoundedDraftCleanup() throws Exception {
        assertCleanStart();
        originalS3Secret = storageProperties.getS3().getSecretKey();
        insertSyntheticUser();
        try {
            probeImageOnly();
            probeVoiceOnly();
            probePendingAndMissingObject();
            probeExpiredDraftCleanup();
            System.out.println(
                    "P31STORAGE PASS image=true voice=true pending=true missing=true "
                            + "cleanupSuccess=true cleanupAbsent=true cleanupRetry=true");
        } finally {
            restoreStorageSecret();
            cleanupSyntheticState();
        }
        assertSyntheticStateRemoved();
        System.out.println("P31STORAGE CLEANUP objects=removed database=removed");
    }

    private void probeImageOnly() throws Exception {
        RecordDetailVO draft = createBlankDraft();
        UploadResult image = uploadAndCommit(
                draft.getId(), RecordAttachmentType.IMAGE, "probe.png", "image/png", PNG_BYTES, 1, 1, null);
        RecordDetailVO saved = recordService.save(USER_ID, draft.getId());
        assertThat(saved.getStatus()).isEqualTo(RecordStatus.SAVED);
        assertThat(saved.getRecordType()).isEqualTo(RecordType.MOMENT);
        assertDownloaded(image.attachment(), PNG_BYTES);

        UpdateRecordRequest update = new UpdateRecordRequest();
        update.setContent("synthetic cleanup anchor");
        update.setRecordType(RecordType.MOMENT);
        assertThat(recordService.update(USER_ID, draft.getId(), update).getStatus()).isEqualTo(RecordStatus.SAVED);
        attachmentService.deleteAttachment(USER_ID, draft.getId(), image.attachment().getId());
        assertThat(attachmentStatus(image.attachment().getId())).isEqualTo(RecordAttachmentStatus.DELETED.name());
        assertObjectAbsent(image.object());
    }

    private void probeVoiceOnly() throws Exception {
        byte[] wav = syntheticWav();
        RecordDetailVO draft = createBlankDraft();
        UploadResult voice = uploadAndCommit(
                draft.getId(), RecordAttachmentType.VOICE, "probe.wav", "audio/wav", wav, null, null, 1);
        RecordDetailVO saved = recordService.save(USER_ID, draft.getId());
        assertThat(saved.getStatus()).isEqualTo(RecordStatus.SAVED);
        assertPlayableWav(assertDownloaded(voice.attachment(), wav));

        UpdateRecordRequest update = new UpdateRecordRequest();
        update.setContent("synthetic cleanup anchor");
        update.setRecordType(RecordType.MOMENT);
        recordService.update(USER_ID, draft.getId(), update);
        attachmentService.deleteAttachment(USER_ID, draft.getId(), voice.attachment().getId());
        assertObjectAbsent(voice.object());
    }

    private void probePendingAndMissingObject() {
        RecordDetailVO pending = createBlankDraft();
        AttachmentUploadTokenVO pendingAuthorization = authorize(
                pending.getId(), RecordAttachmentType.IMAGE, "pending.png", "image/png", PNG_BYTES.length);
        assertThatThrownBy(() -> recordService.save(USER_ID, pending.getId()))
                .isInstanceOf(BizException.class);

        CommitRecordAttachmentRequest missingCommit = commitRequest(
                pendingAuthorization,
                RecordAttachmentType.IMAGE,
                "pending.png",
                "image/png",
                PNG_BYTES.length,
                1,
                1,
                null);
        assertThatThrownBy(() -> attachmentService.commitAttachment(USER_ID, pending.getId(), missingCommit))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> recordService.save(USER_ID, pending.getId()))
                .isInstanceOf(BizException.class);
    }

    private void probeExpiredDraftCleanup() throws Exception {
        RecordDetailVO successful = createBlankDraft();
        UploadResult successObject = uploadAndCommit(
                successful.getId(), RecordAttachmentType.IMAGE, "cleanup.png", "image/png", PNG_BYTES,
                1, 1, null);
        LocalDateTime successExpiry = markExpired(successful.getId());
        assertThat(cleanupWorker.cleanup(successful.getId(), USER_ID, successExpiry, now()))
                .isEqualTo(DraftCleanupResult.DELETED);
        assertRecordMissing(successful.getId());
        assertObjectAbsent(successObject.object());

        RecordDetailVO absent = createBlankDraft();
        UploadResult absentObject = uploadAndCommit(
                absent.getId(), RecordAttachmentType.IMAGE, "absent.png", "image/png", PNG_BYTES,
                1, 1, null);
        deleteObject(absentObject.object());
        assertObjectAbsent(absentObject.object());
        LocalDateTime absentExpiry = markExpired(absent.getId());
        assertThat(cleanupWorker.cleanup(absent.getId(), USER_ID, absentExpiry, now()))
                .isEqualTo(DraftCleanupResult.DELETED);
        assertRecordMissing(absent.getId());

        RecordDetailVO retry = createBlankDraft();
        UploadResult retryObject = uploadAndCommit(
                retry.getId(), RecordAttachmentType.IMAGE, "retry.png", "image/png", PNG_BYTES,
                1, 1, null);
        LocalDateTime retryExpiry = markExpired(retry.getId());
        storageProperties.getS3().setSecretKey("synthetic-invalid-secret");
        try {
            assertThat(cleanupWorker.cleanup(retry.getId(), USER_ID, retryExpiry, now()))
                    .isEqualTo(DraftCleanupResult.RETRY);
            assertRecordPresent(retry.getId());
        } finally {
            restoreStorageSecret();
        }
        assertThat(cleanupWorker.cleanup(retry.getId(), USER_ID, retryExpiry, now()))
                .isEqualTo(DraftCleanupResult.DELETED);
        assertRecordMissing(retry.getId());
        assertObjectAbsent(retryObject.object());
    }

    private RecordDetailVO createBlankDraft() {
        CreateRecordRequest request = new CreateRecordRequest();
        request.setContent("");
        request.setRecordType(RecordType.MOMENT);
        return recordService.create(USER_ID, request);
    }

    private UploadResult uploadAndCommit(
            Long recordId,
            RecordAttachmentType type,
            String fileName,
            String mimeType,
            byte[] bytes,
            Integer width,
            Integer height,
            Integer durationSeconds) throws Exception {
        AttachmentUploadTokenVO authorization = authorize(recordId, type, fileName, mimeType, bytes.length);
        UploadedObject object = upload(authorization, bytes);
        CommitRecordAttachmentRequest commit = commitRequest(
                authorization, type, fileName, mimeType, bytes.length, width, height, durationSeconds);
        RecordAttachmentVO attachment = attachmentService.commitAttachment(USER_ID, recordId, commit);
        assertThat(attachment.getStatus()).isEqualTo(RecordAttachmentStatus.AVAILABLE);
        return new UploadResult(attachment, object);
    }

    private AttachmentUploadTokenVO authorize(
            Long recordId,
            RecordAttachmentType type,
            String fileName,
            String mimeType,
            long sizeBytes) {
        CreateAttachmentUploadTokenRequest request = new CreateAttachmentUploadTokenRequest();
        request.setType(type);
        request.setFileName(fileName);
        request.setMimeType(mimeType);
        request.setSizeBytes(sizeBytes);
        return attachmentService.createUploadToken(USER_ID, recordId, request);
    }

    private UploadedObject upload(AttachmentUploadTokenVO authorization, byte[] bytes) throws Exception {
        assertThat(authorization.getUploadMethod()).isEqualTo("PUT");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(authorization.getUploadUrl()))
                .timeout(Duration.ofSeconds(30))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes));
        authorization.getUploadHeaders().forEach(builder::header);
        HttpResponse<Void> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isBetween(200, 299);
        UploadedObject object = new UploadedObject(
                StorageProvider.valueOf(authorization.getProvider()),
                authorization.getBucket(),
                authorization.getKey());
        uploadedObjects.add(object);
        return object;
    }

    private CommitRecordAttachmentRequest commitRequest(
            AttachmentUploadTokenVO authorization,
            RecordAttachmentType type,
            String fileName,
            String mimeType,
            long sizeBytes,
            Integer width,
            Integer height,
            Integer durationSeconds) {
        CommitRecordAttachmentRequest request = new CommitRecordAttachmentRequest();
        request.setProvider(StorageProvider.valueOf(authorization.getProvider()));
        request.setType(type);
        request.setKey(authorization.getKey());
        request.setFileName(fileName);
        request.setMimeType(mimeType);
        request.setSizeBytes(sizeBytes);
        request.setWidth(width);
        request.setHeight(height);
        request.setDurationSeconds(durationSeconds);
        return request;
    }

    private byte[] assertDownloaded(RecordAttachmentVO attachment, byte[] expected) throws Exception {
        AttachmentAccessUrlVO access = attachmentService.createAccessUrl(
                USER_ID, attachment.getRecordId(), attachment.getId());
        HttpRequest request = HttpRequest.newBuilder(URI.create(access.getUrl()))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).containsExactly(expected);
        return response.body();
    }

    private void assertPlayableWav(byte[] wav) throws Exception {
        try (AudioInputStream audio = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            assertThat(audio.getFormat().getSampleRate()).isEqualTo(8_000F);
            assertThat(audio.getFrameLength()).isPositive();
        }
    }

    private LocalDateTime markExpired(Long recordId) {
        LocalDateTime expiry = now().minusMinutes(5);
        int updated = jdbcTemplate.update(
                "UPDATE `record` SET draft_expires_at = ? WHERE id = ? AND user_id = ? AND status = 'DRAFT'",
                expiry,
                recordId,
                USER_ID);
        assertThat(updated).isOne();
        return expiry;
    }

    private void assertObjectAbsent(UploadedObject object) throws InterruptedException {
        ObjectStorageProvider storage = storageRegistry.getRequired(object.provider());
        for (int attempt = 0; attempt < 8; attempt++) {
            try {
                storage.statObject(object.bucket(), object.key());
            } catch (ObjectStorageException ex) {
                if (ex.isNotFound()) {
                    return;
                }
                throw ex;
            }
            Thread.sleep(250L);
        }
        throw new AssertionError("synthetic object still exists after cleanup");
    }

    private void deleteObject(UploadedObject object) {
        storageRegistry.getRequired(object.provider()).deleteObject(object.bucket(), object.key());
    }

    private void assertRecordMissing(Long recordId) {
        assertThat(recordCount(recordId)).isZero();
    }

    private void assertRecordPresent(Long recordId) {
        assertThat(recordCount(recordId)).isOne();
    }

    private int recordCount(Long recordId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE id = ? AND user_id = ?",
                Integer.class,
                recordId,
                USER_ID);
        return count == null ? 0 : count;
    }

    private String attachmentStatus(Long attachmentId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM record_attachment WHERE id = ? AND user_id = ?",
                String.class,
                attachmentId,
                USER_ID);
    }

    private void assertCleanStart() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id = ? OR username = ?",
                Integer.class,
                USER_ID,
                USERNAME);
        assertThat(count).as("stale P3.1 storage probe state must be handled manually").isZero();
        assertThat(storageRegistry.getActiveProvider().getProvider()).isEqualTo(StorageProvider.S3_COMPATIBLE);
    }

    private void insertSyntheticUser() {
        jdbcTemplate.update("""
                INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at)
                VALUES (?, ?, 'synthetic', 'P3.1 storage probe', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, USER_ID, USERNAME);
    }

    private void cleanupSyntheticState() {
        List<RuntimeException> failures = new ArrayList<>();
        for (UploadedObject object : uploadedObjects) {
            try {
                deleteObject(object);
                assertObjectAbsent(object);
            } catch (RuntimeException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                failures.add(new IllegalStateException("synthetic object cleanup failed", ex));
            }
        }
        if (!failures.isEmpty()) {
            IllegalStateException failure = new IllegalStateException(
                    "P3.1 storage probe cleanup incomplete; database retry anchors were retained");
            failures.forEach(failure::addSuppressed);
            throw failure;
        }
        jdbcTemplate.update("DELETE FROM `record` WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM `user` WHERE id = ? AND username = ?", USER_ID, USERNAME);
    }

    private void assertSyntheticStateRemoved() {
        Integer records = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE user_id = ?", Integer.class, USER_ID);
        Integer users = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id = ? OR username = ?", Integer.class, USER_ID, USERNAME);
        assertThat(records).isZero();
        assertThat(users).isZero();
    }

    private void restoreStorageSecret() {
        if (originalS3Secret != null) {
            storageProperties.getS3().setSecretKey(originalS3Secret);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }

    private byte[] syntheticWav() throws IOException {
        int sampleRate = 8_000;
        int sampleCount = sampleRate / 4;
        byte[] audio = new byte[sampleCount];
        ByteArrayOutputStream output = new ByteArrayOutputStream(44 + audio.length);
        output.write("RIFF".getBytes());
        output.write(littleEndian(36 + audio.length, 4));
        output.write("WAVEfmt ".getBytes());
        output.write(littleEndian(16, 4));
        output.write(littleEndian(1, 2));
        output.write(littleEndian(1, 2));
        output.write(littleEndian(sampleRate, 4));
        output.write(littleEndian(sampleRate, 4));
        output.write(littleEndian(1, 2));
        output.write(littleEndian(8, 2));
        output.write("data".getBytes());
        output.write(littleEndian(audio.length, 4));
        output.write(audio);
        return output.toByteArray();
    }

    private byte[] littleEndian(int value, int bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
        byte[] raw = buffer.array();
        byte[] result = new byte[bytes];
        System.arraycopy(raw, 0, result, 0, bytes);
        return result;
    }

    private record UploadedObject(StorageProvider provider, String bucket, String key) {
    }

    private record UploadResult(RecordAttachmentVO attachment, UploadedObject object) {
    }
}
