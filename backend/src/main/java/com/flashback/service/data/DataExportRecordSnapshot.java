package com.flashback.service.data;

import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentSession;
import com.flashback.domain.Record;
import com.flashback.domain.RecordAttachment;
import com.flashback.domain.RecordLocation;
import com.flashback.domain.Reply;
import com.flashback.domain.Tag;
import java.util.List;

public record DataExportRecordSnapshot(
        Record record,
        RecordLocation location,
        List<Tag> tags,
        Reply reply,
        List<AttachmentContent> attachments,
        List<AgentConversation> agentConversations) {
    public record AttachmentContent(RecordAttachment attachment, byte[] content) {}
    public record AgentConversation(AgentSession session, List<AgentMessage> messages) {}
}
