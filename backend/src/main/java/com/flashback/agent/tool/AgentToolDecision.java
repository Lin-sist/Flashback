package com.flashback.agent.tool;

/**
 * 用户对工具提议的决定（C2）。
 *
 * 只有两种：接受或拒绝。**没有**「以后都同意」这类免确认模式——
 * 每一次执行都必须有一次当场的用户同意（design.md 决策 2）。
 */
public enum AgentToolDecision {

    ACCEPT,
    REJECT
}
