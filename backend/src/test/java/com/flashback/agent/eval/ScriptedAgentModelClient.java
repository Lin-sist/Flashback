package com.flashback.agent.eval;

import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentModelResponse;
import com.flashback.agent.AgentRawToolCall;
import com.flashback.config.AppAgentProperties;
import com.flashback.config.AppAiProperties;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * 可编排的模型客户端替身（C6，N3 / design 决策 2）。
 *
 * <h3>为什么替身挂在这一层，而不是给 {@code AgentMockResponder} 抽接口</h3>
 * 规划期核对代码时发现一件决定性的事（proposal E7）：{@code generateReply} 在
 * {@code if (modelClient.isMockProvider())} 处**直接 return**，因此 mock 分支
 * <b>根本不组装 prompt</b>——mock 路径的轨迹里永远没有 {@code prompt} 步骤。
 * 加上 {@code AgentMockResponder} 按构造产不出任何违规（六句写死合规文案、
 * material 只拼用户发言、toolCalls 只取用户原话），只走它的评测会同时缺
 * 上下文组装维度与**全部降级路径**。
 *
 * 所以本类走的是「非 mock」分支：让 {@code isMockProvider()} 为 false，
 * 把编排推上真实 provider 那条路，再在最外层的 HTTP 边界处返回编排好的响应。
 *
 * <h3>为什么继承而不是 Mockito 整体 mock</h3>
 * 本类**只覆写两个真正发起网络调用的方法**（{@link #completeWithTools} 与
 * {@link #complete}）。其余全部行为——{@code unavailableReason()}、
 * {@code toolCallingUnavailableReason()}、{@code isFunctionCallingModel}、
 * {@code useStrictMode()}、{@code model()}、{@code provider()}、
 * {@code readArgumentText}、{@code extractText}——走的都是**生产实现**，
 * 由真实的 {@link AppAiProperties} 配置驱动。
 *
 * 这一点比省事更重要：整体 mock 会把「配置是否可用」「model 是否在 FC 白名单内」
 * 这些判定一并 stub 掉，于是评测断言的就不再是生产逻辑，而是我自己写的 stub。
 * C5 的教训（H2 全绿≠验证）是同型的——替身替掉的东西越多，测到的生产代码越少。
 *
 * <h3>边界</h3>
 * 本类只存在于测试范围，**不改动任何生产组件**（`AgentMockResponder` 一行未动）。
 * 它不发起任何真实网络调用：覆写的两个方法从不触碰 HttpClient。
 */
public final class ScriptedAgentModelClient extends AgentModelClient {

    /**
     * 一次编排好的响应：要么给内容与提议，要么抛异常。
     *
     * @param content   自然语言回复；null 表示 provider 只给了提议
     * @param toolCalls 原生工具提议
     * @param failure   非 null 时本次调用抛出该异常，用于覆盖 provider 失败路径
     */
    record Scripted(String content, List<AgentRawToolCall> toolCalls, RuntimeException failure) {

        static Scripted reply(String content) {
            return new Scripted(content, List.of(), null);
        }

        static Scripted replyWithTool(String content, AgentRawToolCall toolCall) {
            return new Scripted(content, List.of(toolCall), null);
        }

        static Scripted failure(RuntimeException failure) {
            return new Scripted(null, List.of(), failure);
        }
    }

    private final Deque<Scripted> replyScript = new ArrayDeque<>();
    private final Deque<String> materialScript = new ArrayDeque<>();
    private final List<Integer> promptMessageCounts = new ArrayList<>();

    private int replyCallCount;
    private int materialCallCount;

    /**
     * 构造一个「配置上完全可用」的 openai-compatible 客户端。
     *
     * 三个配置值都不是随手填的：
     * - provider 必须是 openai-compatible，否则 {@code isMockProvider()} 为 true，编排会走 mock
     * 分支；
     * - model 必须在 {@code functionCallingModels} 白名单内，否则
     * {@code toolCallingUnavailableReason()} 非 null，本轮不下发 tools，工具维度就评不到；
     * - apiKey 必须非空，否则 {@code unavailableReason()} 返回「AI服务未配置」，编排在调用前就早退。
     *
     * apiKey 是一个**显式的假值**，不读任何本地 secret，也永不参与请求构造
     * （覆写的方法从不触碰 HttpClient）。
     */
    static ScriptedAgentModelClient available(AppAgentProperties appAgentProperties) {
        AppAiProperties ai = new AppAiProperties();
        ai.setProvider("openai-compatible");
        ai.setModel("deepseek-v4-pro");
        ai.setApiKey("eval-scripted-not-a-real-key");
        ai.setBaseUrl("https://eval.invalid");
        return new ScriptedAgentModelClient(ai, appAgentProperties);
    }

    private ScriptedAgentModelClient(AppAiProperties appAiProperties, AppAgentProperties appAgentProperties) {
        super(appAiProperties, appAgentProperties);
    }

    ScriptedAgentModelClient scriptReply(Scripted scripted) {
        replyScript.addLast(scripted);
        return this;
    }

    ScriptedAgentModelClient scriptMaterial(String material) {
        materialScript.addLast(material);
        return this;
    }

    int replyCallCount() {
        return replyCallCount;
    }

    int materialCallCount() {
        return materialCallCount;
    }

    /**
     * 每次回复调用时 prompt 的消息条数。
     *
     * 存在理由：它是「上下文组装确实发生了」的独立证据。轨迹里的 {@code prompt}
     * 步骤来自编排层的采集，而本列表来自 provider 边界实际收到的东西——
     * 两者对不上就说明采集点与真实调用脱节。
     */
    List<Integer> promptMessageCounts() {
        return List.copyOf(promptMessageCounts);
    }

    @Override
    public AgentModelResponse completeWithTools(
            List<Map<String, String>> messages,
            List<Map<String, Object>> tools,
            boolean strictMode) throws IOException {

        replyCallCount++;
        promptMessageCounts.add(messages == null ? 0 : messages.size());

        if (replyScript.isEmpty()) {
            // 剧本用尽是用例写错了，不是被测代码的问题。明确失败，不悄悄兜一个回复——
            // 那会让「多跑了一轮」表现为一条看起来正常的轨迹。
            throw new IllegalStateException(
                    "scripted reply exhausted at call #" + replyCallCount + "; check the eval case turns");
        }
        Scripted scripted = replyScript.removeFirst();
        if (scripted.failure() != null) {
            throw scripted.failure();
        }
        if (scripted.content() == null && scripted.toolCalls().isEmpty()) {
            // 与生产实现同构：既无内容也无提议时抛 IOException，交由上层按显式失败处理。
            throw new IOException("scripted response missing content and tool_calls");
        }
        return new AgentModelResponse(scripted.content(), scripted.toolCalls());
    }

    /**
     * 素材路径（生产实现走 {@code complete} 并约定返回 JSON 字符串）。
     *
     * 返回 JSON 而不是裸文本：上层紧接着调 {@code extractText(raw, "material")}，
     * 那是**生产实现**，形状不对就解析不出来。保持同构才能让素材路径的护栏真的被走到。
     */
    @Override
    public String complete(List<Map<String, String>> messages) throws IOException {
        materialCallCount++;
        if (materialScript.isEmpty()) {
            throw new IOException("scripted material exhausted");
        }
        String material = materialScript.removeFirst();
        if (material == null) {
            throw new IOException("scripted material failure");
        }
        return "{\"material\":\"" + material.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }
}
