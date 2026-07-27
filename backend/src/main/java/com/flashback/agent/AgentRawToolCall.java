package com.flashback.agent;

/**
 * provider 返回的单条原生工具提议（C2）。
 *
 * 刻意保持未解析状态：name 是否在白名单内、arguments 是否合法，
 * 全部交给 AgentToolValidator 判定，解析层不做任何猜测或补全。
 *
 * @param name      模型给出的工具名
 * @param arguments 参数 JSON 原文
 */
public record AgentRawToolCall(String name, String arguments) {
}
