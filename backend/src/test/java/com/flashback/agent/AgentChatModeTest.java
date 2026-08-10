package com.flashback.agent;

import com.flashback.domain.RecordStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentChatModeTest {

    @Test
    void writingGuidanceShouldAllowDraftAndSavedOnly() {
        assertThat(AgentChatMode.WRITING_GUIDANCE.allowsRecordStatus(RecordStatus.DRAFT)).isTrue();
        assertThat(AgentChatMode.WRITING_GUIDANCE.allowsRecordStatus(RecordStatus.SAVED)).isTrue();
        assertThat(AgentChatMode.WRITING_GUIDANCE.allowsRecordStatus(RecordStatus.SEALED)).isFalse();
        assertThat(AgentChatMode.WRITING_GUIDANCE.allowsRecordStatus(RecordStatus.UNLOCKED)).isFalse();
    }

    @Test
    void reviewChatShouldStillAllowUnlockedOnly() {
        assertThat(AgentChatMode.REVIEW_CHAT.allowsRecordStatus(RecordStatus.UNLOCKED)).isTrue();
        assertThat(AgentChatMode.REVIEW_CHAT.allowsRecordStatus(RecordStatus.SAVED)).isFalse();
    }
}
