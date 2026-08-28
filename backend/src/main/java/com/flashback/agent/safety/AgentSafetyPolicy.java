package com.flashback.agent.safety;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * R1：高精度、零外调的当前输入安全策略。
 *
 * <p>它不是诊断器，也不尝试覆盖所有自然语言。只有第一人称、明确自伤意图、
 * 近时计划或正在实施的合成边界才会打断普通记录；否定、转述、历史回忆、
 * 研究讨论与常见比喻保持普通路径。</p>
 */
public final class AgentSafetyPolicy {

    public static final String LOCAL_RESPONSE =
            "我先不整理记录，先顾好你的安全。请远离可能伤害你的东西，去有人的地方，让可信任的人陪着你。"
                    + "若在中国大陆，请拨打120或110，也可拨打12356；若不在，请联系当地紧急服务。"
                    + "我不是专业救援人员，也无法替你通知任何人。你能先联系一个人吗？";

    private static final Pattern NEGATED = Pattern.compile(
            "我.{0,4}(?:不|没|没有|从没|不会|并不)(?:再)?(?:想|打算|准备|决定|会|要)?"
                    + ".{0,4}(?:自杀|去死|伤害自己|结束生命|不想活)");
    private static final Pattern HISTORICAL = Pattern.compile(
            "我(?:以前|曾经|过去|那时|当时|小时候|去年|前几年).{0,10}"
                    + "(?:想过?|打算过?|准备过?|差点)?(?:自杀|去死|伤害自己|不想活)");
    private static final Pattern CURRENT_DENIAL = Pattern.compile(
            "(?:现在|目前|此刻).{0,3}(?:没有|没|不会|不想|不打算|没了|没有了)"
                    + "(?:这种想法|这个念头|这个打算|自杀|伤害自己)?");
    private static final Pattern META_OR_QUOTE = Pattern.compile(
            "(?:这句话|这几个字|举例|例子|测试|研究|讨论|论文|新闻|报道|剧情|角色|台词|假设"
                    + "|如果(?:有人|用户|角色).{0,4}(?:说|写|表达))");
    private static final Pattern CURRENT_MARKER = Pattern.compile(
            "(?:现在|此刻|马上|立刻|今晚|今天|待会儿?|一会儿?|这就|已经|正在|刚刚)");
    private static final Pattern ATTEMPT_IN_PROGRESS = Pattern.compile(
            "(?:我.{0,4}(?:已经|刚刚|正在|正).{0,5}(?:割腕|跳楼|上吊|伤害自己)"
                    + "|我.{0,4}(?:已经|刚刚|正在|正).{0,8}(?:吞|吃|服).{0,4}"
                    + "(?:很多|一把|过量|整瓶|全部).{0,4}(?:药|药片)"
                    + "|我.{0,4}(?:药|药片).{0,4}(?:已经|刚刚).{0,5}(?:吃|吞|服).{0,4}"
                    + "(?:很多|一把|过量|整瓶|全部))");
    private static final Pattern IMMINENT_PLAN = Pattern.compile(
            "我.{0,6}(?:现在|马上|立刻|今晚|今天|待会儿?|一会儿?|这就).{0,6}"
                    + "(?:要|想|准备|打算|决定|会)?.{0,5}"
                    + "(?:自杀|去死|想死(?:了)?|结束生命|伤害自己|割腕|跳楼|上吊|不想活)");
    private static final Pattern DIRECT_INTENT = Pattern.compile(
            "我.{0,3}(?:真的)?(?:要|想|准备|打算|决定).{0,4}"
                    + "(?:自杀|去死|结束生命|伤害自己|割腕|跳楼|上吊)"
                    + "|我(?:就|便|会).{0,2}(?:自杀|去死|伤害自己|结束生命)"
                    + "|我.{0,3}(?:真的)?不想活了|^我(?:真的)?想死(?:了)?$");

    public AgentSafetyDecision assess(String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty() || META_OR_QUOTE.matcher(normalized).find()) {
            return AgentSafetyDecision.none();
        }
        if (ATTEMPT_IN_PROGRESS.matcher(normalized).find()) {
            return new AgentSafetyDecision(
                    AgentSafetyLevel.IMMEDIATE_SELF_HARM,
                    AgentSafetyRule.ATTEMPT_IN_PROGRESS);
        }
        if (CURRENT_DENIAL.matcher(normalized).find()
                || NEGATED.matcher(normalized).find()) {
            return AgentSafetyDecision.none();
        }
        if (IMMINENT_PLAN.matcher(normalized).find()) {
            return new AgentSafetyDecision(
                    AgentSafetyLevel.IMMEDIATE_SELF_HARM,
                    AgentSafetyRule.IMMINENT_PLAN);
        }
        if (HISTORICAL.matcher(normalized).find()
                && !CURRENT_MARKER.matcher(normalized).find()) {
            return AgentSafetyDecision.none();
        }
        if (DIRECT_INTENT.matcher(normalized).find()) {
            return new AgentSafetyDecision(
                    AgentSafetyLevel.IMMEDIATE_SELF_HARM,
                    AgentSafetyRule.DIRECT_INTENT);
        }
        return AgentSafetyDecision.none();
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s，。！？；：、,.!?;:'\"“”‘’（）()【】\\[\\]]+", "")
                .trim();
    }
}
