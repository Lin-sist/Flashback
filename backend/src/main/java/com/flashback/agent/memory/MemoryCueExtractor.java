package com.flashback.agent.memory;

import com.flashback.config.AppAgentProperties;
import com.flashback.domain.AgentMessage;
import com.flashback.domain.AgentMessageRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 从对话中提取检索线索（C3 agent-memory-retrieval）。
 *
 * 只取**用户自己的表达**，不取 Agent 的提问。理由不是洁癖：
 * Agent 的问题里有大量它自己引入的措辞（「是什么让你想写下这一刻」），
 * 用这些词去检索用户的历史，命中的会是 Agent 的语言习惯而不是用户的心事。
 *
 * 切词方式（本类的关键取舍）：按标点与空白切分，保留达到最小长度的中文片段。
 * 刻意**不引入分词器**——AGENTS.md 禁止改 package / lockfile，
 * 且蓝图 D7 已把 C3 的检索定为「简单检索」。
 * 代价是切出的片段偏长、召回偏低；这是已接受的弱相关性（design 决策 13），
 * 不粉饰、不用更复杂的启发式假装解决。
 *
 * 隐私：产出的关键词来自用户原话，只在内存中存在，不落库、不写日志。
 */
@Component
public class MemoryCueExtractor {

    /**
     * 切分用的分隔符：中英文标点与空白。
     *
     * 不含数字与字母边界处理——保留「后端」「offer」这类词的完整性。
     */
    private static final String DELIMITERS = "[\\s，。！？；：、,.!?;:…—～~()（）\\[\\]【】\"“”'‘’《》<>/\\\\|+*#&%$@^`\\-_=]+";

    /**
     * 停用片段。
     *
     * 存在理由：这些词在任何一条记录里都可能出现，用它们检索等于全表匹配，
     * 命中结果与用户此刻在说什么毫无关系。
     * 清单刻意保持短小——长停用表会变成需要维护的运营负担，
     * 而召回质量的主要抓手是最小长度而不是停用词。
     */
    private static final Set<String> STOP_FRAGMENTS = Set.of(
            "我", "你", "他", "她", "它", "我们", "你们", "他们", "自己",
            "这个", "那个", "这些", "那些", "什么", "怎么", "为什么", "哪里",
            "就是", "然后", "还有", "但是", "不过", "因为", "所以", "如果",
            "可能", "应该", "觉得", "感觉", "知道", "现在", "今天", "有点",
            "一个", "一些", "很多", "真的", "其实", "而且", "或者", "已经",
            "不是", "没有", "可以", "想要", "记录", "写下", "这样", "那样");

    private final AppAgentProperties appAgentProperties;

    public MemoryCueExtractor(AppAgentProperties appAgentProperties) {
        this.appAgentProperties = appAgentProperties;
    }

    /**
     * 从会话历史中提取关键词。
     *
     * @param history 会话消息（正序），仅 role=USER 参与
     */
    public List<String> extractKeywords(List<AgentMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        AppAgentProperties.Memory config = appAgentProperties.getMemory();

        List<String> recentUserContents = recentUserContents(history, config.getCueMessageWindow());
        if (recentUserContents.isEmpty()) {
            return List.of();
        }

        // LinkedHashSet：去重同时保留出现顺序，让「用户最近说的」优先进入关键词。
        Set<String> keywords = new LinkedHashSet<>();
        for (String content : recentUserContents) {
            for (String fragment : content.split(DELIMITERS)) {
                if (keywords.size() >= config.getMaxKeywords()) {
                    return List.copyOf(keywords);
                }
                String candidate = fragment.trim();
                if (candidate.length() < config.getMinKeywordLength()) {
                    continue;
                }
                if (STOP_FRAGMENTS.contains(candidate)) {
                    continue;
                }
                keywords.add(candidate);
            }
        }
        return List.copyOf(keywords);
    }

    /**
     * 取最近若干条用户消息，**最新的在前**。
     *
     * 顺序很重要：extractKeywords 是「先到先占额度」，
     * 而用户最新一句话才是他此刻在说的事——它应该优先决定检索方向，
     * 而不是被几轮之前的话把关键词额度用光。
     */
    private List<String> recentUserContents(List<AgentMessage> history, int window) {
        List<String> contents = new ArrayList<>();
        for (int i = history.size() - 1; i >= 0 && contents.size() < window; i--) {
            AgentMessage message = history.get(i);
            if (message == null || message.getRole() != AgentMessageRole.USER) {
                continue;
            }
            String content = message.getContent();
            if (content != null && !content.isBlank()) {
                contents.add(content);
            }
        }
        return contents;
    }
}
