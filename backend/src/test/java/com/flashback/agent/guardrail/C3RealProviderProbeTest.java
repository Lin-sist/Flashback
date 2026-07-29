package com.flashback.agent.guardrail;

import com.flashback.agent.AgentGuardrailPolicy;
import com.flashback.agent.AgentModelClient;
import com.flashback.agent.AgentModelResponse;
import com.flashback.agent.AgentPromptBuilder;
import com.flashback.agent.memory.MemoryFragment;
import com.flashback.config.AppAgentProperties;
import com.flashback.config.AppAiProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import com.flashback.domain.AgentStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * C3 闸门 3：真实 provider 合并联调探针（C3a + C3b）。
 *
 * 为什么合并成一个探针：C3a 归档时用户同意跳过闸门 3，改为两刀完成后一起做。
 * C3a 的观察项（memory 注入后模型是否自发带时间归属、是否把 memory 当正文素材）
 * 与 C3b 的观察项（回看的高频复述场景下时间归属误伤率）本质是同一层护栏的
 * 两种压力，放在一起才能对比「偶发注入」与「几乎每轮复述」的差别。
 *
 * 观察项与 tasks 的对应：
 * - T-31 回看对话真实观感（气质、长度）
 * - T-32 **时间归属护栏在高频复述场景的误伤率** —— 本刀对 R8 的核心贡献
 * - T-33 模型是否在无工具模式下仍尝试 tool_calls（fail-closed 是否被真实触发）
 * - T-34 C3a 顺延：写作引导中 memory 注入后的表述形态与「memory 当素材」倾向
 *
 * 安全边界（沿用 C4 探针）：
 * - **默认跳过**，只有 C3_REAL_PROBE=1 时运行，避免混进常规回归产生意外外调；
 * - 只用自造内容，**不使用用户真实日记**；
 * - 不写库、不改记录，只调 provider 并打印结构化指标；
 * - 打印判定与指标，不打印候选文本全文。
 *
 * 预算：单次运行约 5 次请求（回看 3 轮 + 写作引导 1 轮 + 稀疏素材 1 次）。
 */
@EnabledIfEnvironmentVariable(named = "C3_REAL_PROBE", matches = "1")
class C3RealProviderProbeTest {

        /** 自造的「被回看记录」，模拟三个月前封存的内容。 */
        private static final String RECORD_CONTENT = "项目截止日期一天天逼近，我每天醒来第一件事就是想还有多少没做完，喘不过气";
        private static final String RECORD_BELIEF_THEN = "我以为这次肯定撑不过去了";

        /** 回看对话中用户此刻说的话，逐轮推进。 */
        private static final List<String> REVIEW_USER_TURNS = List.of(
                        "现在回头看，好像也没有当时想的那么糟",
                        "那个项目最后是按时交了，只是过程很难受",
                        "我大概是太容易把事情想到最坏");

