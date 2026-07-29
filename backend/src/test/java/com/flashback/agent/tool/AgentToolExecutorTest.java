package com.flashback.agent.tool;

import com.flashback.agent.guardrail.AgentContentChecker;
import com.flashback.agent.guardrail.AgentFaithfulnessChecker;
import com.flashback.agent.guardrail.AgentTimeAttributionChecker;
import com.flashback.common.error.ErrorCode;
import com.flashback.common.exception.BizException;
import com.flashback.common.exception.NotFoundException;
import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentToolCall;
import com.flashback.service.RecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 工具执行测试（C2）。
 *
 * 覆盖重点：
 * - 执行必须经 RecordService（继承归属与草稿校验），不存在旁路；
 * - 封存后执行被拒且显式失败，不谎报成功；
 * - 不可逆操作不可执行。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgentToolExecutorTest {

    private static final Long USER_ID = 4001L;
    private static final Long RECORD_ID = 7001L;

    @Mock
    private RecordService recordService;

    private AgentToolExecutor executor;

    @BeforeEach
    void setUp() {
        AppAgentProperties properties = new AppAgentProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-07-27T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        // C4：validator 新增护栏依赖；执行层本身不做忠实度判定（判定已在提议校验阶段完成）。
        AgentFaithfulnessChecker faithfulnessChecker = new AgentFaithfulnessChecker(properties);
        AgentToolValidator validator = new AgentToolValidator(
                new AgentToolRegistry(),
                properties,
                faithfulnessChecker,
                new AgentContentChecker(properties, faithfulnessChecker),
                // C3：新增时间归属检查依赖；执行层不涉及记忆层。
                new AgentTimeAttributionChecker(properties),
                clock);
        executor = new AgentToolExecutor(recordService, validator);
    }

    // ---------- 正常执行经业务层 ----------

    @Test
    void shouldAppendContentThroughRecordService() {
        AgentToolOutcome outcome = executor.execute(
                toolCall(AgentToolName.APPEND_RECORD_CONTENT),
                AgentToolProposal.appendContent("好吗？", "撑不住"));

        assertThat(outcome.isExecuted()).isTrue();
        verify(recordService).appendContent(USER_ID, RECORD_ID, "撑不住");
    }

    @Test
    void shouldAppendTagsThroughRecordService() {
        AgentToolOutcome outcome = executor.execute(
                toolCall(AgentToolName.ADD_RECORD_TAGS),
                AgentToolProposal.addTags("加标签？", List.of(3L, 4L)));

        assertThat(outcome.isExecuted()).isTrue();
        verify(recordService).appendTags(eq(USER_ID), eq(RECORD_ID), eq(List.of(3L, 4L)));
    }

    @Test
    void shouldUpdateUnlockAtWithoutSealing() {
        AgentToolOutcome outcome = executor.execute(
                toolCall(AgentToolName.PROPOSE_UNLOCK_AT),
                AgentToolProposal.proposeUnlockAt("留到明年？", "2027-01-01T09:00:00"));

        assertThat(outcome.isExecuted()).isTrue();
        verify(recordService).updateUnlockAt(USER_ID, RECORD_ID, LocalDateTime.parse("2027-01-01T09:00:00"));
        // 设置解锁时间不得触发封存。
        verify(recordService, never()).seal(anyLong(), anyLong());
    }

    /**
     * 执行永远不触碰不可逆操作：即便走到执行层也没有对应分支。
     */
    @Test
    void shouldNeverInvokeIrreversibleRecordOperations() {
        executor.execute(
                toolCall(AgentToolName.APPEND_RECORD_CONTENT),
                AgentToolProposal.appendContent("好吗？", "撑不住"));

        verify(recordService, never()).seal(anyLong(), anyLong());
        verify(recordService, never()).delete(anyLong(), anyLong());
        verify(recordService, never()).updateLocation(anyLong(), anyLong(), any());
        verify(recordService, never()).deleteLocation(anyLong(), anyLong());
        verify(recordService, never()).updateCover(anyLong(), anyLong(), any());
        verify(recordService, never()).updateLaterReflection(anyLong(), anyLong(), any());
        verify(recordService, never()).update(anyLong(), anyLong(), any());
    }

    @Test
    void shouldOnlyExposeWriteToolsAsExecutable() {
        assertThat(executor.executableToolNames()).containsExactlyInAnyOrder(
                "append_record_content", "add_record_tags", "propose_unlock_at");
    }

    // ---------- 失败显式 ----------

    /**
     * 记录在提议之后被封存：执行被业务层拒绝，返回显式失败而非成功。
     */
    @Test
    void shouldFailExplicitlyWhenRecordSealed() {
        when(recordService.appendContent(anyLong(), anyLong(), anyString()))
                .thenThrow(new BizException(ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST, "记录已封存，不能追加正文"));

        AgentToolOutcome outcome = executor.execute(
                toolCall(AgentToolName.APPEND_RECORD_CONTENT),
                AgentToolProposal.appendContent("好吗？", "撑不住"));

        assertThat(outcome.status()).isEqualTo(AgentToolCallStatus.FAILED);
        assertThat(outcome.failureType()).isEqualTo(AgentToolOutcome.FAILURE_BUSINESS_REJECTED);
        assertThat(outcome.message()).isEqualTo("记录已封存，不能追加正文");
        assertThat(outcome.isExecuted()).isFalse();
    }

    /**
     * 跨用户：service 层抛未找到，执行显式失败。
     */
    @Test
    void shouldFailExplicitlyWhenRecordNotOwned() {
        when(recordService.appendTags(anyLong(), anyLong(), any()))
                .thenThrow(new NotFoundException("记录不存在"));

        AgentToolOutcome outcome = executor.execute(
                toolCall(AgentToolName.ADD_RECORD_TAGS),
                AgentToolProposal.addTags("加标签？", List.of(1L)));

        assertThat(outcome.status()).isEqualTo(AgentToolCallStatus.FAILED);
        assertThat(outcome.failureType()).isEqualTo(AgentToolOutcome.FAILURE_BUSINESS_REJECTED);
    }

    @Test
    void shouldFailExplicitlyOnUnexpectedError() {
        when(recordService.appendContent(anyLong(), anyLong(), anyString()))
                .thenThrow(new IllegalStateException("boom"));

        AgentToolOutcome outcome = executor.execute(
                toolCall(AgentToolName.APPEND_RECORD_CONTENT),
                AgentToolProposal.appendContent("好吗？", "撑不住"));

        assertThat(outcome.status()).isEqualTo(AgentToolCallStatus.FAILED);
        assertThat(outcome.failureType()).isEqualTo(AgentToolOutcome.FAILURE_UNEXPECTED);
    }

    @Test
    void shouldFailWhenSessionHasNoRecord() {
        AgentToolCall toolCall = toolCall(AgentToolName.APPEND_RECORD_CONTENT);
        toolCall.setRecordId(null);

        AgentToolOutcome outcome = executor.execute(
                toolCall, AgentToolProposal.appendContent("好吗？", "撑不住"));

        assertThat(outcome.failureType()).isEqualTo(AgentToolOutcome.FAILURE_PRECONDITION);
        verifyNoInteractions(recordService);
    }

    @Test
    void shouldFailWhenProposalLost() {
        AgentToolOutcome outcome = executor.execute(toolCall(AgentToolName.APPEND_RECORD_CONTENT), null);

        assertThat(outcome.failureType()).isEqualTo(AgentToolOutcome.FAILURE_PRECONDITION);
        verifyNoInteractions(recordService);
    }

    @Test
    void shouldFailWhenUnlockAtUnparsable() {
        AgentToolOutcome outcome = executor.execute(
                toolCall(AgentToolName.PROPOSE_UNLOCK_AT),
                AgentToolProposal.proposeUnlockAt("留到某天？", "明年今天"));

        assertThat(outcome.status()).isEqualTo(AgentToolCallStatus.FAILED);
        verify(recordService, never()).updateUnlockAt(anyLong(), anyLong(), any());
    }

    private AgentToolCall toolCall(AgentToolName tool) {
        AgentToolCall toolCall = new AgentToolCall();
        toolCall.setId(1L);
        toolCall.setSessionId(2L);
        toolCall.setUserId(USER_ID);
        toolCall.setRecordId(RECORD_ID);
        toolCall.setTurnNo(1);
        toolCall.setToolName(tool.wireName());
        toolCall.setStatus(AgentToolCallStatus.PROPOSED);
        return toolCall;
    }
}
