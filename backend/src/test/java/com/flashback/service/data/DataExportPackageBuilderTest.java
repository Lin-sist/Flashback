package com.flashback.service.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.domain.*;
import com.flashback.domain.Record;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;

class DataExportPackageBuilderTest {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final DataExportPackageBuilder builder = new DataExportPackageBuilder(mapper,
            Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void fullContentBuildsOfflinePackageAndVerifiableManifest() throws Exception {
        Record record = record(1L, RecordStatus.SAVED, "属于用户的正文");
        RecordAttachment attachment = new RecordAttachment();
        attachment.setId(11L); attachment.setRecordId(1L); attachment.setFileName("photo.jpg"); attachment.setMimeType("image/jpeg");
        AgentSession session = new AgentSession(); session.setId(21L); session.setRecordId(1L); session.setCreatedAt(LocalDateTime.of(2026, 8, 12, 8, 0));
        AgentMessage message = new AgentMessage(); message.setRole(AgentMessageRole.ASSISTANT); message.setContent("这是 Agent 内容，不是用户原话");
        byte[] media = "synthetic-media".getBytes(StandardCharsets.UTF_8);
        DataExportRecordSnapshot snapshot = new DataExportRecordSnapshot(record, null, List.of(), null,
                List.of(new DataExportRecordSnapshot.AttachmentContent(attachment, media)),
                List.of(new DataExportRecordSnapshot.AgentConversation(session, List.of(message))));

        Map<String, byte[]> entries = unzip(builder.build(List.of(snapshot), SealedContentPolicy.FULL_CONTENT));

        assertTrue(entries.containsKey("flashback-export/index.html"));
        assertTrue(entries.containsKey("flashback-export/records/record-1.md"));
        assertTrue(entries.containsKey("flashback-export/media/record-1/11-photo.jpg"));
        assertTrue(entries.containsKey("flashback-export/agent/record-1-session-21.md"));
        assertTrue(entries.containsKey("flashback-export/manifest.json"));
        assertTrue(entries.containsKey("flashback-export/README.txt"));
        String html = text(entries, "flashback-export/index.html").toLowerCase();
        assertFalse(html.contains("http:")); assertFalse(html.contains("https:")); assertFalse(html.contains("fetch(")); assertFalse(html.contains("cdn"));
        assertTrue(text(entries, "flashback-export/records/record-1.md").contains("属于用户的正文"));
        assertTrue(text(entries, "flashback-export/agent/record-1-session-21.md").contains("这是 Agent 内容，不是用户原话"));
        JsonNode manifest = mapper.readTree(entries.get("flashback-export/manifest.json"));
        JsonNode mediaEntry = java.util.stream.StreamSupport.stream(manifest.path("files").spliterator(), false)
                .filter(node -> node.path("path").asText().startsWith("media/")).findFirst().orElseThrow();
        assertEquals(media.length, mediaEntry.path("bytes").asInt());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(media)), mediaEntry.path("sha256").asText());
    }

    @Test
    void respectSealOmitsSealedContentMediaAndAgent() throws Exception {
        Record record = record(2L, RecordStatus.SEALED, "尚未到达的正文");
        RecordAttachment attachment = new RecordAttachment(); attachment.setId(12L); attachment.setFileName("secret.wav");
        AgentSession session = new AgentSession(); session.setId(22L);
        DataExportRecordSnapshot snapshot = new DataExportRecordSnapshot(record, null, List.of(), null,
                List.of(new DataExportRecordSnapshot.AttachmentContent(attachment, new byte[]{1})),
                List.of(new DataExportRecordSnapshot.AgentConversation(session, List.of())));
        Map<String, byte[]> entries = unzip(builder.build(List.of(snapshot), SealedContentPolicy.RESPECT_SEAL));
        String markdown = text(entries, "flashback-export/records/record-2.md");
        assertFalse(markdown.contains("尚未到达的正文")); assertTrue(markdown.contains("尊重封存"));
        assertTrue(entries.keySet().stream().noneMatch(name -> name.contains("secret.wav") || name.contains("session-22")));
    }

    @Test
    void buildsSyntheticMaximumCountNearTotalSizeBoundaryWithoutSlaClaim() throws Exception {
        Record record = record(3L, RecordStatus.SAVED, "合成边界记录");
        byte[] sharedSixteenMiB = new byte[16 * 1024 * 1024];
        List<DataExportRecordSnapshot.AttachmentContent> media = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            RecordAttachment attachment = new RecordAttachment();
            attachment.setId(100L + i); attachment.setFileName("boundary-" + i + ".bin"); attachment.setMimeType("application/octet-stream");
            media.add(new DataExportRecordSnapshot.AttachmentContent(attachment, sharedSixteenMiB));
        }
        long started = System.nanoTime();
        byte[] zip = builder.build(List.of(new DataExportRecordSnapshot(record, null, List.of(), null, media, List.of())), SealedContentPolicy.FULL_CONTENT);
        long durationMs = (System.nanoTime() - started) / 1_000_000;
        Map<String, byte[]> entries = unzip(zip);
        assertEquals(18, entries.keySet().stream().filter(name -> name.startsWith("flashback-export/media/")).count());
        System.out.printf("P3.2 synthetic-export count=18 logicalBytes=%d artifactBytes=%d durationMs=%d%n",
                18L * sharedSixteenMiB.length, zip.length, durationMs);
    }

    private Record record(Long id, RecordStatus status, String content) {
        Record record = new Record(); record.setId(id); record.setUserId(7L); record.setTitle("一条记录");
        record.setContent(content); record.setStatus(status); record.setRecordType(RecordType.MOMENT); record.setCreatedAt(LocalDateTime.of(2026, 8, 12, 8, 0)); return record;
    }
    private Map<String, byte[]> unzip(byte[] bytes) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry; while ((entry = zip.getNextEntry()) != null) entries.put(entry.getName(), zip.readAllBytes());
        }
        return entries;
    }
    private String text(Map<String, byte[]> entries, String path) { return new String(entries.get(path), StandardCharsets.UTF_8); }
}
