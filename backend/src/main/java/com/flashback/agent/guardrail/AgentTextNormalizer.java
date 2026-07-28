package com.flashback.agent.guardrail;

/**
 * 护栏判定用的文本归一化（C4）。
 *
 * 设计依据（design.md §3.1）：
 * - 模型整理时几乎必然调整标点与空白（口语「嗯就是那种」→「那种」），
 * 保留这些差异会让覆盖率虚低，把正常整理误判成增写；
 * - 因此归一化只保留字母与数字（中文字符在 JDK 中属 letter），
 * 丢弃全部标点、空白与格式字符，并统一全角与大小写。
 *
 * 代价（已在 design 决策 9 声明）：归一化后无法区分「你想做后端。」与「你想做后端？」，
 * 但语气变化不是本 change 要防的问题——要防的是**新增语义内容**。
 *
 * 确定性要求：同一输入必须永远得到同一输出，本类不得引入任何随机性或环境依赖。
 */
public final class AgentTextNormalizer {

    /** 全角字符区间起点（！）。 */
    private static final char FULLWIDTH_START = '\uFF01';

    /** 全角字符区间终点（～）。 */
    private static final char FULLWIDTH_END = '\uFF5E';

    /** 全角与半角的固定偏移。 */
    private static final int FULLWIDTH_OFFSET = 0xFEE0;

    /** 全角空格。 */
    private static final char IDEOGRAPHIC_SPACE = '\u3000';

    private AgentTextNormalizer() {
    }

    /**
     * 归一化文本。null 与空白输入统一返回空串，便于调用方按「无内容」处理。
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char current = toHalfWidth(raw.charAt(i));
            if (!Character.isLetterOrDigit(current)) {
                // 标点、空白、emoji 与格式字符一律丢弃。
                continue;
            }
            builder.append(Character.toLowerCase(current));
        }
        return builder.toString();
    }

    private static char toHalfWidth(char value) {
        if (value == IDEOGRAPHIC_SPACE) {
            return ' ';
        }
        if (value >= FULLWIDTH_START && value <= FULLWIDTH_END) {
            return (char) (value - FULLWIDTH_OFFSET);
        }
        return value;
    }
}
