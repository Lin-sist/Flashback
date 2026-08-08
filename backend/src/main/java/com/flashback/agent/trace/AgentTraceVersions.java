package com.flashback.agent.trace;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.guardrail.AgentGuardrailRules;
import com.flashback.agent.reflection.AgentReflectionPolicy;
import com.flashback.agent.temporal.AgentTemporalLanguageChecker;
import com.flashback.agent.temporal.AgentTemporalPolicy;
import com.flashback.config.AppAgentProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 提示词与护栏规则的版本锚点（C5）。
 *
 * 存在理由（design.md 决策 6）：C6 要拿轨迹做「改动前 vs 改动后」的回归比对，
 * 前提是能判断两批数据是否出自同一版本。手工维护版本常量的失效方式很典型——
 * 改了文案忘了 bump，于是 C6 拿到「版本号没变但行为已变」的脏数据；
 * 那比没有版本更糟，因为没有版本时你知道自己不知道。
 *
 * 因此版本由**内容本身**派生：改文案 → 哈希变 → 版本自动变，不依赖人记得。
 *
 * 代价是版本不可读（`p:3f9a1c` 而非 `v2`），看不出演进先后。这个代价可以接受：
 * C6 需要的是「是否同一版本」，不是「哪个版本更新」。
 *
 * 隐私：输入只有 prompt 模板与规则文案——它们是代码里的常量，不含任何用户数据。
 * 输出只有 8 位十六进制，不可还原原文。
 */
@Component
public class AgentTraceVersions {

    /** 指纹长度：足够区分版本，不足以支撑还原。 */
    private static final int FINGERPRINT_LENGTH = 8;

    private final AgentPromptBuilder promptBuilder;
    private final AgentGuardrailPolicy guardrailPolicy;
    private final AgentGuardrailRules guardrailRules;
    private final AgentReflectionPolicy reflectionPolicy;
    private final AppAgentProperties properties;
    private final AgentTemporalLanguageChecker temporalLanguageChecker;

    public AgentTraceVersions(
            AgentPromptBuilder promptBuilder,
            AgentGuardrailPolicy guardrailPolicy,
            AgentGuardrailRules guardrailRules,
            AgentReflectionPolicy reflectionPolicy,
            AppAgentProperties properties) {
        this.promptBuilder = promptBuilder;
        this.guardrailPolicy = guardrailPolicy;
        this.guardrailRules = guardrailRules;
        this.reflectionPolicy = reflectionPolicy;
        this.properties = properties;
        this.temporalLanguageChecker = new AgentTemporalLanguageChecker();
    }

    /**
     * 提示词版本：由角色设定、阶段目标与轮次指令的实际文案派生。
     *
     * 刻意**不缓存**：缓存会让「改文案 → 版本变」这个性质依赖重启，
     * 而热重载或测试中修改配置后版本不变会造出与手工 bump 同样的脏数据。
     * 计算成本是一次 SHA-256，相对一次 provider 调用可忽略。
     */
    public String promptVersion() {
        return "p" + fingerprint(promptBuilder.promptTemplateFingerprintSource());
    }

    /**
     * 护栏规则版本：由 prompt 侧条款文案与后置检查词表共同派生。
     *
     * 两者都进指纹的理由与 C4 决策 5 一致——它们本就是同一份声明的两面，
     * 只把其中一面纳入版本，改另一面时版本不变，等于给自己留一个观测盲区。
     */
    public String policyVersion() {
        StringBuilder builder = new StringBuilder();
        builder.append(guardrailPolicy.guardrailClause()).append('\n');
        builder.append(guardrailPolicy.maxReplyChars()).append('\n');
        builder.append(guardrailRules.toolUsageClause()).append('\n');
        builder.append(guardrailRules.memoryUsageClause()).append('\n');
        builder.append(guardrailRules.materialClause()).append('\n');
        builder.append(reflectionPolicy.fingerprintSource()).append('\n');
        builder.append(AgentTemporalPolicy.fingerprintSource(properties.getTemporal())).append('\n');
        builder.append(temporalLanguageChecker.fingerprintSource()).append('\n');
        builder.append(AgentGuardrailRules.SAFE_FALLBACK_REPLY).append('\n');
        appendAll(builder, AgentGuardrailRules.MINIMUM_GUARDRAILS);
        appendAll(builder, AgentGuardrailRules.POSITIVE_BEHAVIORS);
        appendAll(builder, AgentGuardrailRules.DIAGNOSTIC_TERMS);
        appendAll(builder, AgentGuardrailRules.DIAGNOSTIC_PATTERNS);
        appendAll(builder, AgentGuardrailRules.FAKE_ACTION_PATTERNS);
        appendAll(builder, AgentGuardrailRules.TIME_ATTRIBUTION_TERMS);
        return "g" + fingerprint(builder.toString());
    }

    private static void appendAll(StringBuilder builder, List<String> values) {
        for (String value : values) {
            builder.append(value).append('|');
        }
        builder.append('\n');
    }

    private static String fingerprint(String source) {
        if (source == null) {
            return "none";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
                if (hex.length() >= FINGERPRINT_LENGTH) {
                    break;
                }
            }
            return hex.substring(0, Math.min(FINGERPRINT_LENGTH, hex.length()));
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 是 JDK 必备算法；真出现则退化为占位符，绝不回退到贴原文。
            return "unavail";
        }
    }
}
