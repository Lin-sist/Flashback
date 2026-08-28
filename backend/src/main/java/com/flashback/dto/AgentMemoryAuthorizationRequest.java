package com.flashback.dto;

import jakarta.validation.constraints.NotNull;

/** P4.2：当前会话是否允许跨记录记忆。 */
public class AgentMemoryAuthorizationRequest {

    @NotNull(message = "crossRecordMemoryEnabled不能为空")
    private Boolean crossRecordMemoryEnabled;

    public Boolean getCrossRecordMemoryEnabled() {
        return crossRecordMemoryEnabled;
    }

    public void setCrossRecordMemoryEnabled(Boolean crossRecordMemoryEnabled) {
        this.crossRecordMemoryEnabled = crossRecordMemoryEnabled;
    }
}
