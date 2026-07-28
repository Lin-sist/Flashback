package com.flashback.agent.guardrail;

import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 素材路径忠实度测试（C4）。
 *
 * 为什么素材也要过闸（design 决策 6）：素材与工具正文参数**结构上完全同质**——
 * 都是模型产出的文本、都会在用户点一下之后进入 record.content、
 * 都只有 prompt 约束而无后置校验。R1 先在工具路径被观测到纯属偶然
 * （手验那次 Agent 选择了提议工具而非收束）。
 * 只做工具路径等于修好一扇门却把旁边同样大的门留着开。
 *
 * 本类直接验证判定语义；服务层的「不忠实即丢弃、会话仍正常结束」
 * 由 AgentChatServiceImpl 的既有可选产物语义承载（materialDraft = null）。
 */
class AgentMaterialGuardrailTest {

    private AppAgentProperties properties;
    private AgentFaithfulnessChecker checker;

    @BeforeEach
    void setUp() {
        properties = new AppAgentProperties();
        checker = new AgentFaithfulnessChecker(properties);
    }

    private AgentSourceCorpus corpusOf(List<AgentMessage> history) {
        return AgentSourceCorpus.of(history, properties.getGuardrail().getFaithfulnessNgramSize());
    }

    private AgentMessage message(AgentMessageRole role, String content) {
        AgentMessage message = new AgentMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    @Test
    void materialThatOnlyConcatenatesUserWordsMustPass() {
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.ASSISTANT, "今天是什么让你想写下这一刻？"),
                message(AgentMessageRole.USER, "工作上有点撑不住"),
                message(AgentMessageRole.ASSISTANT, "让你卡住的是具体某件事吗？"),
                message(AgentMessageRole.USER, "主要是不知道先做哪件事"));

        String material = "工作上有点撑不住\n主要是不知道先做哪件事";

        assertThat(checker.check(material, corpusOf(history)).isPassed()).isTrue();
    }

    @Test
    void materialThatAddsAgentSummaryMustBeRejected() {
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.USER, "工作上有点撑不住"),
                message(AgentMessageRole.USER, "主要是不知道先做哪件事"));

        // 模型在素材里加了一句自己的共情总结——用户从未这样表达。
        String material = "工作上有点撑不住，主要是不知道先做哪件事。"
                + "其实这说明你已经在努力寻找平衡了，只是还需要一点时间来接纳自己的节奏和局限。";

        assertThat(checker.check(material, corpusOf(history)).violation())
                .isEqualTo(AgentGuardrailViolation.UNFAITHFUL);
    }

    /**
     * Agent 自己的回复不得成为素材的「合法来源」——
     * 否则 Agent 的表达会经素材路径混入用户正文。
     */
    @Test
    void materialCopiedFromAgentReplyMustBeRejected() {
        List<AgentMessage> history = List.of(
                message(AgentMessageRole.ASSISTANT, "听起来你最近为方向的事情很纠结，心里也有点空落落的"),
                message(AgentMessageRole.USER, "嗯，是的"));

        assertThat(checker.check("最近为方向的事情很纠结，心里也有点空落落的", corpusOf(history)).isPassed())
                .isFalse();
    }
}
