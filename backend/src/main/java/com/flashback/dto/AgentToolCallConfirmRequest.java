package com.flashback.dto;

import com.flashback.agent.tool.AgentToolDecision;
import jakarta.validation.constraints.NotNull;

/**
 * 工具提议确认请求（C2）。
 *
 * 刻意**不接受**任何工具参数：执行入参一律取自后端持久化的待确认提议。
 * 若允许客户端回传参数，就等于让前端绕过白名单与校验（design 决策 5 同源理由）。
 */
public class AgentToolCallConfirmRequest {

    @NotNull(message = "decision不能为空")
    private AgentToolDecision decision;

    public AgentToolDecision getDecision() {
        return decision;
    }

    public void setDecision(AgentToolDecision decision) {
        this.decision = decision;
    }
}