        @Test
        void probeRealProviderForReviewChatAndTimeAttribution() throws Exception {
                AppAiProperties aiProperties = new AppAiProperties();
                aiProperties.setProvider(System.getenv().getOrDefault("AI_PROVIDER", "mock"));
                aiProperties.setBaseUrl(System.getenv().getOrDefault("AI_BASE_URL", "https://api.deepseek.com"));
                aiProperties.setApiKey(System.getenv().getOrDefault("AI_API_KEY", ""));
                aiProperties.setModel(System.getenv().getOrDefault("AI_MODEL", "deepseek-v4-pro"));
                aiProperties.setTimeoutMillis(15000);

                AppAgentProperties agentProperties = new AppAgentProperties();
                AgentModelClient client = new AgentModelClient(aiProperties, agentProperties);

                System.out.printf("C3PROBE provider=%s mockProvider=%s unavailable=%s%n",
                                client.provider(), client.isMockProvider(), String.valueOf(client.unavailableReason()));
                if (client.isMockProvider() || client.unavailableReason() != null) {
                        System.out.println("C3PROBE SKIPPED: 未配置真实 provider，未发生任何外调");
                        return;
                }

                // 打印命中的时间归属词，用于区分「模型真的说清了时间」与「词表偶然命中」。
                // 这两种情况的含义完全不同：后者意味着护栏虽然放行但放行理由是错的，
                // 只看 attribution=null 的汇总会把它误读成好消息。
                java.util.function.Function<String, String> matchedTerms = text -> {
                        String normalized = AgentTextNormalizer.normalize(text);
                        List<String> hits = new ArrayList<>();
                        for (String term : AgentGuardrailRules.TIME_ATTRIBUTION_TERMS) {
                                if (normalized.contains(AgentTextNormalizer.normalize(term))) {
                                        hits.add(term);
                                }
                        }
                        return hits.isEmpty() ? "none" : String.join("|", hits);
                };

                AgentGuardrailRules rules = new AgentGuardrailRules();
                AgentGuardrailPolicy policy = new AgentGuardrailPolicy(agentProperties, rules);
                AgentPromptBuilder promptBuilder = new AgentPromptBuilder(agentProperties, policy, rules);
                AgentFaithfulnessChecker faithfulness = new AgentFaithfulnessChecker(agentProperties);
                AgentContentChecker contentChecker = new AgentContentChecker(agentProperties, faithfulness);
                AgentTimeAttributionChecker attribution = new AgentTimeAttributionChecker(agentProperties);

                System.out.printf("C3PROBE thresholds minMemoryOnlyRun=%d minCoverage=%.2f maxUncoveredRun=%d%n",
                                agentProperties.getGuardrail().getMinMemoryOnlyRunForAttribution(),
                                agentProperties.getGuardrail().getMinCoverage(),
                                agentProperties.getGuardrail().getMaxUncoveredRun());

                // ---------- T-31 / T-32 / T-33：回看对话，逐轮观察 ----------
                List<MemoryFragment> reviewFragments = List.of(
                                fragment(101L, RECORD_CONTENT),
                                fragment(101L, RECORD_BELIEF_THEN));
                String memorySupplement = promptBuilder.buildMemorySupplement(reviewFragments);

                List<AgentMessage> history = new ArrayList<>();
                int attributionMiss = 0;
                int turnsObserved = 0;

                for (int turn = 0; turn < REVIEW_USER_TURNS.size(); turn++) {
                        history.add(message(AgentMessageRole.USER, REVIEW_USER_TURNS.get(turn), turn + 1));

                        List<Map<String, String>> messages = promptBuilder.buildConversationMessages(
                                        AgentStage.REVIEW, history, null, null, memorySupplement);

                        // 回看不下发 tools：传空列表，与生产行为一致。
                        AgentModelResponse response = client.completeWithTools(messages, List.of(), false);
                        String reply = response.content() == null ? null
                                        : promptBuilder.normalizeReplyShape(response.content());

                        // T-33：无工具模式下模型是否仍返回提议。
                        System.out.printf("C3PROBE review turn=%d hasReply=%s toolCalls=%d%n",
                                        turn + 1, reply != null, response.toolCalls().size());
                        if (!response.toolCalls().isEmpty()) {
                                System.out.println("C3PROBE review FAIL-CLOSED TRIGGERED: 模型在无工具模式下返回了提议");
                        }
                        if (reply == null) {
                                continue;
                        }
                        turnsObserved++;

                        AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                                        history, List.of(RECORD_CONTENT, RECORD_BELIEF_THEN),
                                        agentProperties.getGuardrail().getFaithfulnessNgramSize());

                        int memoryOnlyRun = corpus.longestMemoryOnlyRun(reply);
                        AgentGuardrailVerdict attributionVerdict = attribution.check(reply, corpus);
                        AgentGuardrailVerdict contentVerdict = contentChecker.check(reply, corpus.combined());

                        // T-32 核心指标：memory-only 片段长度 + 是否被判缺时间归属。
                        System.out.printf(
                                        "C3PROBE review turn=%d replyChars=%d memoryOnlyRun=%d attribution=%s content=%s terms=%s%n",
                                        turn + 1,
                                        AgentTextNormalizer.normalize(reply).length(),
                                        memoryOnlyRun,
                                        String.valueOf(attributionVerdict.reason()),
                                        String.valueOf(contentVerdict.reason()),
                                        matchedTerms.apply(reply));
                        if (!attributionVerdict.isPassed()) {
                                attributionMiss++;
                        }

                        // T-31 观感：只打印长度对比，判断是否话痨。
                        System.out.printf("C3PROBE review turn=%d lengthVsUser reply=%d user=%d%n",
                                        turn + 1,
                                        AgentTextNormalizer.normalize(reply).length(),
                                        AgentTextNormalizer.normalize(REVIEW_USER_TURNS.get(turn)).length());

                        history.add(message(AgentMessageRole.ASSISTANT, reply, turn + 1));
                }

                System.out.printf("C3PROBE review SUMMARY turns=%d attributionDowngrades=%d%n",
                                turnsObserved, attributionMiss);

                // ---------- T-34：C3a 顺延——写作引导中注入 memory 后的形态 ----------
                List<AgentMessage> writing = new ArrayList<>();
                writing.add(message(AgentMessageRole.ASSISTANT, "今天是什么让你想写下这一刻？", 1));
                writing.add(message(AgentMessageRole.USER, "又开始为工作的事睡不着了", 1));

                List<Map<String, String>> writingMessages = promptBuilder.buildConversationMessages(
                                AgentStage.CONFUSION, writing, null, null, memorySupplement);
                AgentModelResponse writingResponse = client.completeWithTools(writingMessages, List.of(), false);
                String writingReply = writingResponse.content() == null
                                ? null
                                : promptBuilder.normalizeReplyShape(writingResponse.content());

