package com.flashback.agent.guardrail;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 护栏规则唯一声明源（C4）。
 *
 * 存在理由（design.md 决策 5）：C4 之前护栏规则分散在三处——
 * AgentGuardrailPolicy 的常量、AgentPromptBuilder.buildToolSupplement()、
 * buildMaterialMessages()。若 prompt 里写「不许诊断」而后置检查的词表是另一份独立清单，
 * 两者会随时间分叉，出现「prompt 允许但检查拦」或「检查放过但 prompt 禁止」的
 * 自相矛盾状态，而这种矛盾极难在测试里发现。
 *
 * 因此本类同时承载：
 * 1. 注入 system prompt 的护栏文案（含**正向行为**，避免只有禁止清单）；
 * 2. 后置检查使用的规则词表。
 * 两者派生自同一份声明。
 *
 * 与既有类的关系：AgentGuardrailPolicy 保留（enforceReplyLength 是 C1 已接受的护栏），
 * 其 guardrailClause() 改为委托本类；prompt 的实际文字内容不变，避免踩 R2 延后边界。
 */
@Component
public class AgentGuardrailRules {

    /** 五条最小护栏，注入 system prompt（文案与 C1 一致，仅迁移声明位置）。 */
    public static final List<String> MINIMUM_GUARDRAILS = List.of(
            "不诊断：不使用任何心理或医学诊断词，不判断病症，不给医学建议。",
            "不覆写：不改写、不替换、不“修正”用户的原话；需要引用时原样引用。",
            "建议不代决：封存、解锁、删除只能建议，由用户自己在页面确认，你不能代替用户决定。",
            "被动陪伴：不主动开启新话题分析用户，不催促，不追问已被回避的问题。",
            "输出克制：一次只说一到两句，不长于用户的表达，不说教、不总结陈词。");

    /**
     * 正向行为清单（蓝图 §6.4）。
     *
     * 存在理由：蓝图 C4 风险栏点名「过度限制导致 Agent 无话可说」，
     * 缓解方式是护栏同时定义**可以做什么**，而不仅是负面清单。
     */
    public static final List<String> POSITIVE_BEHAVIORS = List.of(
            "温和引导：问一个具体、好回答的问题，而不是抽象的大问题。",
            "共情回应：用户说难受时先接住他的感受，再谈别的。",
            "尊重沉默：用户不想继续时优雅收束，让他知道可以停在这里。",
            "行动建议：合适的时候提议帮他做一件小事，由他点头才发生。",
            "原样引用：需要引用用户说过的话时，用他自己的措辞。");

    /**
     * 诊断类违规规则（design.md §3.4）。
     *
     * 只在候选文本的**新增区段**匹配——用户自己说「我可能有点焦虑症」时，
     * Agent 共情复述带上这个词是蓝图 §6.4 要求的恰当行为，不得误伤。
     *
     * 规则形态刻意是「病症名」与「判定式表述」两类，
     * 单独出现病症名不构成违规，须落在 Agent 新增表述中才命中。
     */
    public static final List<String> DIAGNOSTIC_TERMS = List.of(
            "焦虑症", "抑郁症", "躁郁", "双相", "强迫症", "创伤后应激", "ptsd",
            "精神分裂", "人格障碍", "神经症", "心理疾病", "精神疾病", "病理性");

    /** 判定式与医疗建议表述。 */
    public static final List<String> DIAGNOSTIC_PATTERNS = List.of(
            "你这是", "你患有", "你得了", "你有病", "典型的症状", "典型症状", "典型表现",
            "诊断为", "确诊", "建议你就医", "建议就医", "去看心理医生", "去看医生",
            "看精神科", "需要吃药", "服用药物", "药物治疗", "临床上");

    /**
     * 代决类违规规则：谎称已代替用户完成不可逆操作。
     *
     * 补的是 C2 的盲区——白名单保证 Agent **做不到** seal，
     * 但拦不住它**说自己做了**。
     */
    public static final List<String> FAKE_ACTION_PATTERNS = List.of(
            "已经帮你封存", "已经帮你解锁", "已经帮你删除", "已经帮你删掉",
            "已经为你封存", "已经为你解锁", "已经为你删除",
            "已帮你封存", "已帮你解锁", "已帮你删除",
            "我把它封存了", "我把它解锁了", "我把它删了", "我把它删除了",
            "已经封存好了", "已经解锁好了", "已经删除好了",
            "帮你封存了", "帮你解锁了", "帮你删除了", "替你封存", "替你解锁", "替你删除");

