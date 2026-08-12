package com.flashback.service.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.Record;
import com.flashback.domain.RecordStatus;
import com.flashback.domain.SealedContentPolicy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class DataExportPackageBuilder {
    private static final String ROOT = "flashback-export/";
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DataExportPackageBuilder(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public byte[] build(List<DataExportRecordSnapshot> snapshots, SealedContentPolicy policy) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            List<Map<String, Object>> files = new ArrayList<>();
            int mediaCount = 0;
            int agentConversationCount = 0;
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                addText(zip, files, "index.html", index(snapshots, policy));
                addText(zip, files, "README.txt", readme(policy));
                for (DataExportRecordSnapshot snapshot : snapshots) {
                    Record record = snapshot.record();
                    boolean hidden = policy == SealedContentPolicy.RESPECT_SEAL && record.getStatus() == RecordStatus.SEALED;
                    addText(zip, files, "records/record-" + record.getId() + ".md", recordMarkdown(snapshot, hidden));
                    if (!hidden) {
                        for (DataExportRecordSnapshot.AttachmentContent item : snapshot.attachments()) {
                            String name = "media/record-" + record.getId() + "/" + item.attachment().getId()
                                    + "-" + safeName(item.attachment().getFileName());
                            addBytes(zip, files, name, item.content(), item.attachment().getMimeType(), "record:" + record.getId());
                            mediaCount++;
                        }
                        for (DataExportRecordSnapshot.AgentConversation conversation : snapshot.agentConversations()) {
                            String name = "agent/record-" + record.getId() + "-session-" + conversation.session().getId() + ".md";
                            addText(zip, files, name, agentMarkdown(conversation));
                            agentConversationCount++;
                        }
                    }
                }
                Map<String, Object> manifest = new LinkedHashMap<>();
                manifest.put("schemaVersion", "1.0");
                manifest.put("generatedAt", LocalDateTime.now(clock).toString());
                manifest.put("sealedContentPolicy", policy.name());
                manifest.put("recordCount", snapshots.size());
                manifest.put("mediaCount", mediaCount);
                manifest.put("agentConversationCount", agentConversationCount);
                manifest.put("files", files);
                byte[] manifestBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
                put(zip, "manifest.json", manifestBytes);
            }
            return bytes.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("export package build failed", ex);
        }
    }

    private String recordMarkdown(DataExportRecordSnapshot snapshot, boolean hidden) {
        Record r = snapshot.record();
        StringBuilder out = new StringBuilder("# ").append(hidden ? "封存记录 " + r.getId() : value(r.getTitle(), "未命名记录")).append("\n\n")
                .append("- 状态：").append(r.getStatus()).append("\n")
                .append("- 创建时间：").append(r.getCreatedAt()).append("\n")
                .append("- 封存时间：").append(r.getSealedAt()).append("\n")
                .append("- 预计解锁：").append(r.getUnlockAt()).append("\n\n");
        if (hidden) return out.append("> 此记录仍在封存中。本次导出选择了“尊重封存”，正文和关联内容未包含。\n").toString();
        out.append("## 用户原文\n\n").append(value(r.getContent(), "（未填写）")).append("\n\n")
                .append("## 用户填写的上下文\n\n")
                .append("- 核心问题：").append(value(r.getCoreQuestion(), "（未填写）")).append("\n")
                .append("- 当时相信：").append(value(r.getBeliefThen(), "（未填写）")).append("\n")
                .append("- 后来现实：").append(value(r.getRealityLater(), "（未填写）")).append("\n")
                .append("- 标签：").append(snapshot.tags().stream().map(t -> t.getName()).toList()).append("\n");
        if (snapshot.location() != null) out.append("- 位置：").append(value(snapshot.location().getName(), "（未命名）")).append("\n");
        out.append("\n## AI 派生字段\n\n- 摘要：").append(value(r.getAiSummary(), "（无）"))
                .append("\n- 提示结果：").append(value(r.getAiPromptResult(), "（无）")).append("\n");
        if (snapshot.reply() != null) out.append("\n## 回信\n\n").append(value(snapshot.reply().getContent(), "（无）")).append("\n");
        return out.toString();
    }

    private String agentMarkdown(DataExportRecordSnapshot.AgentConversation conversation) {
        StringBuilder out = new StringBuilder("# Agent 会话\n\n")
                .append("- 用途：").append(conversation.session().getPurpose()).append("\n")
                .append("- 创建时间：").append(conversation.session().getCreatedAt()).append("\n\n");
        for (AgentMessage message : conversation.messages()) {
            out.append("## ").append(message.getRole()).append(" · ").append(message.getCreatedAt()).append("\n\n")
                    .append(value(message.getContent(), "（空）")).append("\n\n");
        }
        return out.toString();
    }

    private String index(List<DataExportRecordSnapshot> snapshots, SealedContentPolicy policy) {
        StringBuilder links = new StringBuilder();
        for (DataExportRecordSnapshot snapshot : snapshots) {
            links.append("<li><a href=\"records/record-").append(snapshot.record().getId()).append(".md\">")
                    .append(escapeHtml(policy == SealedContentPolicy.RESPECT_SEAL && snapshot.record().getStatus() == RecordStatus.SEALED
                            ? "封存记录 " + snapshot.record().getId() : value(snapshot.record().getTitle(), "未命名记录"))).append("</a></li>");
        }
        return "<!doctype html><meta charset=\"utf-8\"><title>时光回序数据副本</title>"
                + "<style>body{max-width:760px;margin:48px auto;padding:0 20px;font-family:system-ui;line-height:1.7;color:#2d2926}a{color:#76584b}</style>"
                + "<h1>时光回序数据副本</h1><p>这是可离线阅读的数据副本。记录正文使用 Markdown 保存。</p><ul>" + links + "</ul>";
    }

    private String readme(SealedContentPolicy policy) {
        return "时光回序数据副本\n\n封存策略：" + policy + "\nDRAFT 表示尚未完成的记录。\n"
                + "records 保存用户记录，agent 单独保存用户可见的 Agent 会话，media 保存附件原始字节。\n"
                + "本包不包含账号凭据、内部提示词、provider 原始响应、工具瞬态参数、运行日志或运维数据。\n";
    }

    private void addText(ZipOutputStream zip, List<Map<String, Object>> files, String path, String text) throws Exception {
        addBytes(zip, files, path, text.getBytes(StandardCharsets.UTF_8), "text/plain; charset=utf-8", "export");
    }

    private void addBytes(ZipOutputStream zip, List<Map<String, Object>> files, String path, byte[] content, String type, String owner) throws Exception {
        put(zip, path, content);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("path", path); item.put("bytes", content.length); item.put("sha256", sha256(content));
        item.put("contentType", value(type, "application/octet-stream")); item.put("owner", owner);
        files.add(item);
    }

    private void put(ZipOutputStream zip, String path, byte[] content) throws Exception {
        zip.putNextEntry(new ZipEntry(ROOT + path)); zip.write(content); zip.closeEntry();
    }
    private String sha256(byte[] content) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)); }
    private String safeName(String name) { String v = value(name, "attachment.bin").replaceAll("[^a-zA-Z0-9._-]", "_"); return v.isBlank() ? "attachment.bin" : v; }
    private String value(Object value, String fallback) { return value == null || value.toString().isBlank() ? fallback : value.toString(); }
    private String escapeHtml(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
