package com.flashback.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.config.AppStorageProperties;
import com.flashback.domain.DataDeletionScope;
import com.flashback.domain.DataOperationStatus;
import com.flashback.domain.RecordAttachmentStatus;
import com.flashback.domain.RecordAttachmentType;
import com.flashback.domain.RecordType;
import com.flashback.domain.SealedContentPolicy;
import com.flashback.domain.StorageProvider;
import com.flashback.dto.CommitRecordAttachmentRequest;
import com.flashback.dto.CreateAttachmentUploadTokenRequest;
import com.flashback.dto.CreateRecordRequest;
import com.flashback.schedule.DataOwnershipArtifactCleanupJob;
import com.flashback.service.DataOwnershipService;
import com.flashback.service.RecordAttachmentService;
import com.flashback.service.RecordService;
import com.flashback.service.data.DataOwnershipArtifactStore;
import com.flashback.service.impl.DataOwnershipServiceImpl;
import com.flashback.vo.AttachmentUploadTokenVO;
import com.flashback.vo.DataOperationVO;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** P3.2 Gate 3b：真实私有对象存储的导出、删除、恢复与最终清理探针。 */
@EnabledIfEnvironmentVariable(named = "P32_STORAGE_PROBE", matches = "1")
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "app.ai.provider=mock",
        "app.record.unlock-job-cron=-",
        "app.record.draft-cleanup-cron=-",
        "app.data-ownership.recovery-delay-ms=3600000",
        "app.data-ownership.cleanup-delay-ms=3600000",
        "logging.level.com.flashback=INFO"
})
@ActiveProfiles("dev")
class P32RealDataOwnershipStorageProbeTest {
    private static final long USER_ID = 9_932_500L;
    private static final String USERNAME = "p32-storage-probe";
    private static final long SESSION_ID = 9_932_501L;
    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl9Z1sAAAAASUVORK5CYII=");