    /**
     * 时间归属表述（C3 agent-memory-retrieval）。
     *
     * 用途（design.md 决策 2）：当 Agent 回复中出现「只来自记忆层」的连续片段时，
     * 回复里必须有其中之一，否则那段过去的话读起来就像用户刚刚说的。
     *
     * 为什么是关键词而不是模型判断：C4 已立下的护栏标准要求确定性、零外调、可单测；
     * 用第二次模型调用判断「这句有没有时间归属」会让判定不可复现，
     * 无法写进回归用例集（C4 已否决的 LLM-as-judge 方向）。
     *
     * 收词原则偏宽——**宁可放过一句表达生硬的合法回复，也不要把正常回忆句判违规**。
     * 拦截方向由「片段长度阈值」承担，不靠词表收紧。
     */
    public static final List<String> TIME_ATTRIBUTION_TERMS = List.of(
            "以前", "从前", "过去", "当时", "那时", "那阵", "那会", "那次", "上次", "曾经",
            "之前", "早些时候", "先前", "后来", "当初",
            "去年", "今年", "上个月", "这个月", "上周", "前几天", "几天前",
            "上一次", "上回", "记得你", "你写过", "你写下", "你提过", "你说过",
            "一月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "十一月", "十二月",
            "个月前", "个月之前", "年前", "年之前", "周前", "天前", "月份");

    /**
     * 违规时的安全兜底回复。
     *
     * 边界（design.md §6）：这是**本地常量**，不是 provider 产物。
     * 使用它时必须留下结构化痕迹，使其可与真实回复区分——
     * 兜底不得伪装成模型正常输出（对齐 AGENTS.md「真实路径不得 mock success 冒充真实成功」）。
     */
    public static final String SAFE_FALLBACK_REPLY = "这些听起来挺不容易的。你想再多说一点吗？";

    /**
     * 护栏条款文本，供 system prompt 组装使用。
     */
    public String guardrailClause() {
        StringBuilder builder = new StringBuilder("你必须始终遵守以下边界：\n");
        for (int i = 0; i < MINIMUM_GUARDRAILS.size(); i++) {
            builder.append(i + 1).append(". ").append(MINIMUM_GUARDRAILS.get(i)).append('\n');
        }
        builder.append("\n你可以这样做：\n");
        for (String behavior : POSITIVE_BEHAVIORS) {
            builder.append("- ").append(behavior).append('\n');
        }
        return builder.toString().trim();
    }

    /**
     * 工具使用的气质约束文案（原在 AgentPromptBuilder.buildToolSupplement 内联）。
     */
    public String toolUsageClause() {
        return """
                关于你可以做的小动作：
                - 你可以在合适的时候提议帮用户做一件小事（把他说过的话整理进正文、加个标签、设一个解锁时间）。
                - 提议就只是提议：由用户点确认才会发生，你不能替他决定，也不要反复追问同一件事。
                - 封存、解锁、删除这些事你做不了，只能建议用户自己在页面上确认。
                - 你永远不能改写、替换或“修正”用户已经写下的文字，只能追加他自己说过的内容。
                - 整理进正文的文字必须是用户自己说过的：可以调语序、去掉口头语，但绝不能补写他没说过的内容。
                """.trim();
    }

    /**
     * 记忆段的 prompt 文案（C3）。
     *
     * 与后端硬拦成对存在：prompt 说明记忆只是理解材料，
     * 代码层再用「正文只认会话层」把它钉死（design.md 决策 4）。
     * 只写 prompt 不够——C4 的 R1 已经证明 prompt 级约束会被违反。
     */
    public String memoryUsageClause() {
        return """
                关于你还记得的一些事：
                - 下面这些是用户以前写下的片段，只用来帮你理解他，不是这次记录的正文素材。
                - 提到它们的时候，必须说清那是过去哪个时候的事，不要让它听起来像他刚刚说的。
                - 不要把以前写的句子整理进这次的正文，正文只能是他这次说过的话。
                - 如果他没有接这个话题，就不要反复提同一件旧事。
                """.trim();
    }

    /**
     * 素材整理的约束文案（原在 AgentPromptBuilder.buildMaterialMessages 内联）。
     */
    public String materialClause() {
        return """
                现在请把这段对话中【用户自己说过的内容】整理成一小段可以放进记录正文的素材。
                硬性要求：
                - 只使用用户说过的内容，不添加你的分析、评价、建议或诊断；
                - 不改变用户的意思，尽量保留用户自己的措辞；
                - 语气安静克制，不要写成总结报告；
                - 只输出 JSON，格式为 {"material":"整理后的素材"}，JSON 之外不要有任何文本。
                """.trim();
    }
}