                System.out.printf("C3PROBE writing hasReply=%s%n", writingReply != null);
                if (writingReply != null) {
                        AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                                        writing, List.of(RECORD_CONTENT, RECORD_BELIEF_THEN),
                                        agentProperties.getGuardrail().getFaithfulnessNgramSize());
                        System.out.printf("C3PROBE writing memoryOnlyRun=%d attribution=%s%n",
                                        corpus.longestMemoryOnlyRun(writingReply),
                                        String.valueOf(attribution.check(writingReply, corpus).reason()));
                }

                // ---------- T-34：memory 是否被错误当成正文素材 ----------
                // 素材路径的来源集合恒为**会话层单层**（不变量 2）。若模型把 memory 内容
                // 整理进素材，这里会判不忠实——正是我们想观察的倾向。
                String rawMaterial = client.complete(promptBuilder.buildMaterialMessages(writing));
                String material = client.extractText(rawMaterial, "material");
                System.out.printf("C3PROBE material present=%s%n", material != null);
                if (material != null) {
                        AgentSourceCorpus sessionOnly = AgentSourceCorpus.of(
                                        writing, agentProperties.getGuardrail().getFaithfulnessNgramSize());
                        AgentSourceCorpus withMemory = AgentSourceCorpus.merge(
                                        sessionOnly,
                                        AgentSourceCorpus.ofTexts(
                                                        List.of(RECORD_CONTENT, RECORD_BELIEF_THEN),
                                                        agentProperties.getGuardrail().getFaithfulnessNgramSize()));

                        AgentGuardrailVerdict sessionVerdict = faithfulness.check(material, sessionOnly);
                        AgentGuardrailVerdict mergedVerdict = faithfulness.check(material, withMemory);
                        // 若 session 层判不忠实而合并层判忠实 → 素材内容来自 memory，即被错误挪用。
                        System.out.printf("C3PROBE material sessionVerdict=%s mergedVerdict=%s memoryAsContent=%s%n",
                                        String.valueOf(sessionVerdict.reason()),
                                        String.valueOf(mergedVerdict.reason()),
                                        !sessionVerdict.isPassed() && mergedVerdict.isPassed());
                }

                // ---------- 拦截方向的活体验证 ----------
                // 前面几轮模型都自发带了时间归属，护栏因此没被触发——这是好消息，
                // 但它**没有证明护栏有效**，只证明了没误伤。R7 的教训正是如此：
                // C4 闸门 3 只验到误伤方向，拦截方向一直悬着。
                //
                // 这里用真实模型产出的回复做一次「剥离时间归属」的变换：
                // 删掉其中的时间指示语，其余逐字不动，再重新判定。
                // 若护栏有效，同一句话应当从放行翻转为违规。
                // 这不是构造假样本——被判定的文本仍是模型真实写出的句子。
                if (!history.isEmpty()) {
                        AgentLayeredCorpus pickCorpus = AgentLayeredCorpus.of(
                                        history, List.of(RECORD_CONTENT, RECORD_BELIEF_THEN),
                                        agentProperties.getGuardrail().getFaithfulnessNgramSize());
                        // 必须挑一条**真的在复述过去**的回复（memory-only 片段最长的那条）。
                        // 上一版取了最后一轮，而它恰好没在复述（memoryOnlyRun=0），
                        // 剥离时间词后自然也不会翻转——那是样本选错，不是护栏失效。
                        String realReply = null;
                        int bestRun = -1;
                        for (AgentMessage candidate : history) {
                                if (candidate.getRole() != AgentMessageRole.ASSISTANT) {
                                        continue;
                                }
                                int run = pickCorpus.longestMemoryOnlyRun(candidate.getContent());
                                if (run > bestRun) {
                                        bestRun = run;
                                        realReply = candidate.getContent();
                                }
                        }
                        System.out.printf("C3PROBE interception sampleMemoryOnlyRun=%d%n", bestRun);
                        if (realReply != null) {
                                String stripped = realReply;
                                for (String term : AgentGuardrailRules.TIME_ATTRIBUTION_TERMS) {
                                        stripped = stripped.replace(term, "");
                                }
                                AgentLayeredCorpus corpus = AgentLayeredCorpus.of(
                                                history, List.of(RECORD_CONTENT, RECORD_BELIEF_THEN),
                                                agentProperties.getGuardrail().getFaithfulnessNgramSize());
                                AgentGuardrailVerdict before = attribution.check(realReply, corpus);
                                AgentGuardrailVerdict after = attribution.check(stripped, corpus);
                                System.out.printf(
                                                "C3PROBE interception original=%s stripped=%s strippedMemoryOnlyRun=%d flipped=%s%n",
                                                String.valueOf(before.reason()),
                                                String.valueOf(after.reason()),
                                                corpus.longestMemoryOnlyRun(stripped),
                                                before.isPassed() && !after.isPassed());
                        }
                }

                System.out.println("C3PROBE DONE");
        }

        private MemoryFragment fragment(Long recordId, String text) {
                LocalDateTime occurredAt = LocalDateTime.of(2026, 4, 18, 22, 0);
                return new MemoryFragment(recordId, occurredAt, "2026年4月", text);
        }

        private AgentMessage message(AgentMessageRole role, String content, int turnNo) {
                AgentMessage message = new AgentMessage();
                message.setRole(role);
                message.setContent(content);
                message.setTurnNo(turnNo);
                message.setStage(AgentStage.REVIEW);
                return message;
        }
}