    @Autowired private RecordService recordService;
    @Autowired private RecordAttachmentService attachmentService;
    @Autowired private DataOwnershipService ownershipService;
    @Autowired private DataOwnershipServiceImpl ownershipServiceImpl;
    @Autowired private DataOwnershipArtifactCleanupJob artifactCleanupJob;
    @Autowired private DataOwnershipArtifactStore artifactStore;
    @Autowired private ObjectStorageRegistry storageRegistry;
    @Autowired private AppStorageProperties storageProperties;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;

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
        registry.add("app.data-ownership.artifact-directory", () -> Path.of(
                System.getProperty("java.io.tmpdir"), "flashback-p32-storage-probe").toString());
    }

    @Test
    void realStorageMustExportDeleteRecoverAndCleanSyntheticArtifacts() throws Exception {
        assertCleanStart();
        originalS3Secret = storageProperties.getS3().getSecretKey();
        insertSyntheticUser();
        try {
            probeExportReadFailureRetryIntegrityAndExpiry();
            probeDeleteSuccess();
            probeNotFoundAfterRestart();
            probeDeleteFailureAndRetry();
            System.out.println("P32STORAGE PASS export=true mediaHash=true readRetry=true artifactExpiry=true "
                    + "delete=true notFoundRecovery=true deleteRetry=true");
        } finally {
            restoreStorageSecret();
            cleanupSyntheticState();
        }
        assertSyntheticStateRemoved();
        System.out.println("P32STORAGE CLEANUP objects=removed artifacts=removed database=removed");
    }

    private void probeExportReadFailureRetryIntegrityAndExpiry() throws Exception {
        byte[] wav = syntheticWav();
        RecordDetailVO record = createDraft("synthetic export");
        UploadResult image = uploadAndCommit(
                record.getId(), RecordAttachmentType.IMAGE, "probe.png", "image/png", PNG_BYTES, 1, 1, null);
        UploadResult voice = uploadAndCommit(
                record.getId(), RecordAttachmentType.VOICE, "probe.wav", "audio/wav", wav, null, null, 1);
        recordService.save(USER_ID, record.getId());
        insertSyntheticAgentSession(record.getId());

        storageProperties.getS3().setSecretKey("synthetic-invalid-secret");
        DataOperationVO failed = ownershipService.createExport(USER_ID, SealedContentPolicy.FULL_CONTENT);
        assertThat(failed.getStatus()).isEqualTo(DataOperationStatus.RETRY_REQUIRED);
        assertThat(failed.getFailureCode()).isNotNull();
        assertNoPartialArtifacts();

        restoreStorageSecret();
        DataOperationVO completed = ownershipService.retry(USER_ID, failed.getId());
        assertThat(completed.getStatus()).isEqualTo(DataOperationStatus.SUCCEEDED);
        assertThat(completed.isDownloadable()).isTrue();
        byte[] zip = ownershipService.downloadExport(USER_ID, completed.getId());
        Map<String, byte[]> entries = unzip(zip);
        assertThat(entries).containsKeys(
                "flashback-export/index.html",
                "flashback-export/README.txt",
                "flashback-export/manifest.json");
        assertThat(entries.keySet()).anyMatch(path -> path.startsWith("flashback-export/records/"));
        assertThat(entries.keySet()).anyMatch(path -> path.startsWith("flashback-export/agent/"));
        assertMediaAndManifest(entries, image.attachment(), PNG_BYTES);
        assertMediaAndManifest(entries, voice.attachment(), wav);

        String token = jdbc.queryForObject(
                "SELECT artifact_token FROM data_operation WHERE id=?", String.class, completed.getId());
        assertThat(artifactStore.read(token)).containsExactly(zip);
        jdbc.update("UPDATE data_operation SET artifact_expires_at=DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE id=?",
                completed.getId());
        artifactCleanupJob.cleanup();
        assertThat(ownershipService.getOperation(USER_ID, completed.getId()).getStatus())
                .isEqualTo(DataOperationStatus.EXPIRED);
        assertThatThrownBy(() -> artifactStore.read(token)).isInstanceOf(IOException.class);
    }

    private void probeDeleteSuccess() throws Exception {
        DataOperationVO intent = ownershipService.prepareDeletion(USER_ID, DataDeletionScope.ALL_RECORDS, null);
        assertThat(intent.getTotalItems()).isEqualTo(1);
        DataOperationVO completed = ownershipService.confirmDeletion(
                USER_ID, intent.getId(), intent.getConfirmationText());
        assertThat(completed.getStatus()).isEqualTo(DataOperationStatus.SUCCEEDED);
        assertThat(recordCount()).isZero();
        for (UploadedObject object : uploadedObjects) {
            assertObjectAbsent(object);
        }
    }

    private void probeNotFoundAfterRestart() throws Exception {
        RecordDetailVO record = createDraft("synthetic not-found recovery");
        UploadResult uploaded = uploadAndCommit(
                record.getId(), RecordAttachmentType.IMAGE, "absent.png", "image/png", PNG_BYTES, 1, 1, null);
        DataOperationVO intent = ownershipService.prepareDeletion(
                USER_ID, DataDeletionScope.RECORD, record.getId());
        deleteObject(uploaded.object());
        assertObjectAbsent(uploaded.object());
        jdbc.update("UPDATE data_operation SET status='RUNNING', confirmed_at=NOW(), started_at=NOW(), "
                        + "updated_at=DATE_SUB(NOW(), INTERVAL 30 MINUTE) WHERE id=? AND status='PREPARED'",
                intent.getId());
        ownershipServiceImpl.resumeStaleOperations();
        assertThat(ownershipService.getOperation(USER_ID, intent.getId()).getStatus())
                .isEqualTo(DataOperationStatus.SUCCEEDED);
        assertThat(recordCount(record.getId())).isZero();
    }

    private void probeDeleteFailureAndRetry() throws Exception {
        RecordDetailVO record = createDraft("synthetic retry");
        UploadResult uploaded = uploadAndCommit(
                record.getId(), RecordAttachmentType.IMAGE, "retry.png", "image/png", PNG_BYTES, 1, 1, null);
        DataOperationVO intent = ownershipService.prepareDeletion(
                USER_ID, DataDeletionScope.RECORD, record.getId());
        storageProperties.getS3().setSecretKey("synthetic-invalid-secret");
        DataOperationVO failed = ownershipService.confirmDeletion(
                USER_ID, intent.getId(), intent.getConfirmationText());
        assertThat(failed.getStatus()).isEqualTo(DataOperationStatus.RETRY_REQUIRED);
        assertThat(failed.getFailedItems()).isOne();
        assertThat(recordCount(record.getId())).isOne();

        restoreStorageSecret();
        DataOperationVO completed = ownershipService.retry(USER_ID, failed.getId());
        assertThat(completed.getStatus()).isEqualTo(DataOperationStatus.SUCCEEDED);
        assertThat(recordCount(record.getId())).isZero();
        assertObjectAbsent(uploaded.object());
    }

    private RecordDetailVO createDraft(String syntheticContent) {
        CreateRecordRequest request = new CreateRecordRequest();
        request.setContent(syntheticContent);
        request.setRecordType(RecordType.MOMENT);
        return recordService.create(USER_ID, request);
    }

    private UploadResult uploadAndCommit(Long recordId, RecordAttachmentType type, String fileName,
            String mimeType, byte[] bytes, Integer width, Integer height, Integer durationSeconds) throws Exception {
        CreateAttachmentUploadTokenRequest uploadRequest = new CreateAttachmentUploadTokenRequest();
        uploadRequest.setType(type);
        uploadRequest.setFileName(fileName);
        uploadRequest.setMimeType(mimeType);
        uploadRequest.setSizeBytes((long) bytes.length);
        AttachmentUploadTokenVO authorization = attachmentService.createUploadToken(USER_ID, recordId, uploadRequest);
        UploadedObject object = upload(authorization, bytes);

        CommitRecordAttachmentRequest commit = new CommitRecordAttachmentRequest();
        commit.setProvider(StorageProvider.valueOf(authorization.getProvider()));
        commit.setType(type);
        commit.setKey(authorization.getKey());
        commit.setFileName(fileName);
        commit.setMimeType(mimeType);
        commit.setSizeBytes((long) bytes.length);
        commit.setWidth(width);
        commit.setHeight(height);
        commit.setDurationSeconds(durationSeconds);
        RecordAttachmentVO attachment = attachmentService.commitAttachment(USER_ID, recordId, commit);
        assertThat(attachment.getStatus()).isEqualTo(RecordAttachmentStatus.AVAILABLE);
        return new UploadResult(attachment, object);
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
                StorageProvider.valueOf(authorization.getProvider()), authorization.getBucket(), authorization.getKey());
        uploadedObjects.add(object);
        return object;
    }

    private void assertMediaAndManifest(
            Map<String, byte[]> entries, RecordAttachmentVO attachment, byte[] expected) throws Exception {
        String suffix = attachment.getId() + "-" + attachment.getFileName();
        String path = entries.keySet().stream()
                .filter(name -> name.startsWith("flashback-export/media/") && name.endsWith(suffix))
                .findFirst().orElseThrow();
        assertThat(entries.get(path)).containsExactly(expected);

        JsonNode manifest = objectMapper.readTree(entries.get("flashback-export/manifest.json"));
        String relativePath = path.substring("flashback-export/".length());
        JsonNode item = null;
        for (JsonNode candidate : manifest.path("files")) {
            if (relativePath.equals(candidate.path("path").asText())) {
                item = candidate;
                break;
            }
        }
        assertThat(item).isNotNull();
        assertThat(item.path("bytes").asLong()).isEqualTo(expected.length);
        assertThat(item.path("sha256").asText()).isEqualTo(sha256(expected));
    }

    private Map<String, byte[]> unzip(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                entries.put(entry.getName(), input.readAllBytes());
                input.closeEntry();
            }
        }
        return entries;
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private void insertSyntheticAgentSession(Long recordId) {
        LocalDateTime now = now();
        jdbc.update("INSERT INTO agent_session(id,user_id,record_id,purpose,stage,status,turn_count,stage_reask_count,last_active_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                SESSION_ID, USER_ID, recordId, "WRITING_GUIDANCE", "OPENING", "ACTIVE", 0, 0, now, now, now);
        jdbc.update("INSERT INTO agent_message(session_id,user_id,role,turn_no,stage,content,created_at) VALUES (?,?,?,?,?,?,?)",
                SESSION_ID, USER_ID, "ASSISTANT", 1, "OPENING", "synthetic agent output", now);
    }

    private void assertNoPartialArtifacts() throws IOException {
        Path directory = artifactDirectory();
        if (!Files.exists(directory)) return;
        try (var files = Files.list(directory)) {
            assertThat(files.noneMatch(path -> path.getFileName().toString().endsWith(".partial"))).isTrue();
        }
    }

    private void assertObjectAbsent(UploadedObject object) throws InterruptedException {
        ObjectStorageProvider storage = storageRegistry.getRequired(object.provider());
        for (int attempt = 0; attempt < 8; attempt++) {
            try {
                storage.statObject(object.bucket(), object.key());
            } catch (ObjectStorageException ex) {
                if (ex.isNotFound()) return;
                throw ex;
            }
            Thread.sleep(250L);
        }
        throw new AssertionError("synthetic object still exists after cleanup");
    }

    private void deleteObject(UploadedObject object) {
        storageRegistry.getRequired(object.provider()).deleteObject(object.bucket(), object.key());
    }

    private int recordCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM `record` WHERE user_id=?", Integer.class, USER_ID);
    }

    private int recordCount(Long recordId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE id=? AND user_id=?", Integer.class, recordId, USER_ID);
    }

    private void assertCleanStart() throws IOException {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id=? OR username=?", Integer.class, USER_ID, USERNAME);
        assertThat(count).as("stale P3.2 storage probe state must be handled manually").isZero();
        assertThat(storageRegistry.getActiveProvider().getProvider()).isEqualTo(StorageProvider.S3_COMPATIBLE);
        Path directory = artifactDirectory();
        if (Files.exists(directory)) {
            try (var files = Files.list(directory)) {
                assertThat(files.findAny()).as("stale P3.2 artifact must be handled manually").isEmpty();
            }
        }
    }

    private void insertSyntheticUser() {
        jdbc.update("INSERT INTO `user` (id, username, password_hash, nickname, status, created_at, updated_at) "
                        + "VALUES (?, ?, 'synthetic', 'P3.2 storage probe', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                USER_ID, USERNAME);
    }

    private void cleanupSyntheticState() throws Exception {
        List<Exception> failures = new ArrayList<>();
        for (UploadedObject object : uploadedObjects) {
            try {
                try {
                    deleteObject(object);
                } catch (ObjectStorageException ex) {
                    if (!ex.isNotFound()) throw ex;
                }
                assertObjectAbsent(object);
            } catch (Exception ex) {
                failures.add(new IllegalStateException("synthetic object cleanup failed", ex));
            }
        }
        for (String token : jdbc.queryForList(
                "SELECT artifact_token FROM data_operation WHERE user_id=? AND artifact_token IS NOT NULL",
                String.class, USER_ID)) {
            artifactStore.delete(token);
        }
        assertNoPartialArtifacts();
        if (!failures.isEmpty()) {
            IllegalStateException failure = new IllegalStateException(
                    "P3.2 storage cleanup incomplete; database retry anchors were retained");
            failures.forEach(failure::addSuppressed);
            throw failure;
        }
        jdbc.update("DELETE FROM data_operation WHERE user_id=?", USER_ID);
        jdbc.update("DELETE FROM `user` WHERE id=? AND username=?", USER_ID, USERNAME);
        Files.deleteIfExists(artifactDirectory());
    }

    private void assertSyntheticStateRemoved() {
        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE id=? OR username=?", Integer.class, USER_ID, USERNAME);
        Integer records = jdbc.queryForObject(
                "SELECT COUNT(*) FROM `record` WHERE user_id=?", Integer.class, USER_ID);
        Integer operations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM data_operation WHERE user_id=?", Integer.class, USER_ID);
        assertThat(users).isZero();
        assertThat(records).isZero();
        assertThat(operations).isZero();
        assertThat(Files.exists(artifactDirectory())).isFalse();
    }

    private void restoreStorageSecret() {
        if (originalS3Secret != null) storageProperties.getS3().setSecretKey(originalS3Secret);
    }

    private Path artifactDirectory() {
        return Path.of(System.getProperty("java.io.tmpdir"), "flashback-p32-storage-probe");
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
